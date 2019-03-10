/*
 * Copyright (c) 2018.
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

package net.devrieze.util.security;


public class PermissionDeniedException extends RuntimeException {

  private static final long serialVersionUID = 1782673055725449807L;

  public PermissionDeniedException(final String pMessage, final Throwable pCause, final boolean pEnableSuppression, final boolean pWritableStackTrace) {
    super(pMessage, pCause, pEnableSuppression, pWritableStackTrace);
  }

  public PermissionDeniedException(final String pMessage, final Throwable pCause) {
    super(pMessage, pCause);
  }

  public PermissionDeniedException(final String pMessage) {
    super(pMessage);
  }

  public PermissionDeniedException(final Throwable pCause) {
    super(pCause);
  }

}
