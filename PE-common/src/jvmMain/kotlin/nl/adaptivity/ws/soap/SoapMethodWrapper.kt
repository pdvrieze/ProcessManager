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

import net.devrieze.util.Annotations
import net.devrieze.util.Tripple
import nl.adaptivity.messaging.MessagingException
import nl.adaptivity.process.ProcessConsts.Soap
import nl.adaptivity.process.engine.MessagingFormatException
import nl.adaptivity.process.messaging.ActivityResponse
import nl.adaptivity.util.activation.writeToStream
import nl.adaptivity.util.kotlin.arrayMap
import nl.adaptivity.ws.WsMethodWrapper
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.isIgnorable
import nl.adaptivity.xmlutil.util.ICompactFragment
import org.w3.soapEnvelope.Envelope
import org.w3.soapEnvelope.Header
import org.w3c.dom.Node
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.net.URI
import java.security.Principal
import java.util.*
import java.util.logging.Logger
import javax.activation.DataSource
import javax.jws.WebMethod
import javax.jws.WebParam
import javax.jws.WebService
import javax.servlet.http.HttpServletResponse
import javax.xml.bind.JAXB
import javax.xml.bind.JAXBException
import javax.xml.namespace.QName
import javax.xml.transform.Source
import javax.xml.transform.TransformerException


class SoapMethodWrapper(owner: Any, method: Method) : WsMethodWrapper(owner, method) {

    /*
       * If the ActivityResponse was created by a different classloader like
       * when we directly invoke the endpoint through DarwinMessenger shortcut
       * we will have to resort to reflection instead of direct invocation. This
       * should still beat going through tcp-ip hand out.
       */ val resultSource: Source
        get() {
            val result = result
            if (result is Source) {
                return result
            }

            val params: List<Tripple<String, out Class<*>, *>>
            val headers: List<Any>
            if (result == null && method.returnType == Void::class.java) {
                params = Arrays.asList(
                    Tripple.tripple(SoapHelper.RESULT, String::class.java, "result"),
                    Tripple.tripple("result", Void::class.java, null)
                )
                headers = emptyList()

            } else if (result is ActivityResponse<*,*>) {
                val activityResponse = result as ActivityResponse<out Any, out Any?>
                params = listOf(
                    Tripple.tripple(SoapHelper.RESULT, String::class.java, "result"),
                    Tripple.tripple("result", activityResponse.returnType.java, activityResponse.returnValue)
                )
                headers = listOf<Any>(result)
            } else if (result != null && ActivityResponse::class.java.canonicalName == result.javaClass.canonicalName) {

                val returnType: Class<*>
                val returnValue: Any
                try {
                    returnType = result.javaClass.getMethod("getReturnType").invoke(result) as Class<*>
                    returnValue = result.javaClass.getMethod("getReturnValue").invoke(result)
                } catch (e: IllegalArgumentException) {
                    throw MessagingException(e)
                } catch (e: SecurityException) {
                    throw MessagingException(e)
                } catch (e: IllegalAccessException) {
                    throw MessagingException(e)
                } catch (e: InvocationTargetException) {
                    throw MessagingException(e)
                } catch (e: NoSuchMethodException) {
                    throw MessagingException(e)
                }

                params = Arrays.asList(
                    Tripple.tripple(SoapHelper.RESULT, String::class.java, "result"),
                    Tripple.tripple("result", returnType, returnValue)
                )
                headers = listOf(result)

            } else {

                params = Arrays.asList(
                    Tripple.tripple(SoapHelper.RESULT, String::class.java, "result"),
                    Tripple.tripple("result", method.returnType, result)
                )
                headers = emptyList()
            }
            try {
                return SoapHelper.createMessage(QName(method.name + "Response"), headers, params)
            } catch (e: XmlException) {
                throw MessagingException(e)
            } catch (e: JAXBException) {
                throw MessagingException(e)
            }

        }

    fun unmarshalParams(source: Source, attachments: Map<String, DataSource>) {
        val envelope = JAXB.unmarshal(source, Envelope::class.java) as Envelope<ICompactFragment>
        unmarshalParams(envelope, attachments)

    }

    fun unmarshalParams(envelope: Envelope<out ICompactFragment>, attachments: Map<String, out DataSource>) {
        if (paramsInitialised) {
            throw IllegalStateException("Parameters have already been unmarshalled")
        }

        ensureNoUnunderstoodHeaders(envelope)
        processSoapHeader(envelope.header)
        val es = envelope.body.encodingStyle
        if (es == SOAP_ENCODING) {
            try {
                processSoapBody(envelope, attachments)
            } catch (e: XmlException) {
                throw MessagingFormatException("Failure to process message body", e)
            }

        } else {
            throw MessagingFormatException("Ununderstood message body")
        }
    }

    private fun ensureNoUnunderstoodHeaders(envelope: Envelope<*>) {
        if (envelope.header.blocks.isNotEmpty()) {
            throw MessagingFormatException("Soap header not understood")
        }
    }

    private fun processSoapHeader(header: Header?) {
        // TODO Process soap headers
        //
        /* For now just ignore headers, i.e. none understood. Principal is recorded but not handled */
    }

    @Throws(XmlException::class)
    private fun processSoapBody(
        envelope: Envelope<out ICompactFragment>,
        attachments: Map<String, DataSource>
    ) {
        val body = envelope.body
        val reader = body.child.getXmlReader()
        reader.nextTag()
        assertRootNode(reader)


        val params = SoapHelper.unmarshalWrapper(reader)
        reader.require(EventType.END_ELEMENT, null, null)
        if (reader.hasNext()) {
            reader.next()
        }
        while (reader.hasNext() && reader.isIgnorable()) {
            reader.next()
        }
        if (reader.eventType === EventType.START_ELEMENT) {
            throw MessagingFormatException("Multiple body elements not expected")
        }

        val headers = SoapHelper.getHeaderMap(envelope.header)

        val parameterTypes = method.parameterTypes
        val parameterAnnotations = method.parameterAnnotations

        this.params = arrayOfNulls(parameterTypes.size)

        for (i in parameterTypes.indices) {
            val annotation = Annotations.getAnnotation(parameterAnnotations[i], WebParam::class.java)
            val name: String
            if (annotation == null) {
                if (params.isEmpty()) {
                    throw MessagingFormatException(
                        "Missing parameter " + (i + 1) + " of type " + parameterTypes[i] + " for method " +
                            method
                    )
                }
                name = params.keys.iterator().next()
            } else {
                name = annotation.name
            }
            val value: Node?
            if (annotation != null && annotation.header) {
                if (parameterTypes[i].isAssignableFrom(Principal::class.java) && envelope.header!!.principal != null) {
                    this.params[i] = envelope.header!!.principal
                    continue //Finish the parameter, we don't need to unmarshal
                } else if (parameterTypes[i].isAssignableFrom(
                        String::class.java
                    ) && envelope.header!!.principal != null
                ) {
                    this.params[i] = envelope.header!!.principal!!.name
                    continue
                } else {
                    value = headers.remove(name)
                }
            } else {
                value = params.remove(name)
            }

            if (value == null) {
                throw MessagingFormatException("Parameter \"$name\" not found")
            }

            val seeAlso = Annotations.getAnnotation(parameterAnnotations[i], SoapSeeAlso::class.java)
            val jbc = seeAlso?.value?.arrayMap { it.java } ?: emptyArray()
            this.params[i] = SoapHelper.unMarshalNode(method, parameterTypes[i], jbc, parameterAnnotations[i], value)

        }
        if (params.size > 0) {
            Logger.getLogger(javaClass.canonicalName).warning("Extra parameters in message: " + params.keys.toString())
        }
    }

    @Throws(XmlException::class)
    private fun assertRootNode(reader: XmlReader) {
        val wm = method.getAnnotation(WebMethod::class.java)
        if (wm == null || wm.operationName == "") {
            if (!(reader.localName == method.name)) {
                throw MessagingFormatException("Root node does not correspond to operation name")
            }
        } else {
            if (!(reader.localName == wm.operationName)) {
                throw MessagingFormatException("Root node does not correspond to operation name")
            }
        }
        val ws = method.declaringClass.getAnnotation(WebService::class.java)
        if (!(ws == null || ws.targetNamespace == "")) {
            if (!(ws.targetNamespace == reader.namespaceURI)) {
                throw MessagingFormatException("Root node does not correspond to operation namespace")
            }
        }
    }

    companion object {

        val SOAP_ENCODING = URI.create(Soap.SOAP_ENCODING_NS)

        fun marshalResult(response: HttpServletResponse, source: Source) {
            response.contentType = "application/soap+xml"
            try {
                source.writeToStream(response.outputStream)
            } catch (e: TransformerException) {
                throw MessagingException(e)
            } catch (e: IOException) {
                throw MessagingException(e)
            }

        }
    }

}
