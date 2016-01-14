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
import nl.adaptivity.messaging.EndpointDescriptorImpl;
import nl.adaptivity.process.ProcessConsts.Endpoints.UserTaskServiceDescriptor;
import nl.adaptivity.process.messaging.ActivityResponse;
import nl.adaptivity.process.messaging.GenericEndpoint;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;


/*@XmlSeeAlso(XmlTask.class)*/
@XmlAccessorType(XmlAccessType.NONE)
@Descriptor(UserTaskServiceDescriptor.class)
public interface InternalEndpoint extends GenericEndpoint {

  @WebMethod
  public ActivityResponse<Boolean> postTask(@WebParam(name = "repliesParam", mode = Mode.IN) final EndpointDescriptorImpl endPoint, @WebParam(name = "taskParam", mode = Mode.IN) /*@SoapSeeAlso(XmlTask.class)*/ final UserTask<?> task);

}
