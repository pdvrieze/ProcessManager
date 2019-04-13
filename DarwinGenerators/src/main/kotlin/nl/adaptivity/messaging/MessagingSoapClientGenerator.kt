/*
 * Copyright (c) 2018.
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

package nl.adaptivity.messaging

import nl.adaptivity.rest.annotations.RestParam
import nl.adaptivity.rest.annotations.RestParamType
import nl.adaptivity.ws.soap.SoapSeeAlso

import javax.jws.WebMethod
import javax.jws.WebParam
import javax.xml.namespace.QName
import java.io.*
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.*
import kotlin.reflect.*
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.jvmErasure


/**
 * This class can automatically generate soap based clients for endpoints.
 *
 * @author Paul de Vrieze
 */
object MessagingSoapClientGenerator {

    private val METHODSORT = Comparator<KFunction<*>> { m1, m2 ->
        // First sort on name
        val result = m1.name.compareTo(m2.name)
        if (result != 0) {
            result
        } else {

            val param1 = m1.parameters.map { it.type }
            val param2 = m2.parameters.map { it.type }
            // Next on parameter list length
            if (param1.size != param2.size) {
                if (param1.size > param2.size) 1 else -1
            } else {
                param1.indices
                    .map { param1[it].jvmErasure.java.simpleName.compareTo(param2[it].jvmErasure.java.simpleName) }
                    .firstOrNull { it != 0 } ?:
                // This should not happen as methods can not be the same but for return type in Java (but in jvm can)
                m1.returnType.jvmErasure.java.simpleName.compareTo(m2.returnType.jvmErasure.java.simpleName)
            }
        }
    }

    private var _errorCount = 0


    private class ParamInfo(val type: KType, val name: String)

    /**
     * @param args Standard arguments to the generator
     */
    @JvmStatic
    fun main(args: Array<String>) {
        var pkg: String? = null
        var outClass: String? = null
        val inClasses = ArrayList<String>()
        var cp = "."
        var dstdir = "."
        var i = 0
        while (i < args.size) {
            when (args[i]) {
                "-package" -> {
                    if (pkg != null) {
                        showHelp()
                        return
                    }
                    pkg = args[++i]
                }
                "-out"     -> {
                    if (outClass != null) {
                        showHelp()
                        return
                    }
                    outClass = args[++i]
                    val j = outClass.lastIndexOf('.')
                    if (j >= 0 && pkg == null) {
                        pkg = outClass.substring(0, j)
                        outClass = outClass.substring(j + 1)
                    }
                }
                "-cp"      -> {
                    if ("." != cp) {
                        showHelp()
                        return
                    }
                    cp = args[++i]
                }
                "-dstdir"  -> {
                    if ("." != dstdir) {
                        showHelp()
                        return
                    }
                    dstdir = args[++i]
                }
                "-help"    -> {
                    showHelp()
                    return
                }
                else       -> {
                    if (args[i][0] == '-') {
                        System.err.println("Unsupported arguments: " + args[i])
                        showHelp()
                        return
                    } else {
                        inClasses.add(args[i])
                    }
                }
            }
            ++i
        }
        try {
            generateClientJava(pkg, outClass, inClasses, cp, dstdir)
        } catch (e: Exception) {
            e.printStackTrace()
            ++_errorCount
            System.exit(_errorCount)
        }

        System.exit(0)
    }

    private fun generateClientJava(destPkg: String?,
                                   outClass: String?,
                                   inClasses: List<String>,
                                   cp: String,
                                   dstdir: String) {
        val outClass = when {
            inClasses.isEmpty() || destPkg == null  -> {
                System.err.println("Not all three of inclass, outclass and package have been provided")
                showHelp()
                return
            }
            inClasses.size == 1 && outClass == null -> getDefaultOutClassName(inClasses[0])
            else                                    -> outClass
        }


        val fs = FileSystems.getDefault()
        try {
            val pkgdir = destPkg.replace(".", fs.separator)
            val outfile = when (outClass) {
                null -> fs.getPath(dstdir, pkgdir)
                else -> fs.getPath(dstdir, pkgdir, "$outClass.kt")
            }

            // Ensure the parent directory of the outfile exists.
            if (!outfile.parent.toFile().mkdirs()) {
                if (!outfile.parent.toFile().exists()) {
                    System.err.println("Could not create the directory " + outfile.parent)
                    System.exit(2)
                }
            }

            URLClassLoader.newInstance(getUrls(cp)).use { urlclassloader ->

                if (inClasses.size == 1) {
                    writeOutFile(inClasses[0], destPkg, outClass!!, fs, outfile, urlclassloader)
                } else {
                    val outDir = if(outfile.toFile().isDirectory) outfile else outfile.parent
                    for (inClass in inClasses) {
                        val newOutClass = getDefaultOutClassName(inClass)
                        val newOutFile = outDir.resolve("$newOutClass.kt")
                        writeOutFile(inClass, destPkg, newOutClass, fs, newOutFile, urlclassloader)
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            System.exit(3)
        }

    }

    private fun getDefaultOutClassName(inClass: String): String {
        return inClass.substring(inClass.lastIndexOf('.') + 1) + "Client"
    }

    private fun writeOutFile(inClass: String,
                             pkg: String,
                             outClass: String,
                             fs: FileSystem,
                             outfile: Path,
                             classloader: URLClassLoader) {
        val endpointClass: KClass<*>
        try {
            endpointClass = classloader.loadClass(inClass).kotlin
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
            ++_errorCount
            return
        }

        val pkgname = pkg.replace(fs.separator, ".")
        try {
            generateKotlin(outfile, endpointClass, pkgname, outClass)
        } catch (e: IOException) {
            e.printStackTrace()
            ++_errorCount
        }

    }

    private fun getUrls(classPath: String): Array<URL> {
        val fs = FileSystems.getDefault()
        val result = ArrayList<URL>()

        try {
            for (element in classPath.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                if (element.length > 0) {
                    var url: URL
                    try {
                        val uri = URI.create(element)
                        url = uri.toURL()
                    } catch (e: IllegalArgumentException) {
                        val file = fs.getPath(element)
                        url = file.normalize().toUri().toURL()
                    }

                    result.add(url)
                }
            }
        } catch (e: MalformedURLException) {
            System.err.println("Invalid classpath element")
            e.printStackTrace()
        }

        return result.toTypedArray()

    }

    private fun showHelp() {
        println("Usage:")
        println("  -help              : Show this message, ignore everything else")
        println("  -out <classname>   : The output classname to generate")
        println("  -package <pkgname> : The output package name to generate")
        println("  -cp <path>         : The classpath to look for source classes and their dependencies")
        println("  -dstdir <dirname>  : The directory to write the generated kotlin files")
        println("  <inputclass>       : The Endpoint that needs a client")
    }

    private fun generateKotlin(outfile: Path, endpointClass: KClass<*>, pkgname: String, outClass: String) {
        BufferedWriter(FileWriter(outfile.toFile())).use { out -> generateKotlin(out, endpointClass, pkgname, outClass) }
    }

    private fun generateKotlin(out: Writer, endpointClass: KClass<*>, pkgname: String, outClass: String) {
        out.writeHead(endpointClass, pkgname)

        val buffer = CharArrayWriter(0x2000)

        val imports = HashMap<String, String>()
        imports["URI"] = "java.net.URI"
        imports["Future"] = "java.util.concurrent.Future"
        imports["Arrays"] = "java.util.Arrays"
        imports["JAXBElement"] = "javax.xml.bind.JAXBElement"
        imports["JAXBException"] = "javax.xml.bind.JAXBException"
        imports["XmlException"] = "nl.adaptivity.xmlutil.XmlException"
        imports["QName"] = "javax.xml.namespace.QName"
        imports["Source"] = "javax.xml.transform.Source"
        imports["Tripple"] = "net.devrieze.util.Tripple"
        imports["CompletionListener"] = "nl.adaptivity.messaging.CompletionListener"
        imports["Endpoint"] = "nl.adaptivity.messaging.Endpoint"
        imports["EndpointDescriptor"] = "nl.adaptivity.messaging.EndpointDescriptor"
        imports["EndpointDescriptorImpl"] = "nl.adaptivity.messaging.EndpointDescriptorImpl"
        imports["MessagingRegistry"] = "nl.adaptivity.messaging.MessagingRegistry"
        imports["SendableSoapSource"] = "nl.adaptivity.messaging.SendableSoapSource"
        imports["SoapHelper"] = "nl.adaptivity.ws.soap.SoapHelper"

        buffer.writeClassBody(endpointClass, outClass, imports)

        val finalStrings = ArrayList(imports.values)
        Collections.sort(finalStrings)
        var oldPrefix: String? = null
        for (str in finalStrings) {
            val prefix = if (str.indexOf('.') < 0) "" else str.substring(0, str.indexOf('.'))
            if (oldPrefix != null && oldPrefix != prefix) {
                out.write('\n')
            }

            out.append("import ").append(str).append("\n")
            oldPrefix = prefix
        }
        out.write('\n')

        buffer.writeTo(out)
    }

    private fun Writer.writeHead(endpointClass: KClass<*>, pkgname: String) {
        write("/*\n")
        write(" * Generated by MessagingSoapClientGenerator.\n")
        write(" * Source class: ")
        write(endpointClass.java.canonicalName)
        write("\n */\n\n")
        write("@file:Suppress(\"all\")\n")

        write("package ")
        write(pkgname)
        write("\n\n")
    }

    private fun Writer.writeClassBody(endpointClass: KClass<*>,
                                      outClass: String,
                                      imports: MutableMap<String, String>) {
        write("object ")
        write(outClass)
        write(" {\n\n")

        this.writeMethods(endpointClass, imports)

        // Write service location constants / variables.
        var finalService = false
        try {
            val instance: EndpointDescriptor
            if (endpointClass.java.isAnnotationPresent(Descriptor::class.java)) {
                instance = endpointClass.findAnnotation<Descriptor>()!!.value.createInstance()
            } else {
                instance = endpointClass.createInstance() as EndpointDescriptor
            }
            write("  @JvmStatic")
            write("  private val SERVICE: QName = ${qnamestring(instance.serviceName!!)}\n")
            write(appendString(
                StringBuilder("  private const val ENDPOINT = "),
                instance.endpointName).append("\n").toString())
            write("  @JvmStatic")
            if (instance.endpointLocation != null) {
                write(appendString(
                    StringBuilder("  private val LOCATION = URI.create("),
                    instance.endpointLocation!!.toString()).append(")\n\n").toString())
            } else {
                write("  private val LOCATION: URI? = null\n\n")
            }
            finalService = true
        } catch (e: ClassCastException) { /*
                                            * Ignore failure to instantiate. We
                                            * just generate different code.
                                            */
            e.printStackTrace()
        } catch (e: InstantiationException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }

        if (!finalService) {
            write("  @JvmStatic")
            write("  private lateinit var SERVICE: QName\n")
            write("  @JvmStatic")
            write("  private lateinit var ENDPOINT: String\n")
            write("  @JvmStatic")
            write("  private lateinit var LOCATION: URI\n\n")

            write("  private fun init(service: QName, endpoint: String, location: URI) {\n")
            write("    SERVICE=service\n")
            write("    ENDPOINT=endpoint\n")
            write("    LOCATION=location\n")
            write("  }\n\n")
        }

        write("}\n")
    }

    private fun Writer.writeMethods(endpointClass: KClass<*>,
                                    imports: MutableMap<String, String>) {
        val methods = endpointClass.memberFunctions.sortedWith(METHODSORT)
        for (method in methods) {
            val annotation = method.findAnnotation<WebMethod>()
            if (annotation != null) {
                writeMethod(this, method, annotation, imports)
            }
        }
    }

    @Throws(IOException::class)
    private fun writeMethod(out: Writer, method: KFunction<*>, webMethod: WebMethod, imports: MutableMap<String, String>) {
        var principalName: String? = null

        val methodName: String = webMethod.operationName.let { if (it.isEmpty()) method.name else it }

        out.write("  @JvmStatic\n")
        out.write("  @Throws(JAXBException::class, XmlException::class)")
        out.write("  fun ")
        writeTypeParams(out, method.typeParameters, imports)
        out.write(methodName)
        out.write("(")
        var firstParam = true
        val params = ArrayList<ParamInfo>()
        run {
            var paramNo = 0
            val parameters = method.parameters
            for (i in 1 until parameters.size) {
                val parameter = parameters[i]
                val paramType = parameter.type
                var isPrincipal = false
                val name = parameter.annotations
                    .asSequence()
                    .mapNotNull { an ->
                        when {
                            an is WebParam
                                 -> an.name

                            an is RestParam && an.type === RestParamType.PRINCIPAL
                                 -> {
                                isPrincipal = true
                                an.name.let { if (it.isEmpty()) "principal" else it }
                            }
                            else -> null
                        }
                    }.firstOrNull { it.isNotEmpty() } ?: parameter.name ?: "param$paramNo"

                if (isPrincipal) {
                    principalName = name
                } else {
                    params.add(ParamInfo(paramType, name))
                }
                if (firstParam) {
                    firstParam = false
                } else {
                    out.write(", ")
                }
                out.write(name)
                out.write(": ")
                val isVarArgs = parameter.isVararg
                if (isVarArgs) out.write("vararg ")
                writeType(out, paramType, true, isVarArgs, imports)

                ++paramNo
            }
        }
        out.write(", completionListener: CompletionListener<")
        writeType(out, method.returnType, false, false, imports)

        val seeAlso = method.findAnnotation<SoapSeeAlso>()
        if (seeAlso == null) {
            out.write(
                ">?, vararg jaxbcontext: Class<*>)")
        } else {
            out.write(">?)")
        }
        out.write(": Future<")
        writeType(out, method.returnType, false, false, imports)
        out.write("> {\n")

        run {
            var paramNo = 0
            for (param in params) {
                val rawtype = getRawType(param.type)
                out.write("    val param${paramNo} = Tripple.tripple(")
                out.write(appendString(StringBuilder(), param.name).append(", ").toString())
//                if (rawtype.isArray) {
//                    out.write("Array::class.java, ")
//                } else {
                    writeType(out, rawtype, true, false, imports)
                    out.write("::class.java, ")
//                }
                out.write(param.name)
                out.write(")\n")

                ++paramNo
            }
        }
        out.write("\n")
        out.write("    val message = SoapHelper.createMessage(QName(")
        if (webMethod.operationName.isNotEmpty()) {
            out.write(appendString(StringBuilder(), webMethod.operationName).append("), ").toString())
        } else {
            out.write(appendString(StringBuilder(), method.name).append("), ").toString())
        }
        if (principalName != null) {
            out.write(
                "listOf(JAXBElement<String>(QName(\"http://adaptivity.nl/ProcessEngine/\",\"principal\"), String::class.java, "
                + principalName + "?.name)), ")
        }
        out.write("listOf(")
        params.indices.joinTo(out) { "param$it" }
        out.write("))\n\n")

        out.write("    val endpoint = EndpointDescriptorImpl(SERVICE, ENDPOINT, LOCATION)\n\n")

        out.write(
            "    return MessagingRegistry.sendMessage(SendableSoapSource(endpoint, message), completionListener, ")
        writeType(out, getRawType(method.returnType), true, false, imports)

        out.write("::class.java, ")

        if (seeAlso == null) {
            out.write("jaxbcontext")
        } else {
            writeClassArray(out, seeAlso.value, imports)
        }

//        out.write(") as Future<")
//        writeType(out, method.returnType, false, false, imports)
//        out.write(">\n")
        out.write(")\n")

        out.write("  }\n\n")

    }

    @Throws(IOException::class)
    private fun writeClassArray(out: Writer, value: Array<out KClass<*>>, imports: MutableMap<String, String>) {
        if (value.isEmpty()) {
            out.write("emptyArray<Class<?>>()")
            return
        }
        out.write("arrayOf(")
        writeType(out, value[0], false, false, imports)
        for (i in 1 until value.size) {
            out.write(", ")
            writeType(out, value[i], false, false, imports)
        }
        out.write(')')
    }

    private fun getRawType(type: KType): KClass<*> {
        return type.jvmErasure
/*
        return when (type) {
                   is Class<*>
                        -> type
                   is ParameterizedType
                        -> getRawType(type.rawType)
                   is GenericArrayType
                        -> java.lang.reflect.Array.newInstance(
                       getRawType(type.genericComponentType), 0).javaClass
                   is TypeVariable<*>
                        -> type.bounds.asSequence().map {
                       getRawType(it)
                   }.firstOrNull()
                   is WildcardType
                        -> type.upperBounds.asSequence().map {
                       getRawType(it)
                   }.firstOrNull()
                   else -> null
               } ?: Any::class.java
*/
    }

    private fun writeType(out: Writer,
                          projection: KTypeProjection,
                          allowPrimitive: Boolean,
                          varargs: Boolean,
                          imports: MutableMap<String, String>) {
        if (projection.type == null) {
            out.write('*'); return
        }
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (projection.variance) {
            KVariance.IN  -> out.write("in ")
            KVariance.OUT -> out.write("out ")
        }
        writeType(out, projection.type!!, allowPrimitive, varargs, imports)
    }

    private fun writeType(out: Writer,
                          classifier: KClassifier?,
                          allowPrimitive: Boolean,
                          varargs: Boolean,
                          imports: MutableMap<String, String>) {
        when (classifier) {
            null              -> out.write("*")

            is KTypeParameter -> {
                if (classifier.upperBounds.size>1) throw IllegalArgumentException("Multiple upper bounds are not supported yet")
                when (classifier.variance) {
                    KVariance.IN -> out.write("in ")
                    KVariance.OUT -> out.write("out ")
                }
                out.write(classifier.name)
                classifier.upperBounds.singleOrNull()?.run { out.write(": "); writeType(out, this, false, false, imports)}
            }

            is KClass<*>      -> {
                val canonname = classifier.qualifiedName ?: throw IllegalArgumentException("Type is not expressable")
                val pkg = getPackage(canonname)
                val cls = getName(canonname)
                when (pkg) {
                    "java.lang",
                    "kotlin" -> out.write(cls)
                    else -> {
                        val import = imports[cls]
                        if (import == null) {
                            imports[cls] = canonname
                            out.write(cls)
                        } else if (import == canonname) {
                            out.write(cls)
                        } else { // duplicate, use full name
                            out.write(canonname)
                        }
                    }
                }
            }

        }
    }

    private fun writeType(out: Writer,
                          ktype: KType,
                          allowPrimitive: Boolean,
                          varArgs: Boolean,
                          imports: MutableMap<String, String>) {

        writeType(out, ktype.classifier, allowPrimitive, false, imports)
        if (ktype.arguments.isNotEmpty()) {
            writeTypeProjections(out, ktype.arguments, imports)
        }
        if (ktype.isMarkedNullable) {
            out.write('?')
        }
/*


        val jtype = ktype.javaType
        if (jtype is ParameterizedType) {
            writeType(out, jtype.rawType, allowPrimitive, varArgs, imports)
            writeTypeParams(out, jtype.actualTypeArguments, imports)
        } else if (type is GenericArrayType) {
            if (varArgs) {
                writeType(out, type.genericComponentType, true, false, imports)
//                out.write("...")
            } else {
                out.write("Array<")
                writeType(out, type.genericComponentType, true, false, imports)
                out.write(">")
            }
        } else if (type is TypeVariable<*>) {
            out.write(type.name)
        } else if (type is WildcardType) {
            val lower = type.lowerBounds
            val upper = type.upperBounds
            if (lower.isNotEmpty()) {
                out.write("in ")
                writeType(out, lower[0], false, varArgs, imports)
*/
/*
                TODO support multiple bounds

                for (bound in lower) {
                    if (first) {
                        first = false
                    } else {
                        out.write(" & ")
                    }
                    writeType(out, bound, false, varArgs, imports)
                }
*//*


            } else if (upper.isNotEmpty()) {
                out.write("out ")
                writeType(out, upper[0], isNullable, false, varArgs, imports)
*/
/*
                TODO support multiple bounds
                if (!(upper.size == 0 || upper.size == 1 && Any::class.java == upper[0])) {
                    out.write(" extends ")
                    var first = true

                    for (bound in upper) {
                        if (first) {
                            first = false
                        } else {
                            out.write(" & ")
                        }
                        writeType(out, bound, false, varArgs, imports)
                    }

                }
*//*

            } else {
                out.write("*")
            }
        } else if (type is Class<*>) {
            if (type == Any::class.java) {
                out.write("Any")
            } else if (allowPrimitive || !type.isPrimitive) {
                val canonname = type.canonicalName
                val pkg = getPackage(canonname)
                val cls = getName(canonname)
                if ("java.lang" == pkg || type.isPrimitive) {
                    out.write(cls)
                } else {
                    val imp = imports[cls]
                    if (imp == null) {
                        imports[cls] = canonname
                        out.write(cls)
                    } else if (imp == canonname) {
                        out.write(cls)
                    } else { // duplicate, use full name
                        out.write(canonname)
                    }
                }
            } else {
                out.write(toBox(type.simpleName))
            }
        } else {
            throw IllegalArgumentException(
                "Type parameter of type ${type.javaClass.canonicalName} not supported")
        }
*/
    }

    private fun getPackage(name: String): String {
        val i = name.lastIndexOf('.')
        return if (i >= 0) {
            name.substring(0, i)
        } else name
    }

    private fun getName(name: String): String {
        val i = name.lastIndexOf('.')
        return if (i >= 0) {
            name.substring(i + 1)
        } else name
    }

    private fun toBox(simpleName: String): String {
        when (simpleName) {
            "byte"    -> return "Byte"
            "short"   -> return "Short"
            "int"     -> return "Integer"
            "char"    -> return "Character"
            "long"    -> return "Long"
            "float"   -> return "Float"
            "boolean" -> return "Boolean"
            "double"  -> return "Double"
        }
        throw UnsupportedOperationException("Not yet implemented")
    }

    @Throws(IOException::class)
    private fun writeTypeProjections(out: Writer, types: List<KTypeProjection>, imports: MutableMap<String, String>) {
        if (types.isNotEmpty()) {
            out.write('<')
            writeType(out, types[0], false, false, imports)
            for (i in 1 until types.size) {
                out.write(',')
                writeType(out, types[i], false, false, imports)
            }
            out.write('>')
        }

    }

    @Throws(IOException::class)
    private fun writeTypeParams(out: Writer, types: List<KTypeParameter>, imports: MutableMap<String, String>) {
        if (types.isNotEmpty()) {
            out.write('<')
            writeType(out, types[0], false, false, imports)
            for (i in 1 until types.size) {
                out.write(',')
                writeType(out, types[i], false, false, imports)
            }
            out.write('>')
        }

    }
/*

    @Throws(IOException::class)
    private fun writeTypeParams(out: Writer, params: Array<TypeVariable<Method>>, imports: MutableMap<String, String>) {
        if (params.size > 0) {
            out.write('<')
            var first = true
            for (param in params) {
                if (first) {
                    first = false
                } else {
                    out.write(", ")
                }
                out.write(param.name)
                if (param.bounds.size > 0 && !(param.bounds.size == 1 && Any::class.java == param.bounds[0])) {
                    out.write(" extends ")
                    var boundFirst = true
                    for (bound in param.bounds) {
                        if (boundFirst) {
                            boundFirst = false
                        } else {
                            out.write(" & ")
                        }
                        writeType(out, bound, false, false, imports)
                    }
                }
            }
            out.write('>')
        }

    }
*/

    private fun qnamestring(qName: QName): String {
        val result = StringBuilder()

        result.append("QName(")
        appendString(result, qName.namespaceURI).append(", ")
        appendString(result, qName.localPart)
        if (qName.prefix != null) {
            appendString(result.append(", "), qName.prefix)
        }
        result.append(')')
        return result.toString()
    }

    private fun appendString(result: StringBuilder, unescapedStr: String?): StringBuilder {
        if (unescapedStr == null) {
            result.append("null")
        } else {
            result.append('"')
            for (i in 0 until unescapedStr.length) {
                val c = unescapedStr[i]
                when (c) {
                    '\\' -> result.append("\\\\")
                    '"'  -> result.append('\\').append(c)
                    '\t' -> result.append("\\t")
                    '\n' -> result.append("\\n")
                    '\r' -> result.append("\\r")
                    else -> result.append(c)
                }
            }
            result.append('"')
        }
        return result
    }

}

/** No object instances expected. */

inline fun Writer.write(ch: Char) = write(ch.toInt())
