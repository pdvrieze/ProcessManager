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

package nl.adaptivity.process.processModel;


public class IllegalProcessModelException extends RuntimeException {

  private static final long serialVersionUID = 4026941365873864428L;

  public IllegalProcessModelException() {
    super();
  }

  public IllegalProcessModelException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public IllegalProcessModelException(final String message) {
    super(message);
  }

  public IllegalProcessModelException(final Throwable cause) {
    super(cause);
  }

}
