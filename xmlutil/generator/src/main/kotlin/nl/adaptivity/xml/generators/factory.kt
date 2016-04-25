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
import nl.adaptivity.xml.*
import nl.adaptivity.xml.schema.annotations.AnyType
import nl.adaptivity.xml.schema.annotations.Element
import java.io.CharArrayWriter
import java.io.File
import java.io.StringWriter
import java.io.Writer
import java.lang.reflect.*
import java.util.*
import javax.xml.namespace.QName

/*
 * Simple information creating package that just lists the possible, and the available classes.
 * Created by pdvrieze on 15/04/16.
 */

class Factory {
  companion object {

    /**
     * This function implements task level generation. Support that as well in this file.
     */
    @JvmStatic
    fun doGenerate(outputDir: File, input: Iterable<File>) {
      if (outputDir.isFile) throw ProcessingException("The output location is not a directory")
      visitClasses(input) { clazz ->
        if (XmlSerializable::class.java.isAssignableFrom(clazz)) {
          val elementAnnot = clazz.getAnnotation(Element::class.java)
          if (elementAnnot != null) {
            val typeInfo = FullTypeInfo(clazz, elementAnnot)
            generateFactory(outputDir, typeInfo)
          }
        }
      }
    }

    private fun generateFactory(outDir: File, typeInfo: FullTypeInfo) {
      val factoryClassName = typeInfo.factoryClassName
      val packageName = typeInfo.packageName
      val nsPrefix = typeInfo.nsPrefix

      val fileCreator = createJavaFile(packageName, factoryClassName) {
        emptyConstructor()
        writeSerialize(nsPrefix, typeInfo)
        writeDeserialize(nsPrefix, typeInfo)

      }

      val outputFile = getFactorySourceFile(outDir, packageName, factoryClassName)
      outputFile.writer().use {
        fileCreator.appendTo(it)
      }
    }

    private fun JavaFile.writeSerialize(nsPrefix: CharSequence?,
                                        typeInfo: FullTypeInfo) {
      method("serialize",
             null,
             arrayOf(XmlException::class.java),
             XmlWriter::class.java to "writer",
             typeInfo.elemType to "value") {
        val writer: XmlWriter = XmlStreaming.newWriter(StringWriter())

        appendln("    writer.startTag(${toLiteral(typeInfo.nsUri)}, ${toLiteral(typeInfo.elementName)}, ${toLiteral(
              nsPrefix)});")

        typeInfo.attributes.forEach { attr ->
          appendln()
          if (attr.accessorType.isMap) {
            val keyType = ReflectionUtil.typeParams(attr.accessorType.javaType,
                                                    Map::class.java)?.get(0) ?: CharSequence::class.java
            appendln("    for(${Map.Entry::class.java.ref}<${keyType.ref}, ${attr.accessorType.elemType.ref}> attr: value.${attr.readJava}.entrySet()) {")
            val keyClass = keyType.toClass()
            if (QName::class.java.isAssignableFrom(keyClass)) {
              appendln("      QName key = attr.getKey(); writer.attribute(key.getNamespaceURI(), key.getLocalPart(), key.getPrefix(), ${attr.readJava(
                    "attr.getValue()")});")
            } else {
              val getKey = if (String::class.java == keyClass) "attr.getKey()" else "attr.getKey().toString()"
              appendln("      writer.attribute(null, ${getKey}, null, ${attr.readJava("attr.getValue()")});")
            }
            appendln("    }")
          } else {
            appendln("    {")
            appendln("      final ${attr.accessorType.javaType.ref} attrValue = value.${attr.readJava};")
            if (attr.isOptional && !attr.accessorType.isPrimitive) {
              appendln("      if (attrValue!=null) writer.attribute(null, ${toLiteral(attr.name)}, null, ${attr.readJava(
                    "attrValue")});")
            } else {
              appendln("      writer.attribute(null, ${toLiteral(attr.name)}, null, attrValue==null ? ${toLiteral(attr.default)} : ${attr.readJava(
                    "attrValue")});")
            }
            appendln("    }")
          }
        }

        typeInfo.textContent?.let { content ->
          appendln()
          appendln("    {")
          appendln("      final ${content.accessorType.javaType.ref} contentValue = value.${content.readJava};")
          appendln("      if (contentValue!=null) writer.text(${content.readJava("contentValue")});")
          appendln("    }")
        }

        typeInfo.children.forEach { childInfo ->
          val accessor = childInfo.accessorType
          appendln()
          appendln("    {")
          val attrname = if (accessor.isCollection) "childValues" else "childValue"
          appendln("      final ${accessor.javaType.ref} $attrname = value.${childInfo.readJava};")
          var indent: String
          if (accessor.isCollection) {
            indent = " ".repeat(8)
            appendln("      for(final ${accessor.elemType.ref} childValue: childValues) {")
          } else indent = " ".repeat(6)

          writeSerializeChild(indent, typeInfo, childInfo, "childValue")

          if (accessor.isCollection) {
            appendln("      }")
          }
          appendln("    }")

        }

        appendln()
        appendln("    writer.endTag(${toLiteral(typeInfo.nsUri)}, ${toLiteral(typeInfo.elementName)}, ${toLiteral(
              nsPrefix)});")
      }
    }

    private fun getFactorySourceFile(outDir: File, packageName: String, factoryClassName: String): File {
      val directory = packageName.replace('.', '/');
      val filename = factoryClassName + ".java"
      return File(outDir, "${directory}/$filename").apply { parentFile.mkdirs(); createNewFile() }
    }

    private fun JavaFile.writeDeserialize(nsPrefix: CharSequence?,
                                          typeInfo: FullTypeInfo) {

      method("deserialize",
             typeInfo.elemType,
             arrayOf(XmlException::class.java),
             XmlReader::class.java to "reader") {
        if (typeInfo.attributes.size>0) {
          for (attr in typeInfo.attributes) {
            appendln("    ${attr.accessorType.javaType.ref} ${attr.name} = ${if (attr.default.isNotBlank()) attr.javaFromString(
                  toLiteral(attr.default)) else attr.accessorType.defaultValueJava};")
          }
          appendln()
        }

        if (typeInfo.children.size>0) {
          for (child in typeInfo.children) {
            appendln("    ${child.accessorType.javaType.ref} ${child.name} = ${child.accessorType.defaultValueJava};")
          }
          appendln()
        }

        appendln("    reader.require(${XmlStreaming.EventType::class.java.ref}.START_ELEMENT, ${toLiteral(typeInfo.nsUri)}, ${toLiteral(typeInfo.elementName)});")

        if (typeInfo.attributes.size>0) {
          appendln()
          appendln("    for (int i = 0; i < reader.getAttributeCount(); i++) {")
          appendln("      switch(reader.getAttributeLocalName(i).toString() {")
          for(attr in typeInfo.attributes) {
            append("        case \"${attr.name}\": ")
            if (attr.accessorType.isMap) appendln("")
            appendln("")
          }
          appendln("        default:")
          appendln("          throw new XmlException(\"Unexpected attribute found (\"+reader.getAttributeLocalName(i)+\")\");")
          appendln("      }")
          appendln("    }")
        }

        val eventType = XmlStreaming.EventType::class.java.ref
        appendln()
        appendln("    EventType eventType;")
        appendln("    while ((eventType=reader.next())!=${eventType}.END_ELEMENT) {")
        appendln("      switch(eventType) {")
        appendln("        case CDSECT:")
        appendln("        case TEXT:")
        if (typeInfo.textContent!=null) {
          appendln("        break;")
        }
        appendln("      }")
        appendln("    }")

        appendln("    reader.require(${eventType}.END_ELEMENT, ${toLiteral(typeInfo.nsUri)}, ${toLiteral(typeInfo.elementName)});")

      }
    }
  }
}

private class JavaFile(val packageName:String, val className:String) {

  val classBody = mutableListOf<Appendable.()->Unit>()
  val imports = mutableSetOf<Class<*>>()

  fun emptyConstructor() {
    classBody.add {
      appendln("  public $className() {}")
    }
  }

  val Type.ref:String get() {
    return when (this) {
      is Class<*> -> {
        val pkgName = this.`package`.name
        val declaringClass = this.declaringClass
        if (pkgName !="java.lang" && declaringClass==null && pkgName!=packageName) imports.add(this)
        declaringClass?.let{ it.ref }
        this.canonicalName.removePrefix(pkgName+'.')
      }
      is ParameterizedType -> this.actualTypeArguments.joinToString(", ", "${this.rawType.ref}<", ">") { it.ref }
      is GenericArrayType -> "${this.genericComponentType.ref}[]"
      is TypeVariable<*> -> {
        (this.bounds.firstOrNull()?.ref  ?: "Object")
      }
      is WildcardType -> { this.upperBounds.firstOrNull()?.ref ?: "Object" }
      else -> throw UnsupportedOperationException("Cannot display type: ${this}")
    }
  }

  inline fun method(name:String, returnType:Type?, vararg parameters:Pair<Type,String>, crossinline body:Appendable.()->Unit) {
    return method(name, returnType, emptyArray(), *parameters) { body() }
  }

  inline fun method(name:String, returnType:Type?, throws: Array<out Class<out Throwable>>, vararg parameters:Pair<Type,String>, crossinline body:Appendable.()->Unit) {
    classBody.add {
      val typeVars : List<SimpleTypeVar> = getTypeVars(parameters.map { it.first })
      append("  public static final ${returnType?.ref?:"void"} ${name}(")
      parameters.joinTo(this) { val (type, name) = it; "${type.ref} ${name}" }
      append(")")
      if (throws.size>0) {
        append(" throws ")
        throws.joinTo(this) { it.ref }
      }
      appendln(" {")
      body()
      appendln("  }")
    }
  }

  fun appendTo(writer: Writer): Unit {
    // First generate the body, so all imports are resolved.
    val body = CharArrayWriter().apply {
      classBody.forEach {
        appendln()
        it.invoke(this)
      }
    }.toCharArray()

    writer.run {
      appendln("/* Automatically generated by ${this@JavaFile.className} */")
      appendln()
      appendln("package ${packageName};")

      if (imports.size>0) {
        appendln()
        imports.asSequence().map { c -> "import ${c.canonicalName};" }.sorted().forEach { appendln(it) }
      }
      appendln()
      appendln("public class ${className} {")

      write(body)

      appendln()
      appendln("}")
    }
  }


  fun Appendable.writeSerializeChild(indent: String, owner: FullTypeInfo, childInfo: ChildInfo, valueRef: String) {
    append(indent).append("if (").append(valueRef).append("!=null) ")
    if (childInfo.accessorType.isXmlSerializable) {
      append(valueRef).appendln(".serialize(writer);")
    } else if (childInfo.accessorType.isSimpleType) {
      val childType = childInfo.accessorType
      appendln(" {")
      append(indent).appendln("  writer.startTag(${toLiteral(owner.nsUri)}, ${toLiteral(childInfo.name)}, ${toLiteral(
            owner.nsPrefix)});")
      append(indent).appendln("  writer.text(${childInfo.readJava(valueRef)});")
      append(indent).appendln("  writer.endTag(${toLiteral(owner.nsUri)}, ${toLiteral(childInfo.name)}, ${toLiteral(
            owner.nsPrefix)});")
      append(indent).appendln("}")
    } else /*if (childInfo.elemType==AnyType::class.java)*/ {
      appendln("${AbstractXmlWriter::class.java.ref}.serialize(writer, ${valueRef});")
//      } else {
//        throw ProcessingException("Don't know how to serialize child ${childInfo.name} type ${childInfo.elemType.typeName}")
    }
  }

}

private inline fun createJavaFile(packageName: String, className: String, block: JavaFile.()->Unit): JavaFile {
  return JavaFile(packageName, className).apply(block)
}