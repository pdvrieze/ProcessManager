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

package net.devrieze.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MultiException extends RuntimeException {

  private static final long serialVersionUID = -4815858709758051717L;

  private final List<Throwable> mExceptions;

  @Override
  public String getLocalizedMessage() {
    final StringBuilder result = new StringBuilder();
    result.append("MultiException:\n");
    for (final Throwable e : mExceptions) {
      StringUtil.indentTo(result, 4, e.getLocalizedMessage());
    }
    result.trimToSize();
    return result.toString();
  }

  @Override
  public String getMessage() {
    final StringBuilder result = new StringBuilder();
    result.append("MultiException:\n");
    for (final Throwable e : mExceptions) {
      StringUtil.indentTo(result, 4, e.getMessage());
    }
    result.trimToSize();
    return result.toString();
  }

  @Override
  public void printStackTrace() {
    printStackTrace(System.err);
  }

  @Override
  public void printStackTrace(final PrintStream pS) {
    printStackTrace(new OutputStreamWriter(pS));
  }

  @SuppressWarnings("resource")
  public void printStackTrace(final Writer pS) {
    if (pS instanceof PrintWriter) {
      printStackTrace((PrintWriter) pS);
    } else {
      printStackTrace(new PrintWriter(pS));
    }
  }

  @Override
  public void printStackTrace(final PrintWriter pS) {
    super.printStackTrace(pS);
    System.err.println("Contained exceptions:");
    for (final Throwable e : mExceptions) {
      e.printStackTrace(new PrintWriter(StringUtil.indent(4, pS)));
    }
  }

  public MultiException(final Throwable pError) {
    mExceptions = new ArrayList<>();
    mExceptions.add(pError);
  }

  @NotNull
  public static MultiException add(@Nullable final MultiException pTarget, @NotNull final Throwable pElement) {
    final MultiException error;
    if (pTarget ==null) {
      error = new MultiException(pElement);
    } else {
      error = pTarget;
      error.add(pElement);
    }
    return error;
  }

  public static void throwIfError(@Nullable MultiException pTarget) {
    if (pTarget!=null) {
      if (pTarget.mExceptions.size()==1) {
        final Throwable e = pTarget.mExceptions.get(0);
        throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
      } else {
        throw pTarget;
      }
    }
  }

  private void add(@NotNull final Throwable pElement) {
    mExceptions.add(pElement);
  }

  @NotNull
  public List<Throwable> getExceptions() {
    return Collections.unmodifiableList(mExceptions);
  }


}
