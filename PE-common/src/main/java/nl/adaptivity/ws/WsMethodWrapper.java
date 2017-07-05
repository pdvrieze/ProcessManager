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

package nl.adaptivity.ws;

import nl.adaptivity.messaging.MessagingException;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * Created by pdvrieze on 28/11/15.
 */
public abstract class WsMethodWrapper {

  protected final Object mOwner;
  protected final Method method;
  protected Object[]     params;
  protected Object       result;

  public WsMethodWrapper(final Object owner, final Method method) {
    mOwner = owner;
    this.method = method;
  }

  public void exec() {
    if (params == null) {
      throw new IllegalArgumentException("Argument unmarshalling has not taken place yet");
    }
    try {
      result = method.invoke(mOwner, params);
    } catch (@NotNull final IllegalArgumentException | IllegalAccessException e) {
      throw new MessagingException(e);
    } catch (@NotNull final InvocationTargetException e) {
      final Throwable cause = e.getCause();
      throw new MessagingException(cause != null ? cause : e);
    }
  }
}
