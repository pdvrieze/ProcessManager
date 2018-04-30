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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.userMessageHandler.server

import nl.adaptivity.messaging.Descriptor
import nl.adaptivity.messaging.EndpointDescriptorImpl
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.messaging.ActivityResponse
import nl.adaptivity.process.messaging.GenericEndpoint
import java.sql.SQLException
import javax.jws.WebMethod
import javax.jws.WebParam
import javax.jws.WebParam.Mode


/*@XmlSeeAlso(XmlTask.class)*/
@Descriptor(ProcessConsts.Endpoints.USER_TASK_SERVICE_DESCRIPTOR::class)
interface InternalEndpoint : GenericEndpoint {

  @WebMethod
  @Throws(SQLException::class)
  fun postTask(@WebParam(name = "repliesParam", mode = Mode.IN) endPoint: EndpointDescriptorImpl,
               @WebParam(name = "taskParam", mode = Mode.IN) task: UserTask<*>): ActivityResponse<Boolean>

}
