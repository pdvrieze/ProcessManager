package net.devrieze.util;

import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;

public class CompoundException extends RuntimeException {

  private static final long serialVersionUID = -395370803660462253L;

  @NotNull
  private final List<? extends Throwable> mCauses;

  private int replayPos = 0;

  public CompoundException(@NotNull final List<? extends Exception> causes) {
    super("Multiple exceptions occurred");
    mCauses = causes;
  }

  public <T extends Throwable> void replayNext(@NotNull final Class<T> clazz) throws T {
    final int pos = replayPos;
    replayPos++;
    if (pos < mCauses.size()) {
      final Throwable e = mCauses.get(pos);
      if (clazz.isInstance(e)) {
        throw clazz.cast(e);
      }
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getMessage() {
    return super.getMessage();
  }

  @Override
  public void printStackTrace() {
    printStackTrace(System.out);
  }

  @Override
  public void printStackTrace(final PrintStream s) {
    synchronized (Objects.requireNonNull(s)) {
      s.println(this);
      for (int i = 0; i < mCauses.size(); ++i) {
        if (i >= 1) {
          s.println();
        }
        final Throwable cause = mCauses.get(i);
        s.print("Cause ");
        s.print(i);
        s.print(": ");
        s.println(cause);
        for (final StackTraceElement elem : cause.getStackTrace()) {
          s.print(i);
          s.print(":\tat");
          s.print(elem);
        }
        final Throwable elemCause = cause.getCause();
        if (elemCause != null) {
          printStackTraceAsCause(s, i, elemCause);
        }
      }
    }
  }

  private static void printStackTraceAsCause(@NotNull final PrintStream s, final int i, @NotNull final Throwable cause) {
    s.print("Cause ");
    s.print(i);
    s.print(": ");
    s.println(cause);
    for (final StackTraceElement elem : cause.getStackTrace()) {
      s.print(i);
      s.print(":\tat");
      s.print(elem);
    }
    final Throwable elemCause = cause.getCause();
    if (elemCause != null) {
      printStackTraceAsCause(s, i, elemCause);
    }
  }

  @Override
  public void printStackTrace(final PrintWriter s) {
    synchronized (Objects.requireNonNull(s)) {
      s.println(this);
      for (int i = 0; i < mCauses.size(); ++i) {
        if (i >= 1) {
          s.println();
        }
        final Throwable cause = mCauses.get(i);
        s.print("Cause ");
        s.print(i);
        s.print(": ");
        s.println(cause);
        for (final StackTraceElement elem : cause.getStackTrace()) {
          s.print(i);
          s.print(":\tat");
          s.print(elem);
        }
        final Throwable elemCause = cause.getCause();
        if (elemCause != null) {
          printStackTraceAsCause(s, i, elemCause);
        }
      }
    }
  }

  private static void printStackTraceAsCause(final PrintWriter s, final int i, final Throwable cause) {
    s.print("Cause ");
    s.print(i);
    s.print(": ");
    s.println(cause);
    for (final StackTraceElement elem : cause.getStackTrace()) {
      s.print(i);
      s.print(":\tat");
      s.print(elem);
    }
    final Throwable elemCause = cause.getCause();
    if (elemCause != null) {
      printStackTraceAsCause(s, i, elemCause);
    }
  }


}
