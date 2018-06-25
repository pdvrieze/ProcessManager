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

import net.devrieze.util.Annotations
import nl.adaptivity.rest.annotations.RestParam
import nl.adaptivity.rest.annotations.RestParamType
import nl.adaptivity.ws.soap.SoapSeeAlso

import javax.jws.WebMethod
import javax.jws.WebParam
import javax.xml.namespace.QName
import java.io.*
import java.lang.reflect.*
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance


/**
 * This class can automatically generate soap based clients for endpoints.
 *
 * @author Paul de Vrieze
 */
object MessagingSoapClientGenerator {

    private val METHODSORT = Comparator<Method> { m1, m2 ->
        // First sort on name
        val result = m1.name.compareTo(m2.name)
        if (result != 0) {
            result
        } else {

            val param1 = m1.getParameterTypes()
            val param2 = m2.getParameterTypes()
            // Next on parameter list length
            if (param1.size != param2.size) {
                if (param1.size > param2.size) 1 else -1
            } else {
                param1.indices
                    .map { param1[it].simpleName.compareTo(param2[it].simpleName) }
                    .firstOrNull { it != 0 } ?:
                // This should not happen as methods can not be the same but for return type in Java (but in jvm can)
                m1.getReturnType().getSimpleName().compareTo(m2.getReturnType().getSimpleName())
            }
        }
    }

    private var _errorCount = 0


    private class ParamInfo(val mType: Type, val mName: String)

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
            outClass == null                        -> {
                System.err.println("Outclass needs to be specified as source when multiple input classes are present")
                showHelp()
                return
            }
            else                                    -> outClass
        }


        val fs = FileSystems.getDefault()
        try {
            val pkgdir = destPkg.replace(".", fs.separator)
            val classfilename = "$outClass.java"
            val outfile = fs.getPath(dstdir, pkgdir, classfilename)

            // Ensure the parent directory of the outfile exists.
            if (!outfile.parent.toFile().mkdirs()) {
                if (!outfile.parent.toFile().exists()) {
                    System.err.println("Could not create the directory " + outfile.parent)
                    System.exit(2)
                }
            }

            URLClassLoader.newInstance(getUrls(cp)).use { urlclassloader ->

                if (inClasses.size == 1) {
                    writeOutFile(inClasses[0], destPkg, outClass, fs, outfile, urlclassloader)
                } else {
                    for (inClass in inClasses) {
                        val newOutClass = getDefaultOutClassName(inClass)
                        val newOutFile = outfile.parent.resolve("$newOutClass.kt")
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
        val endpointClass: Class<*>
        try {
            endpointClass = classloader.loadClass(inClass)
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
            ++_errorCount
            return
        }

        val pkgname = pkg.replace(fs.separator, ".")
        try {
            generateJava(outfile, endpointClass, pkgname, outClass)
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
        println("  -dstdir <dirname>  : The directory to write the generated java files")
        println("  <inputclass>       : The Endpoint that needs a client")
    }

    @Throws(IOException::class)
    private fun generateJava(outfile: Path, endpointClass: Class<*>, pkgname: String, outClass: String) {
        BufferedWriter(FileWriter(outfile.toFile())).use { out -> generateJava(out, endpointClass, pkgname, outClass) }
    }

    @Throws(IOException::class)
    private fun generateJava(out: Writer, endpointClass: Class<*>, pkgname: String, outClass: String) {
        writeHead(out, endpointClass, pkgname)

        val buffer = CharArrayWriter(0x2000)

        val imports = HashMap<String, String>()
        imports["URI"] = "java.net.URI"
        imports["Future"] = "java.util.concurrent.Future"
        imports["Arrays"] = "java.util.Arrays"
        imports["JAXBElement"] = "javax.xml.bind.JAXBElement"
        imports["JAXBException"] = "javax.xml.bind.JAXBException"
        imports["XmlException"] = "nl.adaptivity.xml.XmlException"
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

        writeClassBody(buffer, endpointClass, outClass, imports)

        val finalStrings = ArrayList(imports.values)
        Collections.sort(finalStrings)
        var oldPrefix: String? = null
        for (str in finalStrings) {
            val prefix = if (str.indexOf('.') < 0) "" else str.substring(0, str.indexOf('.'))
            if (oldPrefix != null && oldPrefix != prefix) {
                out.write('\n')
            }

            out.append("import ").append(str).append(";\n")
            oldPrefix = prefix
        }
        out.write('\n')

        buffer.writeTo(out)
    }

    @Throws(IOException::class)
    private fun writeHead(out: Writer, endpointClass: Class<*>, pkgname: String) {
        out.write("/*\n")
        out.write(" * Generated by MessagingSoapClientGenerator.\n")
        out.write(" * Source class: ")
        out.write(endpointClass.canonicalName)
        out.write("\n */\n\n")

        out.write("package ")
        out.write(pkgname)
        out.write(";\n\n")
    }

    @Throws(IOException::class)
    private fun writeClassBody(out: Writer,
                               endpointClass: Class<*>,
                               outClass: String,
                               imports: MutableMap<String, String>) {
        out.write("@SuppressWarnings(\"all\")\n")
        out.write("public class ")
        out.write(outClass)
        out.write(" {\n\n")

        // Write service location constants / variables.
        var finalService = false
        try {
            val instance: EndpointDescriptor
            if (endpointClass.isAnnotationPresent(Descriptor::class.java)) {
                instance = endpointClass.getAnnotation(Descriptor::class.java).value.java.newInstance()
            } else {
                instance = endpointClass.newInstance() as EndpointDescriptor
            }

            out.write("  private static final QName SERVICE = " + qnamestring(instance.serviceName!!) + ";\n")
            out.write(appendString(StringBuilder("  private static final String ENDPOINT = "),
                                   instance.endpointName).append(";\n").toString())
            if (instance.endpointLocation != null) {
                out.write(appendString(StringBuilder("  private static final URI LOCATION = URI.create("),
                                       instance.endpointLocation!!.toString()).append(");\n\n").toString())
            } else {
                out.write("  private static final URI LOCATION = null;\n\n")
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
            out.write("  private static QName SERVICE = null;\n")
            out.write("  private static String ENDPOINT = null;\n")
            out.write("  private static URI LOCATION = null;\n\n")
        }

        // Constructor
        out.write("  private ")
        out.write(outClass)
        out.write("() { }\n\n")

        writeMethods(out, endpointClass, imports)


        // Initializer in case we can't figure out the locations
        if (!finalService) {
            out.write("  private static void init(QName service, String endpoint, URI location) {\n")
            out.write("    SERVICE=service;\n")
            out.write("    ENDPOINT=endpoint;\n")
            out.write("    LOCATION=location;\n")
            out.write("  }\n\n")
        }

        out.write("}\n")
    }

    @Throws(IOException::class)
    private fun writeMethods(out: Writer, endpointClass: Class<*>, imports: MutableMap<String, String>) {
        val methods = endpointClass.methods
        Arrays.sort(methods, METHODSORT)
        for (method in methods) {
            val annotation = method.getAnnotation(WebMethod::class.java)
            if (annotation != null) {
                writeMethod(out, method, annotation, imports)
            }
        }
    }

    @Throws(IOException::class)
    private fun writeMethod(out: Writer, method: Method, webMethod: WebMethod, imports: MutableMap<String, String>) {
        var principalName: String? = null

        val methodName: String = webMethod.operationName.let { if (it.isEmpty()) method.name else it }

        out.write("  public static")
        writeTypeParams(out, method.typeParameters, imports)
        out.write(" Future<")
        writeType(out, method.genericReturnType, false, false, imports)
        out.write("> ")
        out.write(methodName)
        out.write("(")
        var firstParam = true
        val params = ArrayList<ParamInfo>()
        run {
            var paramNo = 0
            val parameterTypes = method.genericParameterTypes
            for (i in parameterTypes.indices) {
                val paramType = parameterTypes[i]
                var name: String? = null
                var isPrincipal = false
                name = method.parameterAnnotations[paramNo]
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
                    }.firstOrNull { it.isNotEmpty() } ?: "param$paramNo"

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
                writeType(out, paramType, true, method.isVarArgs && i == parameterTypes.size - 1, imports)
                out.write(' ')
                out.write(name)

                ++paramNo
            }
        }
        val seeAlso = Annotations.getAnnotation(method.annotations, SoapSeeAlso::class.java)
        if (seeAlso == null) {
            out.write(
                ", CompletionListener completionListener, Class<?>... jaxbcontext) throws JAXBException, XmlException {\n")
        } else {
            out.write(", CompletionListener completionListener) throws JAXBException, XmlException {\n")
        }
        run {
            var paramNo = 0
            for (param in params) {
                out.write("    final Tripple<String, Class<")
                val rawtype = getRawType(param.mType)
                writeType(out, rawtype, false, false, imports)
                out.write(">, ")
                writeType(out, param.mType, false, false, imports)
                out.write("> param")
                out.write(Integer.toString(paramNo))
                out.write(" = Tripple.<String, Class<")
                writeType(out, rawtype, false, false, imports)
                out.write(">, ")
                writeType(out, param.mType, false, false, imports)
                out.write(">tripple(")
                out.write(appendString(StringBuilder(), param.mName).append(", ").toString())
                if (rawtype!!.isArray) {
                    out.write("Array.class, ")
                } else {
                    writeType(out, rawtype, true, false, imports)
                    out.write(".class, ")
                }
                out.write(param.mName)
                out.write(");\n")

                ++paramNo
            }
        }
        out.write("\n")
        out.write("    Source message = SoapHelper.createMessage(new QName(")
        if (webMethod.operationName != null) {
            out.write(appendString(StringBuilder(), webMethod.operationName).append("), ").toString())
        } else {
            out.write(appendString(StringBuilder(), method.name).append("), ").toString())
        }
        if (principalName != null) {
            out.write(
                "Arrays.asList(new JAXBElement<String>(new QName(\"http://adaptivity.nl/ProcessEngine/\",\"principal\"), String.class, "
                + principalName + ".getName())), ")
        }
        out.write("Arrays.<Tripple<String, ? extends Class<?>, ?>>asList(")
        for (i in params.indices) {
            if (i > 0) {
                out.write(", ")
            }
            out.write("param$i")
        }
        out.write("));\n\n")

        out.write("    EndpointDescriptor endpoint = new EndpointDescriptorImpl(SERVICE, ENDPOINT, LOCATION);\n\n")

        out.write(
            "    return (Future) MessagingRegistry.sendMessage(new SendableSoapSource(endpoint, message), completionListener, ")
        writeType(out, getRawType(method.genericReturnType), true, false, imports)

        out.write(".class, ")

        if (seeAlso == null) {
            out.write("jaxbcontext")
        } else {
            writeClassArray(out, seeAlso.value, imports)
        }

        out.write(");\n")

        out.write("  }\n\n")

    }

    @Throws(IOException::class)
    private fun writeClassArray(out: Writer, value: Array<KClass<*>>, imports: MutableMap<String, String>) {
        if (value.isEmpty()) {
            out.write("new Class<?>[0]")
            return
        }
        out.write("new Class<?>[] {")
        writeType(out, value[0].java, false, false, imports)
        for (i in 1 until value.size) {
            out.write(", ")
            writeType(out, value[i].java, false, false, imports)
        }
        out.write('}')
    }

    private fun getRawType(type: Type): Class<*>? {
        if (type is Class<*>) {
            return type
        } else if (type is ParameterizedType) {
            return getRawType(type.rawType)
        } else if (type is GenericArrayType) {
            val componentType = getRawType(type.genericComponentType)
            return java.lang.reflect.Array.newInstance(componentType, 0).javaClass
        } else if (type is TypeVariable<*>) {
            for (bound in type.bounds) {
                return getRawType(bound)
            }
            return Any::class.java
        } else if (type is WildcardType) {
            for (bound in type.upperBounds) {
                return getRawType(bound)
            }
            return Any::class.java

        }
        return null
    }

    @Throws(IOException::class)
    private fun writeType(out: Writer,
                          type: Type?,
                          allowPrimitive: Boolean,
                          varArgs: Boolean,
                          imports: MutableMap<String, String>) {
        if (type is ParameterizedType) {
            val parameterizedType = type as ParameterizedType?
            writeType(out, parameterizedType!!.rawType, allowPrimitive, varArgs, imports)
            writeTypes(out, parameterizedType.actualTypeArguments, imports)
        } else if (type is GenericArrayType) {
            val genericArrayType = type as GenericArrayType?
            writeType(out, genericArrayType!!.genericComponentType, true, false, imports)
            if (varArgs) {
                out.write("...")
            } else {
                out.write("[]")
            }
        } else if (type is TypeVariable<*>) {
            val typeVariable = type as TypeVariable<*>?
            out.write(typeVariable!!.name)
        } else if (type is WildcardType) {
            val wildcardType = type as WildcardType?
            out.write('?')
            run {
                val lower = wildcardType!!.lowerBounds
                if (lower.size > 0) {
                    out.write(" super ")
                    var first = true

                    for (bound in lower) {
                        if (first) {
                            first = false
                        } else {
                            out.write(" & ")
                        }
                        writeType(out, bound, false, varArgs, imports)
                    }
                }
            }
            run {
                val upper = wildcardType!!.upperBounds
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
            }
        } else if (type is Class<*>) {
            val clazz = type as Class<*>?
            if (allowPrimitive || !clazz!!.isPrimitive) {
                val canonname = clazz!!.canonicalName
                val pkg = getPackage(canonname)
                val cls = getName(canonname)
                if ("java.lang" == pkg || clazz.isPrimitive) {
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
                out.write(toBox(clazz.simpleName))
            }
        } else {
            throw IllegalArgumentException(
                "Type parameter of type " + type!!.javaClass.canonicalName + " not supported")
        }
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
    private fun writeTypes(out: Writer, types: Array<Type>, imports: MutableMap<String, String>) {
        if (types.size > 0) {
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

    private fun qnamestring(qName: QName): String {
        val result = StringBuilder()

        result.append("new QName(")
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

inline fun Writer.write(ch:Char) = write(ch.toInt())