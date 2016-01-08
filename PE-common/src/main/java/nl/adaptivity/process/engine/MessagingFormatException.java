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

package nl.adaptivity.process.engine;

import javax.servlet.http.HttpServletResponse;

import nl.adaptivity.messaging.HttpResponseException;


/**
 * Exception signalling an error in the kind of message body.
 *
 * @author Paul de Vrieze
 */
public class MessagingFormatException extends HttpResponseException {

  private static final long serialVersionUID = 7931145565871734159L;

  public MessagingFormatException(final String message) {
    super(HttpServletResponse.SC_BAD_REQUEST, message);
  }

  public MessagingFormatException(final Throwable cause) {
    super(HttpServletResponse.SC_BAD_REQUEST, cause);
  }

  public MessagingFormatException(final String message, final Throwable cause) {
    super(HttpServletResponse.SC_BAD_REQUEST, message, cause);
  }

}
