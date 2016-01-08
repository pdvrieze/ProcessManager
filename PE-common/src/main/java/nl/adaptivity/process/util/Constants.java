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

package nl.adaptivity.process.util;

import java.net.URI;

import javax.xml.namespace.QName;


public final class Constants {

  // TODO streamline this

  public static final String PROCESS_ENGINE_NS = "http://adaptivity.nl/ProcessEngine/";
  public static final java.lang.String PROCESS_ENGINE_NS_PREFIX = "pe";

  public static final QName PESERVICE = new QName(PROCESS_ENGINE_NS, "ProcessEngine");

  public static final URI WSDL_MEP_ROBUST_IN_ONLY = URI.create("http://www.w3.org/2004/08/wsdl/robust-in-only");

  public static final URI WSDL_MEP_IN_OUT = URI.create("http://www.w3.org/2004/08/wsdl/in-out");

  public static final URI WSDL_MEP_IN_ONLY = URI.create("http://www.w3.org/2004/08/wsdl/in-only");

  public static final URI WEBMETHOD_NS = URI.create("http://www.w3.org/2003/05/soap/features/web-method/Method");

  public static final String PROTOCOL_HEADERS = "javax.jbi.messaging.protocol.headers";

  public static final String USER_MESSAGE_HANDLER_NS = "http://adaptivity.nl/userMessageHandler";

  public static final String DARWIN_NS = "http://darwin.bournemouth.ac.uk/services";

  public static final String MODIFY_NS_STR = "http://adaptivity.nl/ProcessEngine/activity";

  public static final URI MODIFY_NS_URI = URI.create(MODIFY_NS_STR);

  public static final String MY_JBI_NS = "http://adaptivity.nl/jbi";

  private Constants() {

  }

}
