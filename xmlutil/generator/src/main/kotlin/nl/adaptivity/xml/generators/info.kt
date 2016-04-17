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

@file:JvmName("Info")

package nl.adaptivity.xml.generators

import nl.adaptivity.xml.XmlSerializable
import nl.adaptivity.xml.schema.annotations.Element
import java.io.File
import java.io.Writer

/*
 * Simple information creating package that just lists the possible, and the available classes.
 * Created by pdvrieze on 15/04/16.
 */
class Info {
  companion object {
    /**
     * File generator for an overview of element information.
     */
    @JvmStatic
    fun doGenerate(output: Writer, input: Iterable<File>) {
      visitClasses(input) { clazz ->
        if (XmlSerializable::class.java.isAssignableFrom(clazz)) {
          val elementAnnot = clazz.getAnnotationsByType(Element::class.java).apply { if (size > 1) throw ProcessingException("Unexpected multiple annotations")}.firstOrNull()
          if (elementAnnot!=null) {
            output.append("Found annotated serializable: ")
          } else {
            output.append("Missing Element annotation for: ")
          }
          output.appendln(clazz.name)
        }
      }
    }

    /**
     * This function implements task level generation. Support that as well in this file.
     */
    @JvmStatic
    fun doGenerate(output: File, input:Iterable<File>) {
      if (output.isFile) {
        output.writer().use { doGenerate(it, input) }
      } else {
        if (!output.exists()) { output.mkdirs() }
        File(output, "xmlinfo.txt").let {
          it.createNewFile()
          it.writer().use { doGenerate(it, input) }
        }
      }
    }
  }
}
