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

import java.io.FileNotFoundException;
import java.util.concurrent.Future;


/**
 * Interface for classes that can receive completion messages from the
 * {@link IMessenger}. This happens in a separate thread.
 *
 * @author Paul de Vrieze
 */
public interface CompletionListener<T> {

  /**
   * Signify the completion of the task corresponding to the given future. Note
   * that implementations sending completion messages should ensure that the
   * future is complete when this method is called. There should not be a wait
   * when invoking {@link Future#get()} on the future.
   *
   * @param future The future that is complete.
   */
  void onMessageCompletion(@SuppressWarnings("UnusedParameters") Future<? extends T> future);

}