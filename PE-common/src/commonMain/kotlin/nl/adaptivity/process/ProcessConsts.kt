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

package nl.adaptivity.process


import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.process.util.Constants
import kotlin.jvm.JvmField
import nl.adaptivity.util.multiplatform.URI
import nl.adaptivity.xmlutil.QName


class ProcessConsts {

    object Engine {

        const val NAMESPACE = "http://adaptivity.nl/ProcessEngine/"

        const val NSPREFIX = "pe"

        @Deprecated("", ReplaceWith("this.NAMESPACE"))
        fun getNAMESPACE() = NAMESPACE

        @Deprecated("", ReplaceWith("this.NSPREFIX"))
        fun getNSPREFIX() = NSPREFIX
    }

    object Soap {

        const val SOAP_ENCODING_NS = "http://www.w3.org/2003/05/soap-encoding"

        const val SOAP_ENCODING_PREFIX = "soapenc"
    }

    object Endpoints {

        open class UserTaskServiceDescriptor : EndpointDescriptor {
            override val serviceName: QName get() = SERVICENAME

            override val endpointName: String get() = ENDPOINT

            override val endpointLocation: URI? get() = null

            override fun isSameService(other: EndpointDescriptor?): Boolean {
                return other != null &&
                    SERVICENAME == other.serviceName &&
                    ENDPOINT.equals(other.endpointName)
            }

            companion object {
                @JvmField
                val SERVICENAME = QName(Constants.USER_MESSAGE_HANDLER_NS, "userMessageHandler")

                const val ENDPOINT = "internal"
            }

        }

        val USER_TASK_SERVICE_DESCRIPTOR: EndpointDescriptor = UserTaskServiceDescriptor()

    }
}
