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

package nl.adaptivity.process.userMessageHandler.server;

import nl.adaptivity.messaging.Descriptor;
import nl.adaptivity.messaging.EndpointDescriptor;
import nl.adaptivity.messaging.EndpointDescriptorImpl;
import nl.adaptivity.process.messaging.ActivityResponse;
import nl.adaptivity.process.messaging.GenericEndpoint;
import nl.adaptivity.process.util.Constants;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.namespace.QName;

import java.net.URI;


/*@XmlSeeAlso(XmlTask.class)*/
@XmlAccessorType(XmlAccessType.NONE)
@Descriptor(InternalEndpoint.Descriptor.class)
public interface InternalEndpoint extends GenericEndpoint {

  @WebMethod
  public ActivityResponse<Boolean> postTask(@WebParam(name = "repliesParam", mode = Mode.IN) final EndpointDescriptorImpl endPoint, @WebParam(name = "taskParam", mode = Mode.IN) /*@SoapSeeAlso(XmlTask.class)*/ final UserTask<?> task);

  public static class Descriptor implements EndpointDescriptor {
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
  }
}
