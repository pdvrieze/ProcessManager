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

package nl.adaptivity.messaging;

import nl.adaptivity.io.Writable;

import javax.activation.DataSource;

import java.io.Reader;
import java.util.Collection;
import java.util.Map;


/**
 * Interface signalling that a message can be sent by an {@link IMessenger}
 *
 * @author Paul de Vrieze
 */
public interface ISendableMessage {


  /**
   * Class representing a simple HTTP header.
   *
   * @author Paul de Vrieze
   */
  interface IHeader {

    /**
     * Get the name of the header.
     *
     * @return The header name
     */
    String getName();

    /**
     * Get the value of the header.
     *
     * @return The header value
     */
    String getValue();

  }

  Reader getBodyReader();

  /**
   * What should be the destination of the message.
   *
   * @return the url to open. Can be partial!
   */
  EndpointDescriptor getDestination();

  /**
   * What method should be used for the message.
   *
   * @return <code>null</code> if default, otherwise the method (in uppercase)
   */
  String getMethod();

  /** Get the headers needing to be set on the request. */
  Collection<? extends IHeader> getHeaders();

  /**
   * Get the source that represents the body of the message.
   *
   * @return The body of the message. Returns <code>null</code> if there is no
   *         body. This is a DataSource as that will be used for the content
   *         type unless overridden by a header returned by
   *         {@link #getHeaders()}
   */
  Writable getBodySource();

  Map<String, DataSource> getAttachments();

  String getContentType();
}
