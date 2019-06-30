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

package nl.adaptivity.ws.soap

import net.devrieze.util.*
import net.devrieze.util.Tripple
import net.devrieze.util.Types
import net.devrieze.util.security.SYSTEMPRINCIPAL
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.io.Writable
import nl.adaptivity.io.WritableReader
import nl.adaptivity.messaging.MessagingException
import nl.adaptivity.xmlutil.util.ICompactFragment
import nl.adaptivity.util.DomUtil
import nl.adaptivity.xmlutil.*
import org.w3.soapEnvelope.Envelope
import org.w3.soapEnvelope.Header
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.Text

import javax.xml.XMLConstants
import javax.xml.bind.*
import javax.xml.bind.annotation.XmlSeeAlso
import javax.xml.namespace.QName
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.Source
import javax.xml.transform.dom.DOMResult
import javax.xml.transform.dom.DOMSource
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.security.Principal
import java.util.*
import kotlin.reflect.KClass


/**
 * Static helper method that helps with handling soap requests and responses.
 *
 * TODO fix this big mess by refactoring it.
 *
 * @author Paul de Vrieze
 */
object SoapHelper {

    val SOAP_ENVELOPE_NS = "http://www.w3.org/2003/05/soap-envelope"

    val SOAP_RPC_RESULT = QName("http://www.w3.org/2003/05/soap-rpc", "result")

    val RESULT = "!@#\$Result_MARKER::"

    // TODO should no longer be needed as DarwinMessenger will do the class loader shenanigans.
    object XmlDeserializationHelper {
        fun deserializationTarget(clazz: Class<*>,
                                  paramContext: Array<Annotation>): Class<out XmlDeserializerFactory<*>>? {
            val annotation = getFactoryAnnotation(clazz, paramContext)
            return annotation?.value?.java
        }

        private fun getFactoryAnnotation(clazz: Class<*>, paramContext: Array<Annotation>?): XmlDeserializer? {
            run {
                val result = clazz.getAnnotation(XmlDeserializer::class.java)
                if (result != null) {
                    return result
                }
            }
            if (paramContext != null) {
                for (a in paramContext) {
                    if (a is SoapSeeAlso) {
                        for (refClass in a.value) {
                            val result = refClass.java.getAnnotation(XmlDeserializer::class.java)
                            if (result != null) {
                                return result
                            }
                        }
                    }
                }
            }
            return null
        }

        @Throws(XmlException::class)
        fun <T> deserialize(target: Class<T>, deserializer: Class<out XmlDeserializerFactory<out T>>, value: Node): T {
            try {
                val factory = deserializer.newInstance()
                return target.cast(factory.deserialize(XmlStreaming.newReader(DOMSource(value))))
            } catch (e: InstantiationException) {
                throw XmlException(e)
            } catch (e: IllegalAccessException) {
                throw XmlException(e)
            }

        }
    }

    @Throws(JAXBException::class, XmlException::class)
    fun createMessage(pOperationName: QName, pParams: List<Tripple<String, out Class<*>, *>>): Source {
        return createMessage(pOperationName, null, pParams)
    }

    /**
     * Create a Source encapsulating a soap message for the given operation name
     * and parameters.
     *
     * @param operationName The name of the soap operation (name of the first
     * child of the soap body)
     * @param headers A list of optional headers to add to the message.
     * @param params The parameters of the message
     * @return a Source that encapsulates the message.
     * @throws JAXBException
     */
    @Throws(JAXBException::class, XmlException::class)
    fun createMessage(operationName: QName, headers: List<*>?, params: List<Tripple<String, out Class<*>, *>>): Source {
        val db: DocumentBuilder
        run {
            val dbf = DocumentBuilderFactory.newInstance()
            dbf.isNamespaceAware = true
            try {
                db = dbf.newDocumentBuilder()
            } catch (e: ParserConfigurationException) {
                throw RuntimeException(e)
            }
        }
        val resultDoc = db.newDocument()

        val envelope = createSoapEnvelope(resultDoc)
        if (headers != null && headers.size > 0) {
            createSoapHeader(envelope, headers)
        }
        val body = createSoapBody(envelope)
        val message = createBodyMessage(body, operationName)
        for (param in params) {
            addParam(message, param)
        }
        return DOMSource(resultDoc)
    }

    /**
     * Create a SOAP envelope in the document and return the body element.
     *
     * @param pDoc The document that needs to contain the envelope.
     * @return The body element.
     */
    private fun createSoapEnvelope(pDoc: Document): Element {
        val envelope = pDoc.createElementNS(SOAP_ENVELOPE_NS, "soap:Envelope")
        envelope.setAttribute("encodingStyle", SoapMethodWrapper.SOAP_ENCODING.toString())
        pDoc.appendChild(envelope)
        return envelope
    }

    private fun createSoapHeader(pEnvelope: Element, pHeaders: List<*>): Element {
        val ownerDoc = pEnvelope.ownerDocument
        val header = ownerDoc.createElementNS(SOAP_ENVELOPE_NS, "soap:Header")
        pEnvelope.appendChild(header)
        for (headerElem in pHeaders) {
            if (headerElem is Node) {
                val node = ownerDoc.importNode(headerElem, true)
                header.appendChild(node)
            }
            if (headerElem is XmlSerializable) {
                try {
                    val out = XmlStreaming.newWriter(DOMResult(header))
                    headerElem.serialize(out)
                } catch (e: MessagingException) {
                    throw e
                } catch (e: Exception) { // XMLException
                    throw MessagingException(e)
                }

            } else {
                try {
                    val marshaller: Marshaller
                    run {
                        val context: JAXBContext
                        if (headerElem is JAXBElement<*> || headerElem==null) {
                            context = JAXBContext.newInstance()
                        } else {
                            context = JAXBContext.newInstance(headerElem.javaClass)
                        }
                        marshaller = context.createMarshaller()
                    }
                    marshaller.marshal(headerElem, header)

                } catch (e: JAXBException) {
                    throw MessagingException(e)
                }

            }
        }

        return header
    }

    private fun createSoapBody(pEnvelope: Element): Element {
        val body = pEnvelope.ownerDocument.createElementNS(SOAP_ENVELOPE_NS, "soap:Body")
        pEnvelope.appendChild(body)
        return body
    }

    /**
     * Create the actual body of the SOAP message.
     *
     * @param pBody The body element in which the body needs to be embedded.
     * @param pOperationName The name of the wrapping name (the operation name).
     * @return
     */
    private fun createBodyMessage(pBody: Element, pOperationName: QName): Element {
        val pResultDoc = pBody.ownerDocument

        val message = pResultDoc.createElementNS(pOperationName.namespaceURI, pOperationName.toCName())

        pBody.appendChild(message)
        return message
    }

    @Throws(JAXBException::class, XmlException::class)
    private fun addParam(pMessage: Element, pParam: Tripple<String, out Class<*>, *>): Element {
        val ownerDoc = pMessage.ownerDocument
        val prefix: String
        if (pMessage.prefix != null && pMessage.prefix.length > 0) {
            prefix = pMessage.prefix + ':'
        } else {
            prefix = ""
        }
        if (pParam.elem1 === RESULT) { // We need to create the wrapper that refers to the actual result.
            val rpcprefix = DomUtil.getPrefix(pMessage, SOAP_RPC_RESULT.namespaceURI)
            val wrapper: Element
            if (rpcprefix == null) {
                wrapper = ownerDoc.createElementNS(SOAP_RPC_RESULT.namespaceURI, "rpc:" + SOAP_RPC_RESULT.localPart)
                wrapper.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:rpc", SOAP_RPC_RESULT.namespaceURI)
            } else {
                wrapper = ownerDoc.createElementNS(SOAP_RPC_RESULT.namespaceURI,
                                                   rpcprefix + ":" + SOAP_RPC_RESULT.localPart)
            }
            pMessage.appendChild(wrapper)

            wrapper.appendChild(ownerDoc.createTextNode(prefix + pParam.elem3.toString()))
            return wrapper
        }

        val wrapper = ownerDoc.createElementNS(pMessage.namespaceURI, prefix + pParam.elem1)
        wrapper.prefix = pMessage.prefix
        pMessage.appendChild(wrapper)

        val paramType = pParam.elem2
        if (pParam.elem3 == null) {
            // don't add anything
        } else if (Types.isPrimitive(paramType) || Types.isPrimitiveWrapper(paramType)) {
            wrapper.appendChild(ownerDoc.createTextNode(pParam.elem3.toString()))
        } else if (XmlSerializable::class.java.isAssignableFrom(paramType)) {
            val xmlWriter = XmlStreaming.newWriter(DOMResult(wrapper))
            (pParam.elem3 as XmlSerializable).serialize(xmlWriter)
            xmlWriter.close()
        } else if (Collection::class.java.isAssignableFrom(paramType)) {
            val params = pParam.elem3 as Collection<*>
            val paramTypes = HashSet<Class<*>>()

            for (elem in params) {
                if (elem!=null) {
                    paramTypes.add(elem.javaClass)
                }
            }

            val marshaller: Marshaller
            run {
                val context = JAXBContext.newInstance(*paramTypes.toTypedArray())
                marshaller = context.createMarshaller()
            }
            for (elem in params) {
                marshaller.marshal(elem, wrapper)
            }
        } else if (Node::class.java.isAssignableFrom(paramType)) {
            var param = pParam.elem3 as Node
            param = ownerDoc.importNode(param, true)
            wrapper.appendChild(param)
        } else if (pParam.elem3 === SYSTEMPRINCIPAL) {
            val tag = ownerDoc.createElementNS(SYSTEMPRINCIPAL.NS, SYSTEMPRINCIPAL.TAG)
            tag.appendChild(ownerDoc.createTextNode(SYSTEMPRINCIPAL.KEY.toString()))
            wrapper.appendChild(tag)
        } else if (Principal::class.java.isAssignableFrom(paramType)) {
            wrapper.appendChild(ownerDoc.createTextNode((pParam.elem3 as Principal).name))
        } else {
            val marshaller: Marshaller
            run {
                val context = JAXBContext.newInstance(paramType)
                marshaller = context.createMarshaller()
            }
            marshaller.marshal(pParam.elem3, wrapper)
        }
        return wrapper
    }

    @Throws(XmlException::class)
    fun <T> processResponse(resultType: Class<T>,
                            context: Array<Class<*>>,
                            useSiteAnnotations: Array<Annotation>,
                            source: Source): T? {
        val `in` = XmlStreaming.newReader(source)
        val env = Envelope.deserialize(`in`)
        return processResponse(resultType, context, useSiteAnnotations, env)
    }

    private fun <T> processResponse(resultType: Class<T>,
                                    context: Array<Class<*>>,
                                    useSiteAnnotations: Array<Annotation>,
                                    env: Envelope<out ICompactFragment>): T? {
        val bodyContent = env.body!!.bodyContent
        try {
            val reader = bodyContent!!.getXmlReader()
            while (reader.hasNext()) {
                when (reader.next()) {
                    EventType.TEXT                 -> if (!isXmlWhitespace(reader.text)) {
                        throw XmlException("Unexpected text content")
                    }
                    EventType.IGNORABLE_WHITESPACE -> {
                    }
                    EventType.START_ELEMENT        -> {
                        val params = unmarshalWrapper(reader)
                        // This is the parameter wrapper
                        return unMarshalNode(null, resultType, context, useSiteAnnotations, params[RESULT])
                    }
                    else                           -> throw XmlException("Unexpected content in soap response")
                }// whitespace is ignored
            }
            return null // no nodes
        } catch (e: XmlException) {
            throw RuntimeException(e)
        }

    }

    @Throws(XmlException::class)
    fun <T> processResponse(resultType: Class<T>,
                            context: Array<Class<*>>,
                            useSiteAnnotations: Array<Annotation>,
                            pContent: Writable): T? {
        val env = Envelope.deserialize(XmlStreaming.newReader(WritableReader(pContent)))
        return processResponse(resultType, context, useSiteAnnotations, env)
    }

    @Throws(XmlException::class)
    internal fun unmarshalWrapper(reader: XmlReader): LinkedHashMap<String, Node> {
        reader.skipPreamble()
        reader.require(EventType.START_ELEMENT, null, null)
        val params = LinkedHashMap<String, Node>()
        val wrapperName = reader.name
        var returnName: QName? = null
        outer@ while (reader.hasNext()) {
            when (reader.next()) {
                EventType.TEXT                 -> if (!isXmlWhitespace(reader.text)) {
                    throw XmlException("Unexpected text content")
                }
                EventType.IGNORABLE_WHITESPACE -> {
                }
                EventType.START_ELEMENT        -> {
                    if (reader.isElement("http://www.w3.org/2003/05/soap-rpc", "result")) {
                        val s = reader.readSimpleElement().toString()
                        val i = s.indexOf(':')
                        if (i >= 0) {
                            returnName = QName(reader.getNamespaceURI(s.substring(0, i)), s.substring(i + 1))
                        } else {
                            val ns = reader.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX)
                            if (ns == null || ns.isEmpty()) {
                                returnName = QName(s)
                            } else {
                                returnName = QName(ns, s)
                            }
                        }
                        if (params.containsKey(returnName.localPart)) {
                            params[RESULT] = params.remove(returnName.localPart)!!
                        }

                    } else if (returnName != null && reader.isElement(returnName)) {
                        params[RESULT] = DomUtil.childToNode(reader)
                        reader.require(EventType.END_ELEMENT, returnName.namespaceURI, returnName.localPart)
                    } else {
                        params[reader.localName] = DomUtil.childToNode(reader)
                        reader.require(EventType.END_ELEMENT, null, null)
                    }
                }
                EventType.END_ELEMENT          -> break@outer
                else                           -> throw XmlException("Unexpected content in SOAP invocation")
            }// whitespace is ignored
            // This is the parameter wrapper
        }


        reader.require(EventType.END_ELEMENT, wrapperName.namespaceURI, wrapperName.localPart)
        return params
    }


    private fun getParamMap(bodyParamRoot: Node): LinkedHashMap<String, Node> {
        val params = LinkedHashMap<String, Node>()
        run {

            var child: Node? = bodyParamRoot.firstChild
            var returnName: String? = null
            while (child != null) {
                if (child.nodeType == Node.ELEMENT_NODE) {
                    if ("http://www.w3.org/2003/05/soap-rpc" == child.namespaceURI && "result" == child.localName) {
                        returnName = child.textContent
                        val i = returnName!!.indexOf(':')
                        if (i >= 0) {
                            returnName = returnName.substring(i + 1)
                        }
                        if (params.containsKey(returnName)) {
                            val value = params.remove(returnName)!!
                            params[RESULT] = value
                        }

                    } else if (returnName != null && child.localName == returnName) {
                        params[RESULT] = child
                    } else {

                        params[child.localName] = child
                    }
                }
                child = child.nextSibling
            }
        }
        return params
    }

    fun getHeaderMap(pHeader: Header?): MutableMap<String, Node> {
        if (pHeader == null) {
            return mutableMapOf()
        }
        val result = LinkedHashMap<String, Node>()
        for (frag in pHeader.any) {

            val node = DomUtil.childToNode(frag.getXmlReader())
            result[node.localName] = node
        }
        return result
    }

    internal fun <T> unMarshalNode(pMethod: Method?,
                                   pClass: Class<T>,
                                   jaxbContext: Array<out Class<*>>?,
                                   useSiteAnnotations: Array<out Annotation>,
                                   pAttrWrapper: Node?): T? {
        var value: Node? = pAttrWrapper?.firstChild
        while (value != null && value is Text && isXmlWhitespace(value.data)) {
            value = value.nextSibling
        }
        var result: Any?
        if (value != null && !pClass.isInstance(value)) {
            if (Types.isPrimitive(pClass) || Types.isPrimitiveWrapper(pClass)) {
                result = Types.parsePrimitive(pClass, value.textContent)
            } else if (Enum::class.java.isAssignableFrom(pClass)) {
                val value = value.textContent
                result = (pClass as Class<Enum<*>>).enumConstants.first { it.name == value }
            } else if (pClass.isAssignableFrom(Principal::class.java)
                       && value is Element
                       && SYSTEMPRINCIPAL.NS.equals(value.namespaceURI)
                       && SYSTEMPRINCIPAL.TAG.equals(value.localName)) {
                val key = java.lang.Long.parseLong(value.textContent)
                if (key == SYSTEMPRINCIPAL.KEY) {
                    result = SYSTEMPRINCIPAL
                } else {
                    throw IllegalArgumentException("Invalid system principal key!! $key")
                }
            } else if (pClass.isAssignableFrom(Principal::class.java) && value is Text) {
                result = SimplePrincipal(value.data)
            } else if (CharSequence::class.java.isAssignableFrom(pClass) && value is Text) {
                if (pClass.isAssignableFrom(String::class.java)) {
                    result = value.data
                } else if (pClass.isAssignableFrom(StringBuilder::class.java)) {
                    val `val` = value.data
                    result = StringBuilder(`val`.length)
                    result.append(`val`)
                } else {
                    throw UnsupportedOperationException(
                            "Can not unmarshal other strings than to string or stringbuilder")
                }
            } else {
                var helper: Class<*>? = null
                var deserializabletargetType: Class<*>? = null
                try {
                    helper = Class.forName(XmlDeserializationHelper::class.java.name, true, pClass.classLoader)
                    val deserializationTarget = helper!!.getMethod("deserializationTarget", Class::class.java,
                                                                   arrayOfNulls<Annotation>(0).javaClass)
                    deserializabletargetType = deserializationTarget.invoke(null, pClass,
                                                                            useSiteAnnotations) as Class<*>
                } catch (e: ClassNotFoundException) {
                    throw RuntimeException(e)
                } catch (e: NoSuchMethodException) {
                    throw RuntimeException(e)
                } catch (e: InvocationTargetException) {
                    throw RuntimeException(e)
                } catch (e: IllegalAccessException) {
                    throw RuntimeException(e)
                }

                if (deserializabletargetType != null) {
                    try {
                        result = helper.getMethod("deserialize", Class::class.java, Class::class.java,
                                                  Node::class.java).invoke(null, pClass, deserializabletargetType,
                                                                           value)
                    } catch (e: IllegalAccessException) {
                        throw RuntimeException(e)
                    } catch (e: NoSuchMethodException) {
                        throw RuntimeException(e)
                    } catch (e: InvocationTargetException) {
                        if (e.cause is XmlException) {
                            throw (e.cause as XmlException)
                            throw UnsupportedOperationException("Unreachable")
                        } else if (e.cause is RuntimeException) {
                            throw e.cause as RuntimeException
                        } else {
                            throw RuntimeException(e)
                        }
                    }

                } else {

                    if (value.nextSibling != null && value.nextSibling is Element) {
                        throw UnsupportedOperationException(
                                "Collection parameters not yet supported: " + pMethod!!.toGenericString() + " found: '" + DomUtil.toString(
                                        value.nextSibling) + "' in " + DomUtil.toString(value.parentNode))
                    }
                    try {
                        val context: JAXBContext

                        if (pClass.isInterface) {
                            context = newJAXBContext(pMethod, Arrays.asList(*jaxbContext!!))
                        } else {
                            val list = ArrayList<Class<*>>(1 + (jaxbContext?.size ?: 0))
                            list.add(pClass)
                            if (jaxbContext != null) {
                                list.addAll(Arrays.asList(*jaxbContext))
                            }
                            context = newJAXBContext(pMethod, list)
                        }
                        val um = context.createUnmarshaller()
                        if (pClass.isInterface) {
                            if (value is Text) {
                                result = value.data
                            } else {
                                result = um.unmarshal(value)

                                if (result is JAXBElement<*>) {
                                    result = result.value
                                }
                            }
                        } else {
                            val umresult: JAXBElement<*>
                            umresult = um.unmarshal(value, pClass)
                            result = umresult.value
                        }

                    } catch (e: JAXBException) {
                        throw MessagingException("Error unmarshalling node " + pAttrWrapper!!, e)
                    }

                }
            }
        } else {
            result = value
        }
        if (Types.isPrimitive(pClass)) {
            return result as T?
        }

        return if (result == null) null else pClass.cast(result)
    }

    @Throws(JAXBException::class)
    private fun newJAXBContext(pMethod: Method?, pClasses: List<Class<*>>): JAXBContext {
        val classList: Array<Class<*>>
        val seeAlso: XmlSeeAlso?
        if (pMethod != null) {
            val clazz = pMethod.declaringClass
            seeAlso = clazz.getAnnotation(XmlSeeAlso::class.java)
        } else {
            seeAlso = null
        }
        if (seeAlso != null && seeAlso.value.isNotEmpty()) {

            classList = (seeAlso.value.map<KClass<*>, Class<*>> { javaClass } + pClasses).toTypedArray()

        } else {
            classList = pClasses.toTypedArray()
        }
        return JAXBContext.newInstance(*classList)
    }

}
