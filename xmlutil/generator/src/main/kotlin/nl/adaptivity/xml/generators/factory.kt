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

@file:JvmName("Factory")

package nl.adaptivity.xml.generators

import nl.adaptivity.xml.XmlSerializable
import nl.adaptivity.xml.XmlStreaming
import nl.adaptivity.xml.XmlWriter
import nl.adaptivity.xml.schema.annotations.Element
import nl.adaptivity.xml.schema.annotations.XmlName
import java.io.File
import java.io.StringWriter
import java.io.Writer
import java.lang.reflect.Method
import kotlin.reflect.KClass

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
            val typeInfo = TypeInfo(clazz, elementAnnot)
            generateFactory(outputDir, typeInfo)
          }
        }
      }
    }

    private fun generateFactory(outDir:File, typeInfo:TypeInfo) {
      val factoryClassName = typeInfo.factoryClassName
      val packageName = typeInfo.packageName
      val nsPrefix = typeInfo.nsPrefix

      val fileCreator = createJavaFile(packageName, factoryClassName) {
        emptyConstructor()

        method("serialize", Void::class.java, XmlWriter::class.java to "writer", typeInfo.javaType to "value") {
          val writer:XmlWriter = XmlStreaming.newWriter(StringWriter())

          appendln("    writer.startTag(${toLiteral(typeInfo.nsUri)}, ${toLiteral(typeInfo.elementName)}, ${toLiteral(nsPrefix)})")
          typeInfo.attributes.forEach { attr ->
            appendln()
            appendln("    {")
            appendln("      ${attr.type.simpleName} attrValue = value.${attr.readJava}")
            if (attr.isOptional && !attr.type.isPrimitive) {
              appendln("      if (attrValue!=null) writer.attribute(null, ${attr.name}, null, attrValue);")
            } else {
              appendln("      writer.attribute(null, ${attr.name}, null, attrValue==null ? ${attr.default} else attrValue);")
            }
            appendln("    }")
          }

          appendln()
          appendln("    writer.endTag(${toLiteral(typeInfo.nsUri)}, ${toLiteral(typeInfo.elementName)}, ${toLiteral(nsPrefix)})")
        }

      }

      val outputFile = getFactorySourceFile(outDir, packageName, factoryClassName)
      outputFile.writer().use {
        fileCreator.appendTo(it)
      }
    }

    private fun getFactorySourceFile(outDir: File, packageName:String, factoryClassName:String): File {
      val directory = packageName.replace('.','/');
      val filename = factoryClassName +".java"
      return File(outDir, directory+filename).apply { parentFile.mkdirs(); createNewFile() }
    }


  }
}

private class JavaFile(val packageName:String, val className:String) {

  val classBody = mutableListOf<Appendable.()->Unit>()
  val imports = mutableSetOf<Class<*>>()

  fun emptyConstructor() {
    classBody.add {
      appendln("public $className() {}")
    }
  }

  inline fun method(name:String, returnType:Class<*>, vararg parameters:Pair<Class<*>,String>, crossinline body:Appendable.()->Unit) {
    classBody.add {
      imports.add(returnType)
      append("  public static final ${returnType.simpleName} ${name}(")
      parameters.joinTo(this) { val (type, name) = it; imports.add(type); "${type.simpleName} ${name}" }
      appendln(") {")
      this.body()
      appendln("  }")
    }
  }

  fun appendTo(writer: Writer): Unit {
    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
  }


}

private inline fun createJavaFile(packageName: String, className: String, block: JavaFile.()->Unit): JavaFile {
  return JavaFile(packageName, className).apply(block)
}
