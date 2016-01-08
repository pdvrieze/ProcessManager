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

import net.devrieze.util.HandleMap.HandleAware;
import nl.adaptivity.messaging.EndpointDescriptorImpl;
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState;

import java.security.Principal;
import java.util.List;


public interface UserTask<T extends UserTask<T>> extends HandleAware<T> {


  interface TaskItem {

    List<String> getOptions();

    String getValue();

    String getType();

    String getName();

    String getParams();

    String getLabel();

  }

  NodeInstanceState getState();

  void setState(NodeInstanceState newState, Principal user);

  void setEndpoint(EndpointDescriptorImpl endPoint);

  Principal getOwner();

  List<? extends TaskItem> getItems();

  void setItems(List<? extends TaskItem> items);

  long getRemoteHandle();

  long getInstanceHandle();

  String getSummary();

}
