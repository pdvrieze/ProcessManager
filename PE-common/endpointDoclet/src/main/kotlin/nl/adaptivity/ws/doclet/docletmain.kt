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
import java.io.File
import java.util.*
import javax.jws.WebMethod

/**
 * Created by pdvrieze on 10/04/16.
 */

data class Options(var destDir: File?=File("wsDoc"))

class WsDoclet {
  companion object {
    @JvmStatic
    public fun start(root: RootDoc):Boolean {
      val genericEndpoint = root.classNamed(GenericEndpoint::class.java.name)
      val options = readOptions(root.options())

      val targetClasses=root.classes().filter { classDoc -> classDoc.subclassOf(genericEndpoint) }.toList()

      targetClasses.forEach { root.printWarning("Found endpoint ${it.name()}") }

      targetClasses.forEach { processSoap(options, it, root) }


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

data class WebMethodInfo(val method:MethodDoc, val annotation:AnnotationDesc) {
  val anchor=soapName
  val soapName: String
    get () = annotation.elementValues()
            .asSequence()
            .filter { pair -> pair.element().name()=="operationName" }
            .map { pair -> pair.value().value().toString() }
            .firstOrNull() ?: method.name()


}


private fun readOptions(options: Array<out Array<out String>>):Options {
  return Options().apply {
    options.forEach { option->
      when(option[0]) {
        "-d" -> destDir = File(option[1])
      }
    }
  }
}

private fun processSoap(options: Options, classDoc: ClassDoc, root: RootDoc) {

  fun webMethodAnot(method:MethodDoc) = method.annotations().find { a->a.annotationType().qualifiedTypeName()== WebMethod::class.java.canonicalName}

  fun isWebMethod(method:MethodDoc):Boolean = webMethodAnot(method)!=null

  if (classDoc.methods().any{method -> webMethodAnot(method)!=null}) {
    val outFile = File(options.destDir, "SOAP/${classDoc.qualifiedTypeName().replace("/",".")}.md")
    outFile.parentFile.mkdirs()
    outFile.createNewFile()
    outFile.writer().markDown {
      val webMethods = classDoc
            .methods()
            .map { method -> webMethodAnot(method)?.let {annot -> WebMethodInfo(method, annot)} }
            .filterNotNull().toList()

      heading1("SOAP methods for ${classDoc.qualifiedTypeName()}")
      appendln()
      table("Method", "Description") {
        webMethods.forEach { it ->
          val (method, annot) = it
          root.printWarning("Found webmethod ${it.soapName} with annotation: ${annot.annotationType().qualifiedTypeName()}")
          row {
            col { link("#${it.anchor}", it.soapName) }
            col { text(method.commentText().let{it -> it.substring(0,it.indexOf('.')+1)}) }
          }
        }
      }
      appendln()

      webMethods.forEach { m ->
        appendln()
        heading2("<a name=\"${m.anchor}\">${m.soapName}</a>")
        appendln()
        table("Parameter", "Type", "Description") {
          m.method.parameters().forEach { param ->
            val tag = m.method.paramTags().firstOrNull { it.parameterName() == param.name() }
            row {
              col { text(param.name()) }
              col { text(param.type().qualifiedTypeName()) }
              parcol { if(tag!=null) text(tag.parameterComment()) }
            }
          }
          row {
            col { text("return") }
            col { text(m.method.returnType().typeName()) }
            col { m.method.tags("return").firstOrNull()?.let { text(it.text()) } }
          }

        }
      }


    }

  }




//  throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
}

private fun processRest(destDir: String?, classDoc: ClassDoc, root: RootDoc) {
//  throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
}