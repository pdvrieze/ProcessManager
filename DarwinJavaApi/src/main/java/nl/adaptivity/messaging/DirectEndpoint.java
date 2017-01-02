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

package nl.adaptivity.messaging;

import java.util.concurrent.Future;


/**
 * An interface marking an endpoint that can handle direct
 * (non-reflection-based) delivery of messages to it. This is an optimization
 * for the MessagingRegistry.
 *
 * @author Paul de Vrieze
 */
public interface DirectEndpoint extends Endpoint {

  /**
   * Direct delivery of the given message.
   *
   * @param message The message to deliver
   * @param completionListener The completion Listener to notify of completion.
   */
  <T> Future<T> deliverMessage(ISendableMessage message, CompletionListener completionListener, Class<T> returnType);

}
