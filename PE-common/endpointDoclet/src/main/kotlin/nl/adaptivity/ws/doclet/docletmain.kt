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

import com.sun.javadoc.*
import nl.adaptivity.process.messaging.GenericEndpoint
import nl.adaptivity.rest.annotations.RestMethod
import nl.adaptivity.rest.annotations.RestParam
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

      targetClasses.forEach { root.printNotice("Found endpoint ${it.name()}") }

      targetClasses.forEach { processSoap(options, it, root) }

      targetClasses.forEach { processRest(options, it, root) }


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
    get () = annotation["operationName"]?.toString() ?: method.name()


}

operator fun AnnotationDesc.get(name:String) = elementValues()
      .asSequence()
      .filter { pair -> pair.element().name()==name }
      .map { pair -> pair.value().value() }
      .firstOrNull()

data class RestMethodInfo(val method:MethodDoc, val annotation:AnnotationDesc) {
  val anchor=method.name()

  val path: String
    get () = annotation["path"]!!.toString()

  val httpMethod: String
    get () = annotation["method"]?.toString()?.substringAfter('.') ?: "NULL"

  val query: Array<String>?
    get () = annotation["query"]?.let { it as Array<String> }

  val post: Array<String>?
    get () = annotation["post"]?.let { it as Array<String> }

  val get: Array<String>?
    get () = annotation["get"]?.let { it as Array<String> }

  val contentType: String
    get () = annotation["get"]?.toString() ?: ""

  fun parameters(): Sequence<RestParamInfo> = method.parameters().asSequence().map { RestParamInfo(it, method.paramTag(it)) }
}

fun ExecutableMemberDoc.paramTag(param: Parameter): ParamTag? =
      this.paramTags().firstOrNull { it.parameterName()==param.name() }

data class RestParamInfo(val parameter: Parameter, val paramTag: ParamTag?) {
  val annotation = parameter.annotations().asSequence().firstOrNull {a->a.annotationType().qualifiedTypeName()== RestParam::class.java.canonicalName }

  val name:String = annotation?.get("name")?.toString() ?: parameter.name()

  val dataType:String = parameter.typeName()

  val paramType:String = annotation?.get("type")?.toString()?.substringAfter('.') ?: "QUERY"

}

fun String.substringAfter(match:Char):String = lastIndexOf(match).let { pos->
  if (pos>0) substring(pos+1) else this
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
          root.printNotice("Found webmethod ${it.method.name()} with annotation: ${annot.annotationType().qualifiedTypeName()}")
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
        appendln(m.method.commentText())
        appendln()
        table("Parameter", "Type", "Description") {
          m.method.parameters().forEach { param ->
            val tag = m.method.paramTags().firstOrNull { it.parameterName() == param.name() }
            row {
              col { text(param.name()) }
              col { text(param.type().typeName()) }
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

}

private fun processRest(options: Options, classDoc: ClassDoc, root: RootDoc) {
  fun restMethodAnot(method:MethodDoc) = method.annotations().find { a->a.annotationType().qualifiedTypeName()== RestMethod::class.java.canonicalName}

  fun isRestMethod(method:MethodDoc):Boolean = restMethodAnot(method)!=null

  if (classDoc.methods().any{method -> restMethodAnot(method)!=null}) {
    val outFile = File(options.destDir, "REST/${classDoc.qualifiedTypeName().replace("/",".")}.md")
    outFile.parentFile.mkdirs()
    outFile.createNewFile()
    outFile.writer().markDown {
      val restMethods = classDoc
            .methods()
            .map { method -> restMethodAnot(method)?.let {annot -> RestMethodInfo(method, annot)} }
            .filterNotNull().toList()

      heading1("REST methods for ${classDoc.qualifiedTypeName()}")
      appendln()
      table("Method", "HTTP", "Path", "Description") {
        restMethods.forEach { it ->
          val (method, annot) = it
          root.printWarning("Found Restmethod ${it.method.name()} for path: ${it.path} with annotation: ${annot.annotationType().qualifiedTypeName()}")
          row {
            col { link("#${it.anchor}", it.method.name()) }
            col { text(it.httpMethod) }
            col { text(it.path) }
            col { text(method.commentText().let{it -> it.substring(0,it.indexOf('.')+1)}) }
          }
        }
      }
      appendln()

      restMethods.forEach { m ->
        appendln()
        heading2("<a name=\"${m.anchor}\">${m.method.name()}</a>")
        m.method.commentText().wordWrap(80).forEach{appendln(it)}

        appendln()
        appendln("**Path**: ${m.path}")
        appendln()
        table("Parameter", "Param type", "Data Type", "Description") {
          m.parameters().forEach { param ->
            val tag = m.method.paramTags().firstOrNull { it.parameterName() == param.parameter.name() }
            row {
              col { text(param.name) }
              col { text(param.paramType) }
              col { text(param.dataType) }
              col { if(tag!=null) text(tag.parameterComment()) }
            }
          }
          row {
            col { text("return") }
            col { m.contentType }
            col { text(m.method.returnType().typeName()) }
            col { m.method.tags("return").firstOrNull()?.let { text(it.text()) } }
          }

        }
      }


    }

  }
}