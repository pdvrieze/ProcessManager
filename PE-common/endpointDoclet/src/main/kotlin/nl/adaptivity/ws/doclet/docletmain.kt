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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.ws.doclet

import com.sun.javadoc.*
import nl.adaptivity.process.messaging.GenericEndpoint
import nl.adaptivity.rest.annotations.RestMethod
import nl.adaptivity.rest.annotations.RestParam
import java.io.File
import javax.jws.WebMethod

/**
 * Created by pdvrieze on 10/04/16.
 */

internal const val WORDWRAP:Int = 80

data class Options(var destDir: File?=File("wsDoc"))

class WsDoclet {
  companion object {
    @JvmStatic
    public fun start(root: RootDoc):Boolean {
      val genericEndpoint = root.classNamed(GenericEndpoint::class.java.name)
      val options = readOptions(root.options())

      root.classes().toList().forEach { root.printNotice("Available class: ${it.name()}")}
      val paramType = root.classes().asSequence().firstOrNull { classDoc -> classDoc.name()?.contains("ParamType") ?:false }
      root.printNotice("ParamType is: $paramType")

      val targetClasses=root.classes().filter { classDoc -> classDoc.subclassOf(genericEndpoint) }.toList()

      targetClasses.forEach { root.printNotice("Found endpoint ${it.name()}") }

      targetClasses.forEach { processSoap(options, it, root) }

      targetClasses.forEach { processRest(options, it, root, paramType) }


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

  val query: Array<String>? by lazy {
    annotation["query"]?.let { (it as Array<AnnotationValue>).map { it.value() as String }.toTypedArray() }
  }

  val post: Array<String>? by lazy {
    annotation["post"]?.let { (it as Array<AnnotationValue>).map { it.value() as String }.toTypedArray() }
  }

  val get: Array<String>? by lazy {
    annotation["get"]?.let { (it as Array<AnnotationValue>).map { it.value() as String }.toTypedArray() }
  }

  val contentType: String
    get () = annotation["get"]?.toString() ?: ""

  fun parameters(): Sequence<RestParamInfo> = method.parameters().asSequence().map { RestParamInfo(it, method.paramTag(it)) }

  val pathWithQueries: CharSequence
    get () {
      val allParams = mutableSetOf<String>().apply {
        query?.let{addAll(it)}
        get?.let{addAll(it)}
      }.toList().sorted()
      return allParams.joinToString("&","$path?") { "$it" }
    }
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
            col { text(method.summary) }
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

private fun processRest(options: Options, classDoc: ClassDoc, root: RootDoc, paramType:ClassDoc?) {
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
      if (!classDoc.commentText().isNullOrBlank()) {
        appendln()
        text(classDoc.commentText())
      }
      appendln()
      table("Method", "HTTP", "Path", "Description") {
        restMethods.forEach { it ->
          val (method, annot) = it
          root.printWarning("Found Restmethod ${it.method.name()} for path: ${it.path} with annotation: ${annot.annotationType().qualifiedTypeName()}")
          row {
            col { link("#${it.anchor}", it.method.name()) }
            col { text(it.httpMethod) }
            col { text(it.pathWithQueries) }
            col { text(method.summary) }
          }
        }
      }
      appendln()

      restMethods.forEach { m ->
        appendln()
        heading2("<a name=\"${m.anchor}\">${m.method.name()}</a>")
        text(m.method.commentText())

        appendln()
        appendln("**Path**: ${m.path}")
        if (((m.query?.size ?:0)+(m.post?.size ?:0)+(m.get?.size ?:0))>0) {
          appendln()
          heading3("Mandatory parameters")
          text("These will determine the actual method invoked, more specific over less specific.")
          appendln()
          table("Kind", "content", "Explanation") {
            m.get?.forEach {
              row{ col { text("GET") }; col { text(it) }; col { text("Must be set on the GET url") } }
            }
            m.post?.forEach {
              row{ col { text("POST") }; col { text(it) }; col { text("Must be set in the POST body") } }
            }
            m.query?.forEach {
              row{ col { text("query") }; col { text(it) }; col { text("Must be set on the GET url or POST body") } }
            }
          }

        }
        appendln()
        heading3("Actual parameters")
        text("Parameters that will be translated to actual changes in method invocation.")
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
            col("return")
            col { m.contentType }
            col { text(m.method.returnType().typeName()) }
            col { m.method.tags("return").firstOrNull()?.let { text(it.text()) } }
          }

        }
      }

      val paramTypes = paramType?.fields()?.filter { it.isStatic && it.isFinal }
      if (paramTypes!=null) {
        appendln()
        heading2("Legend")
        text("There are various parameter types available ${paramType!!.name()}:")
        appendln() // new line to trigger table
        table("Type", "Description") {
          paramTypes.forEach {
            row {
              col(it.name())
              col(it.commentText())
            }
          }
        }
      }

    }

  }
}

val Doc.summary:String get() {
  return commentText().let{it -> it.indexOf('.').let {pos -> if (pos>0) { it.substring(0,pos+1)} else it } }
}