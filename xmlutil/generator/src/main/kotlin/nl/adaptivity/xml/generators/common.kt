/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xml.generators

import net.devrieze.util.ReflectionUtil
import nl.adaptivity.kotlin.jvmhelpers.ThrowableUtil
import nl.adaptivity.xml.AbstractXmlReader
import nl.adaptivity.xml.XmlSerializable
import nl.adaptivity.xml.schema.annotations.*
import java.io.File
import java.lang.reflect.*
import java.net.URI
import java.net.URL
import java.net.URLClassLoader
import java.util.*
import javax.xml.XMLConstants
import javax.xml.namespace.QName
import kotlin.reflect.KClass

/*
 * Code shared between all code generators
 */

class ProcessingException(message: String?=null, cause: Throwable?=null) : Exception(message, cause) {
  constructor(cause:Throwable):this(null,cause)
}

fun visitClasses(classpath :Iterable<File>, visitor: (Class<*>)->Unit) {
  val inputClasspath = classpath.filter { it.isDirectory || it.extension.toLowerCase() == "jar" }.map { it.toURI().toURL() }.toTypedArray()
  val classLoader = URLClassLoader(inputClasspath, XmlSerializable::class.java.classLoader)
  val collectedErrors = mutableListOf<ProcessingException>()

  fun File.visit(baseDir:File) {
    if (this.isDirectory) {
      listFiles().forEach { it.visit(baseDir) }
    } else {
      if (extension.toLowerCase() == "class") {
        try {
          val clazz = classLoader.loadClass(className(baseDir, this))
          visitor(clazz)
        } catch (e:Exception) {
          collectedErrors.add(if (e is ProcessingException) e else ProcessingException(e))
        }
      }
    }
  }

  try {
    classpath.asSequence().filter { it.isDirectory }.forEach { it.visit(it) }

    collectedErrors.reduceOrNull { left, right ->  ThrowableUtil.addSuppressed(left, right) }?.let { throw it }
  } catch (e:ProcessingException) {
    throw e
  }


}


fun className(baseDir: File?, file: File): String {
  if (baseDir!=null) {
    val out = file.relativeToOrSelf(baseDir)
    return out.path.removeSuffix(".class").replace("/", ".")
  } else {
    throw ProcessingException("Could not determine the class name of file $file")
  }
}


inline fun <T> Iterable<T>.reduceOrNull(block: (T, T)->T) =
      if (iterator().hasNext()) { reduce(block) } else null

abstract class MemberInfo {
  val name:String
  private val _declaredType: TypeInfo?
  val declaredType:TypeInfo get() = _declaredType?: accessorType
  val accessorType: TypeInfo
  val xmlType: QName get()= declaredType.xmlType
  val readJava: String

  constructor(memberName:String, propertyName:String, ownerType: Type, lookup: TypeInfoProvider, declaredMemberType: TypeInfo?=null):
        this(memberName, propertyName, ownerType.toClass(), lookup, declaredMemberType)

  constructor(memberName:String, propertyName:String, ownerType: Class<*>, lookup: TypeInfoProvider, declaredMemberType: TypeInfo?=null) {
    this.name = memberName
    _declaredType = declaredMemberType

    val m: Method? = ownerType.getGetterForName(propertyName)
    if (m!=null) {
      accessorType = lookup.getTypeInfo(m.genericReturnType)
      readJava = m.name+"()"
    } else {
      val f: Field = ownerType.getFieldForName(propertyName) ?: throw ProcessingException("No accessor for member \"${memberName}\" found on type ${ownerType.canonicalName}")
      accessorType = lookup.getTypeInfo(f.genericType)
      readJava = f.name
    }
  }


  protected fun <T> Class<T>.getGetterForName(name: String): Method? {
    val possibleGetters = allMethods.filter { m -> !( Modifier.isPrivate(m.modifiers) || Modifier.isStatic(m.modifiers) || m.isSynthetic || m.isBridge) && m.parameterCount == 0 }.toList()

    // First find annotated methods
    possibleGetters.asSequence()
          .filter { m -> m.getAnnotation(XmlName::class.java)?.let {it.value==name}?:false }
          .firstOrNull()?.let { m-> return m }

    possibleGetters
          .filter { m -> m.isGetter(name) }
          .let { it: List<Method> ->
            if (it.size>2) throw ProcessingException("Multiple candidate getters for member ${name} found")
            return it.singleOrNull()
          }
  }


  protected fun <T> Class<T>.getFieldForName(name: String): Field? {
    val possibleFields = allFields.filter { f -> (! Modifier.isPrivate(f.modifiers) || Modifier.isStatic(f.modifiers)) }.toList()

    // First find annotated methods
    possibleFields.asSequence()
          .filter { f -> f.getAnnotation(XmlName::class.java).value==name }
          .firstOrNull()?.let { f-> return f }

    possibleFields
          .filter { f -> f.name==name }
          .let { it: List<Field> ->
            if (it.size>2) throw ProcessingException("Multiple candidate fields for member ${name} found")
            return it.singleOrNull()
          }
  }

}

class TypeInfoProvider {
  private val map = mutableMapOf<Type, TypeInfo>()

  fun getTypeInfo(type:Type):TypeInfo {
    return (map[type] ?: let {
      (type.toClass().getAnnotation(Element::class.java)
            ?.let { FullTypeInfo(type, it, this)} // this will add the type info itself
            ?: SimpleTypeInfo(type).apply { map[type]= this } )
    })
  }

  fun register(type: Type, typeInfo: TypeInfo) {
    map[type]=typeInfo
  }
}

class AttributeInfo:MemberInfo {
  val isOptional: Boolean

  var default: String

  constructor(attribute: Attribute, ownerType: Type, lookup: TypeInfoProvider):super(attribute.value, attribute.value, ownerType, lookup) {
    isOptional = attribute.optional
    default = attribute.default
  }

  override fun toString(): String{
    return "AttributeInfo(name=$name, accessorType=${accessorType}, declaredType=${declaredType}, readJava=$readJava, isOptional=$isOptional, default='$default')"
  }


}

class ChildInfo:MemberInfo {

  fun readJava(valueRef:String):String {
    BUILTINS.get(declaredType.elemType)?.let { builtin:Builtin ->
      return builtin.serializeJava(valueRef)
    } ?: throw ProcessingException("Unable to convert type to string content: ${declaredType.elemType}")
  }

  constructor(child: Child, ownerType: Type, lookup: TypeInfoProvider):super(child.name, if (child.property.isBlank()) child.name else child.property, ownerType, lookup, lookup.getTypeInfo(child.type.java)) {
    System.err.println("Getting child information($child, $ownerType, $declaredType)")
  }

  override fun toString(): String{
    return "ChildInfo(name=$name, accessorType=${accessorType}, declaredType=${declaredType}, readJava=$readJava)"
  }


}


private val GETTERPREFIXES = arrayOf("get", "is", "has")

fun Method.isGetter(name: String): Boolean {
  val suffix = "${name[0].toUpperCase()}${name.substring(1)}"
  return !isSynthetic && GETTERPREFIXES.any {
    this.name.length==it.length+suffix.length &&
          this.name.startsWith(it) &&
          this.name.endsWith(suffix) }
}

abstract class TypeInfo(clazz: Type) {
  init {
    if (clazz==Class::class.java) { throw IllegalArgumentException("Java classes can not be serialized") }
  }

  val javaType:Type = clazz

  abstract val isSimpleType: Boolean

  val isXmlSerializable: Boolean by lazy {
    XmlSerializable::class.java.isAssignableFrom(javaType.toClass()) ||
    XmlSerializable::class.java.isAssignableFrom(elemType.toClass())
  }

  val isCollection: Boolean by lazy {
    javaType.toClass(). let { c ->
      (! XmlSerializable::class.java.isAssignableFrom(c)) &&
      (c.isArray || Iterable::class.java.isAssignableFrom(c))
    }
  }

  val elemType:Type by lazy {
    if (javaType is Class<*>&& javaType.isArray) {
      javaType.componentType
    } else {
      val c = javaType.toClass()
      if (XmlSerializable::class.java.isAssignableFrom(c)) {
        javaType
      } else {
        javaType.iterableTypeParam ?: javaType
      }
    }
  }

  val xmlType: QName by lazy { if (javaType==AnyType::class.java) Object::class.java.xmlType else javaType.toClass().xmlType }
  val isPrimitive: Boolean by lazy { elemType.toClass().isPrimitive }
}



fun Type.toClass(): Class<*> {
  fun Type.helper():Class<*>? {
    return when(this) {
      is Class<*>          -> this
      is TypeVariable<*>   -> this.bounds.firstOrNull()?.helper()
      is WildcardType      -> this.upperBounds.firstOrNull()?.helper()
      is GenericArrayType  -> this.genericComponentType?.helper()?.let { java.lang.reflect.Array.newInstance(it,0).javaClass }
      is ParameterizedType -> this.rawType.helper()
      else                 -> throw ClassCastException("Not an expected subtype of class ${javaClass.typeName}")
    }

  }
  return helper()!!
}

val Type.isIterable:Boolean get() = Iterable::class.java.isAssignableFrom(this.toClass())

val Type.iterableTypeParam:Type? get() {
  return ReflectionUtil.typeParams(this, Iterable::class.java)?.get(0)
}

internal class Builtin(val typeName:QName, val serializeJava: (String)->String, val deserializeJava: (String)->String) {
  constructor(typeName:String, serializeJava: (String) -> String, deserializeJava: (String) -> String):
      this(QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, typeName, "xs"), serializeJava, deserializeJava)
}

internal val BUILTINS = mapOf<Class<out Any>,Builtin>(
      Int::class.java to Builtin("int", { it -> "Integer.toString(${it})"}, {it -> "Integer.valueOf($it)"}),
      String::class.java to Builtin("string", { it -> it}, {it -> it}),
      UUID::class.java to Builtin("string", { it -> "$it.toString()"}, { it -> "UUID.fromString($it)"}),
      QName::class.java to Builtin("string", { it -> "{$it.namespaceUri}$it.localName"}, { it -> "${AbstractXmlReader::class.java.canonicalName}.toQname($it)"}),
      Long::class.java to Builtin("long", { it -> "Long.toString(${it})"}, {it -> "Long.valueOf($it)"})
)


class SimpleTypeInfo(clazz:Type) :TypeInfo(if (clazz==AnyType::class.java) Object::class.java else clazz) {
  override val isSimpleType: Boolean
    get() = (elemType is Class<*> && (elemType as Class<*>).isPrimitive) || BUILTINS.containsKey(elemType)

  override fun toString(): String{
    return "SimpleTypeInfo(javaType=${javaType})"
  }

}

class FullTypeInfo:TypeInfo {
  val nsPrefix: CharSequence?
  val nsUri: String?
  val elementName: CharSequence
  val attributes: Array<AttributeInfo>

  val children: Array<ChildInfo>

  override val isSimpleType: Boolean get() = false

  val packageName: String get() =elemType.toClass().`package`.name

  val factoryClassName: String get() =
    "${elemType.toClass().canonicalName.substring(elemType.toClass().`package`.name.length+1).replace('.','_')}Factory"

  @Deprecated("Use the 3 argument version")
  constructor(type: Type, element: Element):this(type, element, TypeInfoProvider())

  constructor(type: Type, element: Element, typeMap: TypeInfoProvider):super(type) {
    nsPrefix = if (element.nsPrefix.isEmpty()) null else element.nsPrefix
    nsUri = if (element.nsUri.isBlank()) null else element.nsUri
    elementName = element.name
    val elemClass = elemType.toClass()
    attributes = Array(element.attributes.size) { AttributeInfo(element.attributes[it], elemClass, typeMap) }

    typeMap.register(type, this) // register the type to enable cyclic type graphs
    children = Array<ChildInfo>(element.children.size) { idx ->
      ChildInfo(element.children[idx], type, typeMap)
    }
  }

  override fun toString(): String{
    return "FullTypeInfo(javaType=$javaType, nsPrefix=$nsPrefix, nsUri=$nsUri, elementName=$elementName, attributes=${Arrays.toString(attributes)}, children=${Arrays.toString(children)}, xmlType=$xmlType)"
  }

}

private val Class<*>.xmlType: QName get() {
  fun schemaname(name:String) = QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, name, "xs")

  when(kotlin) {
    Int::class             -> return schemaname("int")
    Short::class           -> return schemaname("short")
    String::class          -> return schemaname("string")
    URI::class, URL::class -> return schemaname("uri")
    AnyType::class         -> return schemaname("any")
  }
  val element = getAnnotation(Element::class.java)
  if (element==null) throw ProcessingException("Missing xml information for type ${this}")
  val typeName = element.typeName.let { if (it.isBlank()) "${simpleName}T" else it }
  return QName(element.nsUri, typeName, element.nsPrefix)
}

fun toLiteral(seq:CharSequence?):String {
  if (seq ==null) return "null"
  return buildString {
    append('"')
    seq.forEach { c -> when (c) {
      '"','\'','\\' -> append('\\').append(c)
      '\n' -> append("\\n")
      '\r' -> append("\\r")
      '\t' -> append("\\t")
      else -> append(c)
    } }
    append('"')
  }
}

val Class<*>.allMethods: Sequence<Method>
get() {
  return object:Iterator<Method> {
    var parent = this@allMethods.superclass
    var curMethods = this@allMethods.declaredMethods
    var curPos=0

    override fun hasNext(): Boolean {
      if (curPos<curMethods.size) return true
      if (parent==null) return false
      curMethods = parent.declaredMethods
      parent = parent.superclass
      curPos = 0
      return hasNext()
    }

    override fun next(): Method {
      if (!hasNext()) throw NoSuchElementException("Iterating beyond the range")
      return curMethods[curPos++]
    }

  } .asSequence()
}

val Class<*>.allFields: Sequence<Field>
get() {
  return object:Iterator<Field> {
    var parent = this@allFields.superclass
    var curFields = this@allFields.declaredFields
    var curPos=0

    override fun hasNext(): Boolean {
      if (curPos<curFields.size) return true
      if (parent==null) return false
      curFields = parent.declaredFields
      parent = parent.superclass
      curPos = 0
      return hasNext()
    }

    override fun next(): Field {
      if (!hasNext()) throw NoSuchElementException("Iterating beyond the range")
      return curFields[curPos++]
    }

  } .asSequence()
}
