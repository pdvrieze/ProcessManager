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

import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlSerializable
import nl.adaptivity.xml.XmlStreaming
import nl.adaptivity.xml.XmlWriter
import nl.adaptivity.xml.schema.annotations.AnyType
import nl.adaptivity.xml.schema.annotations.Element
import java.io.CharArrayWriter
import java.io.File
import java.io.StringWriter
import java.io.Writer
import java.lang.reflect.Type

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
    fun doGenerate(outputDir: File, input:Iterable<File>) {
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

    private fun generateFactory(outDir:File, typeInfo:FullTypeInfo) {
      val factoryClassName = typeInfo.factoryClassName
      val packageName = typeInfo.packageName
      val nsPrefix = typeInfo.nsPrefix

      val fileCreator = createJavaFile(packageName, factoryClassName) {
        emptyConstructor()
        method("serialize", null, arrayOf(XmlException::class.java), XmlWriter::class.java to "writer", typeInfo.elemType to "value") {
          val writer:XmlWriter = XmlStreaming.newWriter(StringWriter())

          appendln("    writer.startTag(${toLiteral(typeInfo.nsUri)}, ${toLiteral(typeInfo.elementName)}, ${toLiteral(nsPrefix)});")

          typeInfo.attributes.forEach { attr ->
            appendln()
            appendln("    {")
            appendln("      ${attr.type.ref} attrValue = value.${attr.readJava};")
            if (attr.isOptional && !attr.type.isPrimitive) {
              appendln("      if (attrValue!=null) writer.attribute(null, ${toLiteral(attr.name)}, null, attrValue);")
            } else {
              appendln("      writer.attribute(null, ${toLiteral(attr.name)}, null, attrValue==null ? ${toLiteral(attr.default)} : attrValue);")
            }
            appendln("    }")
          }

          typeInfo.children.forEach { childInfo ->
            val childType = childInfo.typeInfo

            appendln("    {")
            val attrname = if (childType.isCollection) "childValues" else "childValue"
            appendln("      ${childType.accessorType.ref} $attrname = value.${childInfo.readJava};")
            var indent:String
            if (childType.isCollection) {
              indent = " ".repeat(8)
              appendln("      for(${childInfo.elemType.ref} childValue: childValues)")
            } else indent = " ".repeat(6)

            writeSerializeChild(indent, typeInfo, childInfo, "childValue")

            if (childType.isCollection) {
              appendln("      }")
            }
            appendln("    }")

          }

          appendln()
          appendln("    writer.endTag(${toLiteral(typeInfo.nsUri)}, ${toLiteral(typeInfo.elementName)}, ${toLiteral(nsPrefix)});")
        }

      }

      val outputFile = getFactorySourceFile(outDir, packageName, factoryClassName)
      outputFile.writer().use {
        fileCreator.appendTo(it)
      }
    }

    private fun Appendable.writeSerializeChild(indent: String, owner: FullTypeInfo, childInfo: ChildInfo, valueRef: String) {
      append(indent).append("if (").append(valueRef).append("!=null) ")
      if (childInfo.typeInfo.isSerializable) {
        append(valueRef).appendln("serialize(writer);")
      } else if (childInfo.typeInfo.isSimpleType) {
        val childType = childInfo.typeInfo
        appendln(" {")
        append(indent).appendln("  writer.startTag(${toLiteral(owner.nsUri)}, ${toLiteral(childInfo.name)}, ${toLiteral(
              owner.nsPrefix)});")
        append(indent).appendln("  writer.text(${childInfo.readJava});")
        append(indent).appendln("  writer.endTag(${toLiteral(owner.nsUri)}, ${toLiteral(childInfo.name)}, ${toLiteral(
              owner.nsPrefix)});")
        append(indent).appendln("}")
      } else /*if (childInfo.elemType==AnyType::class.java)*/ {
        append(indent).appendln("AbstractXmlWriter.serialize(writer, ${childInfo.readJava})")
//      } else {
//        throw ProcessingException("Don't know how to serialize child ${childInfo.name} type ${childInfo.elemType.typeName}")
      }
    }

    private fun getFactorySourceFile(outDir: File, packageName:String, factoryClassName:String): File {
      val directory = packageName.replace('.','/');
      val filename = factoryClassName +".java"
      return File(outDir, "${directory}/$filename").apply { parentFile.mkdirs(); createNewFile() }
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

    toClass().let { c ->
      if (c.isPrimitive) { return c.name }
      if (c.`package`.name!="java.lang") imports.add(c)
      return c.canonicalName
    }
  }

  inline fun method(name:String, returnType:Type?, vararg parameters:Pair<Type,String>, crossinline body:Appendable.()->Unit) {
    return method(name, returnType, emptyArray(), *parameters) { body() }
  }

  inline fun method(name:String, returnType:Type?, throws: Array<out Class<out Throwable>>, vararg parameters:Pair<Type,String>, crossinline body:Appendable.()->Unit) {
    classBody.add {
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


}

private inline fun createJavaFile(packageName: String, className: String, block: JavaFile.()->Unit): JavaFile {
  return JavaFile(packageName, className).apply(block)
}
