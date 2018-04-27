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

package nl.adaptivity.process.util

import nl.adaptivity.util.multiplatform.createUri
import nl.adaptivity.xml.QName


object Constants {

    val PROCESS_ENGINE_NS = "http://adaptivity.nl/ProcessEngine/"
    val PROCESS_ENGINE_NS_PREFIX: String = "pe"

    val PESERVICE = QName(PROCESS_ENGINE_NS, "ProcessEngine")

    val WSDL_MEP_ROBUST_IN_ONLY = createUri("http://www.w3.org/2004/08/wsdl/robust-in-only")

    val WSDL_MEP_IN_OUT = createUri("http://www.w3.org/2004/08/wsdl/in-out")

    val WSDL_MEP_IN_ONLY = createUri("http://www.w3.org/2004/08/wsdl/in-only")

    val WEBMETHOD_NS = createUri("http://www.w3.org/2003/05/soap/features/web-method/Method")

    val PROTOCOL_HEADERS = "javax.jbi.messaging.protocol.headers"

    val USER_MESSAGE_HANDLER_NS = "http://adaptivity.nl/userMessageHandler"

    val USER_MESSAGE_HANDLER_NS_PREFIX = "umh"

    val DARWIN_NS = "http://darwin.bournemouth.ac.uk/services"

    val MODIFY_NS_STR = "http://adaptivity.nl/ProcessEngine/activity"

    val MODIFY_NS_URI = createUri(MODIFY_NS_STR)
    val MODIFY_NS_PREFIX = "jbi"

    val MY_JBI_NS_STR = "http://adaptivity.nl/jbi"

}
