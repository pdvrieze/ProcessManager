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

/**
 * A messaging exception that responds to an http status code.
 *
 * @author Paul de Vrieze
 */
public class HttpResponseException extends MessagingException {

  private static final long serialVersionUID = -1958369502963081324L;

  private final int mCode;

  public HttpResponseException(final int code, final String message) {
    super(message);
    mCode = code;
  }

  public HttpResponseException(final int code, final Throwable cause) {
    super(cause);
    mCode = code;
  }

  public HttpResponseException(final int code, final String message, final Throwable cause) {
    super(message, cause);
    mCode = code;
  }

  public int getResponseCode() {
    return mCode;
  }

}
