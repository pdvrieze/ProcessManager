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

package nl.adaptivity.ws.doclet

import com.sun.javadoc.AnnotationDesc
import com.sun.javadoc.ClassDoc
import com.sun.javadoc.MethodDoc
import com.sun.javadoc.RootDoc
import nl.adaptivity.process.messaging.GenericEndpoint
import java.util.*
import javax.jws.WebMethod

/**
 * Created by pdvrieze on 10/04/16.
 */

class WsDoclet {
  companion object {
    @JvmStatic
    public fun start(root: RootDoc):Boolean {
      val genericEndpoint = root.classNamed(GenericEndpoint::class.java.name)
      val destDir = readOptions(root.options())

      val targetClasses=root.classes().filter { classDoc -> classDoc.subclassOf(genericEndpoint) }.toList()

      targetClasses.forEach { root.printWarning("Found endpoint ${it.name()}") }

      targetClasses.forEach { processSoap(destDir, it, root) }


      return true
    }

    @JvmStatic
    fun optionLength(s:String):Int = when(s) {
      "-d" -> 2
      "-doctitle" -> 2
      "-windowtitle" -> 2
      else -> 0
    }

  }

}


private fun readOptions(options: Array<out Array<out String>>):String? {
  return options.filter { it[0]=="-d" }.map { it[1] }.lastOrNull()
}

private fun processSoap(destDir: String?, classDoc: ClassDoc, root: RootDoc) {

  fun webMethodAnot(method:MethodDoc) = method.annotations().find { a->a.annotationType().qualifiedTypeName()== WebMethod::class.java.canonicalName}

  fun isWebMethod(method:MethodDoc):Boolean = webMethodAnot(method)!=null

  classDoc
        .methods()
        .associate { method -> method to webMethodAnot(method) }
        .filterValues{  it!=null }
        .forEach {
          val (method, annot) = it
          root.printWarning("Found webmethod ${method.name()} with annotation: ${annot!!.annotationType().qualifiedTypeName()}")
        }


//  throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
}

private fun processRest(destDir: String?, classDoc: ClassDoc, root: RootDoc) {
//  throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
}