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
import java.io.File
import java.net.URLClassLoader

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
