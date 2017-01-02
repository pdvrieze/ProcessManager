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

package net.devrieze.util;

import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


/**
 * An exception for collecting the occurrence of multiple exceptions. With modern java this can mainly be replaced by
 * suppressed exceptions, but in some cases still has independent use.
 */
public class CompoundException extends RuntimeException {

  private static final long serialVersionUID = -395370803660462253L;

  @NotNull
  private final List<? extends Throwable> mCauses;

  private int mReplayPos = 0;

  /**
   * Create a new exception.
   * @param causes The causes for the exception to be thrown.
   */
  public CompoundException(@NotNull final List<? extends Exception> causes) {
    super("Multiple exceptions occurred");
    mCauses = causes;
  }

  /**
   * Replay the next cause of the exception. If this cause is an instance of the clazz parameter, it will be thrown as
   * such an instance, otherwise it will be wrapped in a {@link RuntimeException}. The method does not throw if there is
   * no further cause.
   *
   * @param clazz The type to attempt to cast the cause to.
   * @param <T>   The type, so it is also declared as throwing this type.
   * @throws T Most likely, unless it throws a RuntimeException.
   */
  public <T extends Throwable> void replayNext(@NotNull final Class<T> clazz) throws T {
    final int pos = mReplayPos;
    mReplayPos++;
    if (pos < mCauses.size()) {
      final Throwable e = mCauses.get(pos);
      if (clazz.isInstance(e)) {
        throw clazz.cast(e);
      }
      throw new RuntimeException(e);
    }
  }

  @Override
  public void printStackTrace() {
    printStackTrace(System.out);
  }

  @Override
  public void printStackTrace(final PrintStream stream) {
    // This does not need to be closed as that would only close the parent stream, that is not this method's responsibility.
    printStackTrace(new PrintWriter(stream));
  }

  @SuppressWarnings("resource")
  @Override
  public void printStackTrace(final PrintWriter writer) {
    synchronized (Objects.requireNonNull(writer)) {
      writer.println(this);
      for (int i = 0; i < mCauses.size(); ++i) {
        if (i >= 1) {
          writer.println();
        }
        final Throwable cause = mCauses.get(i);
        printStackTraceAsCause(writer, i, cause);
      }
    }
  }

  private static void printStackTraceAsCause(final PrintWriter writer, final int i, final Throwable cause) {
    writer.print("Cause ");
    writer.print(i);
    writer.print(": ");
    writer.println(cause);
    for (final StackTraceElement elem : cause.getStackTrace()) {
      writer.print(i);
      writer.print(":\tat");
      writer.print(elem);
    }
    final Throwable elemCause = cause.getCause();
    if (elemCause != null) {
      printStackTraceAsCause(writer, i, elemCause);
    }
  }

  /**
   * Retrieve the causes for this exception being thrown.
   * @return An immutable list with the causes.
   */
  @NotNull
  public List<? extends Throwable> getCauses() {
    return Collections.unmodifiableList(mCauses);
  }
}
