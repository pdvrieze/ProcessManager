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
import nl.adaptivity.xml.XmlReader
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
  val getterJava: String
  val setterJavaBase: String
  val setterIsField: Boolean

  fun readJava(valueRef:String):String {
    BUILTINS.get(declaredType.elemType)?.let { builtin:Builtin ->
      return builtin.serializeJava(valueRef)
    } ?: throw ProcessingException("Unable to convert type to string content: ${declaredType.elemType}")
  }

  open fun setJavaFromString(stringRepr:String):String {
    if (setterIsField) {
      return "$setterJavaBase = ${javaFromString(stringRepr)}"
    } else {
      return "$setterJavaBase(${javaFromString(stringRepr)})"
    }
  }

  open fun javaFromString(stringRepr: String): String {
    BUILTINS.get(declaredType.elemType)?.let { builtin:Builtin ->
      return builtin.deserializeJava(stringRepr)
    } ?: throw ProcessingException("Unable to convert string content to type: ${declaredType.elemType}")
  }

  constructor(memberName:String, propertyName:String, ownerType: Type, lookup: TypeInfoProvider, declaredMemberType: TypeInfo?=null):
        this(memberName, propertyName, ownerType.toClass(), lookup, declaredMemberType)

  constructor(memberName:String, propertyName:String, ownerType: Class<*>, lookup: TypeInfoProvider, declaredMemberType: TypeInfo?=null) {
    this.name = if(memberName.isBlank()) propertyName else memberName
    _declaredType = declaredMemberType

    var getter: Method? = ownerType.getGetterForName(propertyName)
    var setter: Method? = ownerType.getSetterForName(propertyName)
    if (getter!=null && setter==null) {
      val javaPropertyName = getter.propertyName
      if (javaPropertyName!=propertyName) { setter = ownerType.getSetterForName(javaPropertyName) }
    } else if (setter!=null && getter==null) {
      val javaPropertyName = setter.propertyName
      if (javaPropertyName!=propertyName) { getter = ownerType.getGetterForName(javaPropertyName) }
    }

    if (getter!=null) {
      accessorType = lookup.getTypeInfo(getter.genericReturnType)
      val getterOnly = accessorType.isMap || accessorType.isCollection
      if (getterOnly) {
        getterJava = getter.name+"()"
        setterJavaBase = getter.name+"()"
        setterIsField = false
      } else if (setter!=null) {
        // TODO make this nicer on generic assignability
        if (lookup.getTypeInfo(setter.genericParameterTypes[0])!=accessorType) throw ProcessingException("Setter (${setter.genericParameterTypes[0]}) and getter types ($accessorType) do not match for property \"$propertyName\"")
        getterJava = getter.name+"()"
        setterJavaBase = setter.name
        setterIsField = false
      } else {
        val f: Field = ownerType.getFieldForName(propertyName) ?: throw ProcessingException("No accessor for member \"${propertyName}\" found on type ${ownerType.canonicalName}")
        getterJava = getter.name+"()"
        setterJavaBase = f.name
        setterIsField = true
      }
    } else {
      val f: Field = ownerType.getFieldForName(propertyName) ?: throw ProcessingException("No accessor for member \"${propertyName}\" found on type ${ownerType.canonicalName}")
      accessorType = lookup.getTypeInfo(f.genericType)
      getterJava = f.name
      setterJavaBase = f.name
      setterIsField = true
    }
  }

  protected fun <T> Class<T>.getGetterForName(name: String): Method? {
    val possibleGetters = allMethods.filter { m -> !( Modifier.isPrivate(m.modifiers) || Modifier.isStatic(m.modifiers) || m.isSynthetic || m.isBridge) && m.parameterTypes.size == 0 }.toList()

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


  protected fun Class<*>.getSetterForName(name: String): Method? {
    val possibleSetters = allMethods.filter { m -> !( Modifier.isPrivate(m.modifiers) || Modifier.isStatic(m.modifiers) || m.isSynthetic || m.isBridge) && m.parameterTypes.size == 1 }.toList()

    // First find annotated methods
    possibleSetters.asSequence()
          .filter { m -> m.getAnnotation(XmlName::class.java)?.let {it.value==name}?:false }
          .firstOrNull()?.let { m-> return m }

    possibleSetters
          .filter { m -> m.isSetter(name) }
          .let { it: List<Method> ->
            if (it.size>2) throw ProcessingException("Multiple candidate setters for member ${name} found")
            return it.singleOrNull()
          }
  }

  protected fun Class<*>.getFieldForName(name: String): Field? {
    val possibleFields = allFields.filter { f -> (! Modifier.isPrivate(f.modifiers) || Modifier.isStatic(f.modifiers)) }.toList()

    // First find annotated methods
    possibleFields.asSequence()
          .filter { f -> f.getAnnotation(XmlName::class.java)?.value==name }
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

class ContentInfo: MemberInfo {
  constructor(propertyName:String, ownerType: Type, lookup: TypeInfoProvider):super(propertyName, propertyName, ownerType, lookup)
}

class AttributeInfo:MemberInfo {
  val isOptional: Boolean

  var default: String

  constructor(attribute: Attribute, ownerType: Type, lookup: TypeInfoProvider):super(attribute.value, attribute.value, ownerType, lookup) {
    isOptional = attribute.optional
    default = attribute.default
  }

  fun setJavaFromString(key:String, value: String): String {
    return "$getterJava.put($key, $value)"
  }

  override fun toString(): String {
    return "AttributeInfo(name=$name, accessorType=${accessorType}, declaredType=${declaredType}, readJava=$getterJava, isOptional=$isOptional, default='$default')"
  }


}

/**
 * Function to determine the property name for a child/attribute.
 */
private fun Child.propertyName(lookup: TypeInfoProvider):String {
  return if (!property.isBlank()) {
    property
  } else if (!name.isBlank()) {
    name
  } else {
    lookup.getTypeInfo(this.type.java).let { info ->
      if (info is FullTypeInfo) {
        info.elementName.toString()
      } else {
        val base = info.javaType.toClass().simpleName
        "${base[0].toLowerCase()}${base.substring(1)}"
      }
    }
  }
}

class ChildInfo:MemberInfo {

  constructor(child: Child, ownerType: Type, lookup: TypeInfoProvider):super(child.name, child.propertyName(lookup), ownerType, lookup, lookup.getTypeInfo(child.type.java))

  override fun toString(): String{
    return "ChildInfo(name=$name, accessorType=${accessorType}, declaredType=${declaredType}, readJava=$getterJava)"
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

private val SETTERPREFIXES = arrayOf("set")

fun Method.isSetter(name: String): Boolean {
  val suffix = "${name[0].toUpperCase()}${name.substring(1)}"
  return !isSynthetic && SETTERPREFIXES.any {
    this.name.length==it.length+suffix.length &&
          this.name.startsWith(it) &&
          this.name.endsWith(suffix) }
}

val Method.propertyName: String get() {
  return (GETTERPREFIXES.asSequence() + SETTERPREFIXES.asSequence())
        .first { name.startsWith(it) && Character.isUpperCase(name[it.length]) }
        ?.let { "${Character.toLowerCase(name[it.length])}${name.substring(it.length+1)}" }
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

  val isMap: Boolean by lazy {
    javaType.toClass(). let { c ->
      (! XmlSerializable::class.java.isAssignableFrom(c)) &&
      Map::class.java.isAssignableFrom(c) &&
            (ReflectionUtil.concreteTypeParams(javaType, Map::class.java)?.let{(CharSequence::class.java.isAssignableFrom(it[0])||
                  QName::class.java.isAssignableFrom(it[0])
                  )} ?: false)
    }
  }

  val elemType:Type by lazy {
    if (javaType is Class<*>&& javaType.isArray) {
      javaType.componentType
    } else {
      val c = javaType.toClass()
      if (XmlSerializable::class.java.isAssignableFrom(c)) {
        javaType
      } else if (Map::class.java.isAssignableFrom(c)) {
        ReflectionUtil.typeParams(javaType, Map::class.java)?.get(1) ?: javaType
      } else {
        javaType.iterableTypeParam ?: javaType
      }
    }
  }

  val xmlType: QName by lazy { if (javaType==AnyType::class.java) Object::class.java.xmlType else javaType.toClass().xmlType }
  val isPrimitive: Boolean by lazy { elemType.toClass().isPrimitive }
  val defaultValueJava: String by lazy {
    if (javaType is Class<*>&& javaType.isArray) {
      "null"
    } else {
      val c = javaType.toClass()
      if (c.isPrimitive) {
        BUILTINS[c]?.defaultValueJava ?: throw UnsupportedOperationException("No default value know for type ${c}")
      } else if (Iterable::class.java.isAssignableFrom(c) && c.isAssignableFrom(ArrayList::class.java)) {
        "new ArrayList()"
      } else if (Map::class.java.isAssignableFrom(c) && c.isAssignableFrom(HashMap::class.java)) {
        "new HashMap()"
      } else {
        "null"
      }
    }
  }
}



fun Type.toClass(): Class<*> {
  fun Type.helper():Class<*>? {
    return when(this) {
      is Class<*>          -> this
      is TypeVariable<*>   -> this.bounds.firstOrNull()?.helper()
      is WildcardType      -> this.upperBounds.firstOrNull()?.helper()
      is GenericArrayType  -> this.genericComponentType?.helper()?.let { java.lang.reflect.Array.newInstance(it,0).javaClass }
      is ParameterizedType -> this.rawType.helper()
      else                 -> throw ClassCastException("Not an expected subtype of class ${javaClass.canonicalName}")
    }

  }
  return helper()!!
}

val Type.isIterable:Boolean get() = Iterable::class.java.isAssignableFrom(this.toClass())

val Type.iterableTypeParam:Type? get() {
  return ReflectionUtil.typeParams(this, Iterable::class.java)?.get(0)
}

internal class Builtin(val typeName:QName, val defaultValueJava:String, val serializeJava: (String)->String, val deserializeJava: (String)->String) {
  constructor(typeName:String, defaultValueJava:String, serializeJava: (String) -> String, deserializeJava: (String) -> String):
      this(QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, typeName, "xs"), defaultValueJava, serializeJava, deserializeJava)
}

internal val BUILTINS = mapOf<Class<out Any>,Builtin>(
      Int::class.java to Builtin("int", "-1", { it -> "Integer.toString(${it})"}, {it -> "Integer.valueOf($it)"}),
      String::class.java to Builtin("string", "null", { it -> it}, {it -> it}),
      CharSequence::class.java to Builtin("string", "null", { it -> "$it.toString"}, {it -> it}),
      Boolean::class.java to Builtin("boolean", "false", { it -> "$it ? \"true\" : \"false\""}, {it -> "Boolean.valueOf($it)"}),
      UUID::class.java to Builtin("string", "null", { it -> "$it.toString()"}, { it -> "UUID.fromString($it)"}),
      QName::class.java to Builtin("QName", "null", { it -> "{$it.namespaceUri}$it.localName"}, { it -> "nl.adaptivity.xml.XmlUtil.toQname($it)"}),
      Long::class.java to Builtin("long", "-1L", { it -> "Long.toString(${it})"}, {it -> "Long.valueOf($it)"})
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

  val textContent: ContentInfo?


  @Deprecated("Use the 3 argument version")
  constructor(type: Type, element: Element):this(type, element, TypeInfoProvider())

  constructor(type: Type, element: Element, typeMap: TypeInfoProvider):super(type) {
    nsPrefix = if (element.nsPrefix.isEmpty()) null else element.nsPrefix
    nsUri = if (element.nsUri.isBlank()) null else element.nsUri
    elementName = element.name
    val elemClass = elemType.toClass()
    attributes = Array(element.attributes.size) { AttributeInfo(element.attributes[it], elemClass, typeMap) }

    textContent = if (element.content.isBlank()) { null} else {
      ContentInfo(element.content, type, typeMap)
    }

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

internal data class SimpleTypeVar(val name:String, val bounds:Array<Type>) {
  override fun equals(other: Any?): Boolean{
    if (this === other) return true
    if (other?.javaClass != javaClass) return false

    other as SimpleTypeVar

    if (name != other.name) return false
    if (!Arrays.equals(bounds, other.bounds)) return false

    return true
  }

  override fun hashCode(): Int{
    var result = name.hashCode()
    result = 31 * result + Arrays.hashCode(bounds)
    return result
  }

}


internal fun getTypeVars(source: List<Type>): List<SimpleTypeVar> {
  val usedNames = mutableSetOf<String>()
  val result = mutableListOf<SimpleTypeVar>()

  fun getName(n:String):String {
    if (n !in usedNames) { return n }
    return (1..Int.MAX_VALUE).asSequence().map { "$n$it" }.first { it !in usedNames }
  }

  fun getBounds(type: Type):Array<Type> {
    return when (type) {
      is TypeVariable<*> -> type.bounds
      is GenericArrayType -> arrayOf<Type>(Array<Any>::class.java)
      is ParameterizedType -> getBounds(type.rawType)
      is WildcardType -> type.upperBounds
      is Class<*> -> arrayOf<Type>(type)
      else -> emptyArray()
    }
  }

  fun addTypeVar(type: Type) {
    when (type) {
      is TypeVariable<*> -> result.add(SimpleTypeVar(getName(type.name), type.bounds))
      is GenericArrayType -> addTypeVar(type.genericComponentType)
      is ParameterizedType -> {
        val params = (type.rawType as Class<*>).typeParameters
        type.actualTypeArguments.forEachIndexed { i, type ->
          val name = getName(params[i].name)
          result.add(SimpleTypeVar(name, getBounds(type)))
        }
      }
//        is WildcardType -> {}
//        is Class<*> -> {}
      else -> {}
    }
  }

  for(type in source) {
    addTypeVar(type)
  }
  return result
}

fun resolveMethodType(m:Method, t:Type, newType: (Class<*>)->String = {it.canonicalName}): String {
  val owner = m.declaringClass

  fun doActualLookup(tv:TypeVariable<*>, candidateDeclaration:Type): TypeVariable<*>? {
    if (tv.genericDeclaration==candidateDeclaration) {
      return tv
    } else {
      if (candidateDeclaration is Class<*>) {
        val allAncestors = sequenceOf(candidateDeclaration.genericInterfaces.asSequence(),
                                      sequenceOf(candidateDeclaration.genericSuperclass)).flatten()
        allAncestors.map { ancestor ->
          val lookup = doActualLookup(tv, ancestor)
          if (ancestor is GenericDeclaration) {
            lookup?.let {
              ancestor.typeParameters.mapIndexed { i, typeVariable ->  }
            }
          } else { lookup }
        }
//      candidateDeclaration.
      }

      return null
    }
  }

  val variableLookup :(TypeVariable<*>)->String? = { tv: TypeVariable<*> ->
    val l = doActualLookup(tv, owner)
    (l?: tv).bounds.asSequence().firstOrNull()?.let {resolveMethodType(m, it, newType)}
  }

  return resolveType(t, variableLookup, newType)
}

fun Class<*>.withParams(vararg params:Type):Type {
  val typeParams = typeParameters
  if (typeParams.size!=params.size) throw IllegalArgumentException("Generic parameter count mismatch.")
  return object: ParameterizedType {
    val _rawType = if (this@withParams is ParameterizedType) this@withParams.rawType else this@withParams.toClass()
    override fun getRawType() = _rawType

    override fun getOwnerType() = this@withParams.enclosingClass

    override fun getActualTypeArguments() = params

    override fun toString(): String {
      return params.joinToString(", ","WithParams: ${_rawType.toClass().canonicalName}<", ">") { it.toString() }
    }
  }
}

fun resolveType(type: Type, variableLookup:(TypeVariable<*>)->String?={null}, newType: (Class<*>)->String = {it.canonicalName}): String {
  fun getTypeString(type: Class<*>): String {
    return type.declaringClass?.let {
      "${newType(it)}.${type.simpleName}"
    } ?: newType(type)
  }

  fun doResolve(t:Type):String {
    return when (t) {
      is TypeVariable<*> -> {

        val lookupResults = variableLookup(t)
        if (lookupResults!=null){
          lookupResults
        } else {
//          val resolve = doResolve((t.genericDeclaration.typeParameters.firstOrNull { it.name == t.name } ?: t).bounds[0])
          val resolve = doResolve(t.bounds[0])

          "? extends $resolve"
        }
      }
      is GenericArrayType -> "${doResolve(t.genericComponentType)}"
      is ParameterizedType -> buildString {
        append(getTypeString(t.rawType.toClass()))
        append('<')
        t.actualTypeArguments.joinTo(this) { doResolve(it) }
        append('>')
      }
      is WildcardType -> {
        if(t.lowerBounds.size>0) {
          "? super ${t.lowerBounds.joinToString{doResolve(it)}}"
        } else if (t.upperBounds.filter { t==Object::class.java }.size>0) {
          "? extends ${t.upperBounds.joinToString{doResolve(it)}}"
        } else {
          "?"
        }
      }
      is Class<*> -> {
        val typeString = getTypeString(t)
        buildString {
          append(typeString)
          t.typeParameters.let { typeParams -> if (typeParams.size>0) {typeParams.joinTo(this, ", ", "<", ">") {doResolve(it)}}}
        }
      }
      else -> t.toString()

    }

  }

  return doResolve(type)

}