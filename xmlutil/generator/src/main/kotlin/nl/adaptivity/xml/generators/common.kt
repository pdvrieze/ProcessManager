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

import nl.adaptivity.kotlin.jvmhelpers.ThrowableUtil
import nl.adaptivity.xml.XmlSerializable
import nl.adaptivity.xml.schema.annotations.Attribute
import nl.adaptivity.xml.schema.annotations.Element
import nl.adaptivity.xml.schema.annotations.XmlName
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.net.URLClassLoader
import javax.xml.XMLConstants
import javax.xml.namespace.QName

/**
 * Created by pdvrieze on 15/04/16.
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
          collectedErrors.add(ProcessingException(e))
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

class AttributeInfo {
  val name: String
  val isOptional: Boolean
  val type: Class<*>
  val xmlType: QName
  val readJava: String

  var default: String

  constructor(attribute: Attribute, ownerType: Class<*>) {
    name = attribute.value
    isOptional = attribute.optional
    default = attribute.default

    val m: Method? = ownerType.getGetterForName(attribute.value)
    if (m!=null) {
      type = m.returnType
      readJava = m.name+"()"
    } else {
      val f: Field = ownerType.getFieldForName(attribute.value) ?: throw ProcessingException("No accessor for attribute ${attribute.value} found")
      type = f.type
      readJava = f.name
    }
    xmlType = getXmlType(type)

  }

  private fun getXmlType(type: Class<*>): QName {
    fun schemaname(name:String) = QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, name, "xs")

    when(type.kotlin) {
      Int::class -> return schemaname("int")
      Short::class -> return schemaname("short")
    }
    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
  }


  private fun <T> Class<T>.getGetterForName(name: String): Method? {
    val possibleGetters = methods.filter { m -> Modifier.isPublic(m.modifiers) && !Modifier.isStatic(m.modifiers) && m.parameterCount == 0 }

    // First find annotated methods
    possibleGetters.asSequence()
          .filter { m -> m.getAnnotation(XmlName::class.java).value==name }
          .firstOrNull()?.let { m-> return m }

    possibleGetters
          .filter { m -> m.isGetter(name) }
          .let { it: List<Method> ->
            if (it.size>2) throw ProcessingException("Multiple candidate getters for propery ${name} found")
            return it.singleOrNull()
          }
  }


  private fun <T> Class<T>.getFieldForName(name: String): Field? {
    val possibleFields = fields.filter { f -> Modifier.isPublic(f.modifiers) && !Modifier.isStatic(f.modifiers) }

    // First find annotated methods
    possibleFields.asSequence()
          .filter { f -> f.getAnnotation(XmlName::class.java).value==name }
          .firstOrNull()?.let { f-> return f }

    possibleFields
          .filter { f -> f.name==name }
          .let { it: List<Field> ->
            if (it.size>2) throw ProcessingException("Multiple candidate fields for propery ${name} found")
            return it.singleOrNull()
          }
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


class TypeInfo {
  val javaType:Class<*>
  val nsPrefix: CharSequence?
  val nsUri: String?
  val elementName: CharSequence
  val attributes: Array<AttributeInfo>

  val packageName: String get() =javaType.`package`.name

  val factoryClassName: String get() =
  javaType.canonicalName.removePrefix(javaType.`package`.name).replace('.', '_')

  constructor(clazz: Class<*>, element: Element) {
    javaType = clazz
    nsPrefix = if (element.nsPrefix.isEmpty()) null else element.nsPrefix
    nsUri = if (element.nsUri.isBlank()) null else element.nsUri
    elementName = element.name
    attributes = Array(element.attributes.size) { AttributeInfo(element.attributes[it], javaType) }
  }

}

fun toLiteral(seq:CharSequence?):String {
  if (seq ==null) return "null"
  return buildString { seq.forEach { c -> when (c) {
    '"','\'','\\' -> append('\\').append(c)
    '\n' -> append("\\n")
    '\r' -> append("\\r")
    '\t' -> append("\\t")
    else -> append(c)
  } } }
}