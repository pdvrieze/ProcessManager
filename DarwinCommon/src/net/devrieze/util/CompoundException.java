package net.devrieze.util;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;


public class CompoundException extends RuntimeException {

  private static final long serialVersionUID = -395370803660462253L;

  private final List<? extends Throwable> aCauses;

  private int replayPos = 0;

  public CompoundException(final List<? extends Exception> pCauses) {
    super("Multiple exceptions occurred");
    aCauses = pCauses;
  }

  public <T extends Throwable> void replayNext(final Class<T> pClass) throws T {
    final int pos = replayPos;
    replayPos++;
    if (pos < aCauses.size()) {
      final Throwable e = aCauses.get(pos);
      if ((pClass != null) && pClass.isInstance(e)) {
        throw pClass.cast(e);
      }
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getMessage() {
    // TODO Auto-generated method stub
    return super.getMessage();
  }

  @Override
  public void printStackTrace() {
    printStackTrace(System.out);
  }

  @Override
  public void printStackTrace(final PrintStream s) {
    synchronized (s) {
      s.println(this);
      for (int i = 0; i < aCauses.size(); ++i) {
        if (i >= 1) {
          s.println();
        }
        final Throwable cause = aCauses.get(i);
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

  private static void printStackTraceAsCause(final PrintStream s, final int i, final Throwable pCause) {
    final Throwable cause = pCause;
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
    synchronized (s) {
      s.println(this);
      for (int i = 0; i < aCauses.size(); ++i) {
        if (i >= 1) {
          s.println();
        }
        final Throwable cause = aCauses.get(i);
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

  private static void printStackTraceAsCause(final PrintWriter s, final int i, final Throwable pCause) {
    final Throwable cause = pCause;
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
