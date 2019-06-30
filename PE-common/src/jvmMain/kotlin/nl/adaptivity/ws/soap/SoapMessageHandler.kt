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

import net.devrieze.util.PrefixMap
import net.devrieze.util.ValueCollection
import nl.adaptivity.messaging.HttpResponseException
import nl.adaptivity.util.HttpMessage
import nl.adaptivity.xmlutil.util.ICompactFragment
import nl.adaptivity.xmlutil.*
import org.w3.soapEnvelope.Envelope

import javax.activation.DataSource
import javax.jws.WebMethod
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.xml.namespace.QName
import javax.xml.transform.Source

import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.Hashtable
import java.util.concurrent.ConcurrentHashMap


abstract class SoapMessageHandler {

    class ClassLoadedSoapMessageHandler(private val mTarget: Any) : SoapMessageHandler() {

        private val cache = Hashtable<Class<*>, PrefixMap<Method>>()

        @Throws(IOException::class, XmlException::class)
        override fun processRequest(request: HttpMessage, response: HttpServletResponse): Boolean {
            val source = request.body

            val result: Source
            try {
                result = processMessage(source!!, request.attachments)
            } catch (e: HttpResponseException) {
                response.sendError(e.responseCode, e.message)
                return false
            }

            SoapMethodWrapper.marshalResult(response, result)
            return true
        }


        @Throws(XmlException::class)
        private fun processMessage(source: ICompactFragment, pAttachments: Map<String, DataSource>): Source {
            val reader = source.getXmlReader()
            return processMessage(reader, pAttachments)
        }

        @Throws(XmlException::class)
        override fun processMessage(source: Reader, attachments: Map<String, DataSource>): Source {
            return processMessage(XmlStreaming.newReader(source), attachments)
        }

        private fun processMessage(source: XmlReader, pAttachments: Map<String, DataSource>): Source {
            val envelope = Envelope.deserialize(source)
            val reader = envelope.body!!.bodyContent!!.getXmlReader()
            loop@ while (reader.hasNext()) {
                when (reader.next()) {
                    EventType.START_ELEMENT -> break@loop
                    else                    -> throw XmlException("Unexpected content in soap message")
                }
            }
            if (reader.eventType !== EventType.START_ELEMENT) {
                throw HttpResponseException(HttpServletResponse.SC_BAD_REQUEST, "Operation element not found")
            }

            val operation = reader.name

            val method = getMethodFor(operation, mTarget)

            if (method != null) {
                method.unmarshalParams(envelope, pAttachments)
                method.exec()
                return method.resultSource
            }
            throw HttpResponseException(HttpServletResponse.SC_BAD_REQUEST, "Operation $operation not found")
        }


        private fun getMethodFor(operation: QName, target: Any): SoapMethodWrapper? {
            //    final Method[] candidates = target.getClass().getDeclaredMethods();
            val candidates = getCandidatesFor(target.javaClass, operation)
            for (candidate in candidates) {
                val annotation = candidate.getAnnotation(WebMethod::class.java)

                if (annotation != null && (annotation.operationName.isEmpty() && candidate.name == operation.localPart || annotation.operationName == operation.localPart)) {
                    return SoapMethodWrapper(target, candidate)
                }

            }
            return null
        }


        private fun getCandidatesFor(pClass: Class<*>, pOperation: QName): Collection<Method> {
            var v: PrefixMap<Method>? = cache[pClass]
            if (v == null) {
                v = createCacheElem(pClass)
                cache[pClass] = v
            }
            val w = v[pOperation.localPart]

            return ValueCollection(w)
        }

        private fun createCacheElem(pClass: Class<*>): PrefixMap<Method> {
            val result = PrefixMap<Method>()
            val methods = pClass.methods

            for (m in methods) {
                val annotation = m.getAnnotation(WebMethod::class.java)
                if (annotation != null) {
                    var operation = annotation.operationName
                    if (operation.length == 0) {
                        operation = m.name
                    }
                    result.put(operation, m)
                }
            }
            return result
        }

    }

    private class SoapMessageHandlerAccessor(target: Any) : SoapMessageHandler() {

        private val other: Any
        private val processRequest: Method
        private val processMessage: Method

        init {
            try {
                val otherClass = Class.forName(ClassLoadedSoapMessageHandler::class.java.name, true,
                                               target.javaClass.classLoader)

                val constructor = otherClass.getConstructor(Any::class.java)
                other = constructor.newInstance(target)

                var processRequest: Method? = null
                var processMessage: Method? = null
                for (m in otherClass.declaredMethods) {
                    if (!Modifier.isPublic(m.modifiers)) {
                        continue
                    }
                    var methodIdx: Int
                    if (m.name == "processRequest") {
                        methodIdx = 1
                    } else if (m.name == "processMessage") {
                        methodIdx = 2
                    } else {
                        continue
                    }
                    m.isAccessible = true // force accessible
                    if (methodIdx == 1) {
                        processRequest = m
                    } else {
                        processMessage = m
                    }
                }
                if (processRequest == null || processMessage == null) {
                    throw IllegalStateException("Required methods not found")
                }

                this.processRequest = processRequest
                this.processMessage = processMessage
            } catch (e: ClassNotFoundException) {
                throw RuntimeException(e)
            } catch (e: NoSuchMethodException) {
                throw RuntimeException(e)
            } catch (e: InstantiationException) {
                throw RuntimeException(e)
            } catch (e: IllegalAccessException) {
                throw RuntimeException(e)
            } catch (e: InvocationTargetException) {
                throw RuntimeException(e)
            }

        }

        @Throws(IOException::class)
        override fun processRequest(request: HttpMessage, response: HttpServletResponse): Boolean {
            try {
                try {
                    return processRequest.invoke(other, request, response) as Boolean
                } catch (i: InvocationTargetException) {
                    throw i.cause ?: i
                }

            } catch (e: RuntimeException) {
                throw e
            } catch (e: IOException) {
                throw e
            } catch (throwable: Throwable) {
                throw RuntimeException(throwable)
            }

        }

        @Throws(XmlException::class)
        override fun processMessage(source: Reader, attachments: Map<String, DataSource>): Source {
            try {
                try {
                    return processMessage.invoke(other, source, attachments) as Source
                } catch (i: InvocationTargetException) {
                    throw i.cause ?: i
                }

            } catch (e: RuntimeException) {
                throw e
            } catch (e: XmlException) {
                throw e
            } catch (throwable: Throwable) {
                throw RuntimeException(throwable)
            }

        }
    }

    @Throws(IOException::class, XmlException::class)
    abstract fun processRequest(request: HttpMessage, response: HttpServletResponse): Boolean

    @Deprecated("")
    @Throws(IOException::class, XmlException::class)
    fun processMessage(dataSource: DataSource, attachments: Map<String, DataSource>): Source {
        return processMessage(InputStreamReader(dataSource.inputStream, "UTF8"), attachments)
    }

    @Throws(XmlException::class)
    abstract fun processMessage(source: Reader, attachments: Map<String, DataSource>): Source

    companion object {

        @Volatile
        private var instances: MutableMap<Any, SoapMessageHandlerAccessor>? = null


        fun newInstance(target: Any): SoapMessageHandler {
            val instances = this.instances ?: (ConcurrentHashMap<Any, SoapMessageHandlerAccessor>().also { this.instances = it })

            return if (!instances.containsKey(target)) {
                synchronized(instances) {
                    return instances.getOrPut(target) { SoapMessageHandlerAccessor(target) }
                }
            } else {
                instances[target]!!
            }
        }

        fun canHandle(pClass: Class<*>): Boolean {
            for (m in pClass.methods) {
                val an = m.getAnnotation(WebMethod::class.java)
                if (an != null) {
                    return true
                }
            }
            return false
        }

        fun isSoapMessage(pRequest: HttpServletRequest): Boolean {
            return "application/soap+xml" == pRequest.contentType
        }
    }


}
