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

package nl.adaptivity.process;


import nl.adaptivity.messaging.EndpointDescriptor;
import nl.adaptivity.process.util.Constants;

import javax.xml.namespace.QName;

import java.net.URI;


public class ProcessConsts {

  public static final class Engine {

    public static final String NAMESPACE = "http://adaptivity.nl/ProcessEngine/";

    public static final String NSPREFIX = "pe";
  }

  public static final class Soap {

    public static final String SOAP_ENCODING_NS = "http://www.w3.org/2003/05/soap-encoding";

    public static final String SOAP_ENCODING_PREFIX = "soapenc";
  }

  public static final class Endpoints {

    public static final EndpointDescriptor USER_TASK_SERVICE_DESCRIPTOR = new UserTaskServiceDescriptor();

    public static class UserTaskServiceDescriptor implements EndpointDescriptor {
      @Override
      public QName getServiceName() {
        return SERVICENAME;
      }

      public static final String ENDPOINT = "internal";

      public static final QName SERVICENAME = new QName(Constants.USER_MESSAGE_HANDLER_NS, "userMessageHandler");

      @Override
      public String getEndpointName() {
        return ENDPOINT;
      }

      @Override
      public URI getEndpointLocation() {
        return null;
      }

      @Override
      public boolean isSameService(final EndpointDescriptor other) {
        return getServiceName().equals(other.getServiceName()) && getEndpointName().equals(other.getEndpointName());
      }
    }

  }
}
