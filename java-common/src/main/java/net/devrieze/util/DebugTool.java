/*
 * Copyright (c) 2017.
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

/*
 * DebugTool.java
 *
 * Created on 10 March 2001, 20:49
 */

package net.devrieze.util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;


/**
 * A tool for debugging code.
 * 
 * @author Paul de Vrieze
 * @version 1.1 $Revision$
 */
public final class DebugTool {

  private DebugTool() {
    // Do nothing.
  }

  private static final String _DEFAULT_NOT_NULL_PARAM_MESSAGE = "The given parameter is null, while not allowed";

  private static int sDebugLevel = 0;

  private static final boolean _DEFAULT_ENABLE_ASSERTIONS = false;

  private static PrintStream sDebugStream = System.err;

  /** The default debugging level for messages. */
  public static final int _DEF_DEBUG_LEVEL = 1;

  /** The debugging level for abstract listeners. */
  public static final int _ABSTRACT_LISTENER_LEVEL = 20;

  /** The debugging level for events. */
  public static final int _EVENT_LEVEL = 10;

  /**
   * Set the debugging level for the program.
   * 
   * @param debugLevel The level of debugging that is desired.
   */
  public static void setDebugLevel(final int debugLevel) {
    DebugTool.sDebugLevel = debugLevel;
    dPrintLn(1, "This Java program is debugged by net.devrieze.util.debugTool version 1.0");
    dPrintLn(1, " copyright 2001,2004 by Paul de Vrieze");
    if (_DEFAULT_ENABLE_ASSERTIONS) {
      dPrintLn(1, "");
      dPrintLn(1, "Assertions enabled for newly loaded classes");
      ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);
    }
  }

  /**
   * Get the level of debugging that is used.
   * 
   * @return The debug level
   */
  public static int getDebugLevel() {
    return sDebugLevel;
  }

  /**
   * Set the stream to which the debugging is send.
   * 
   * @param debugStream The stream to send the debugging to
   */
  public static void setDebugStream(final PrintStream debugStream) {
    ensureParamNotNull(debugStream);
    DebugTool.sDebugStream = debugStream;
  }

  /**
   * Get the stream used for debugging.
   * 
   * @return PrintStream
   */
  public static PrintStream getDebugStream() {
    return sDebugStream;
  }

  /**
   * Print with the specified debug level. If the level is lower than the
   * current level the message is printed, else it is ignored.
   * 
   * @param level The debug level of the message
   * @param message The message to be printed
   */
  public static void dPrintLn(final int level, final CharSequence message) {
    if (sDebugLevel >= level) {
      sDebugStream.println(message);
    }
  }

  /**
   * Print the message with the default debug level.
   * 
   * @param message The message to be printed
   * @see #dPrintLn(int, CharSequence)
   */
  public static void dPrintLn(final CharSequence message) {
    dPrintLn(_DEF_DEBUG_LEVEL, message);
  }

  /**
   * Print the specified character. Do not print a line break.
   * 
   * @param level The debug level of the char
   * @param pChar the character to be printed
   */
  public static void dPrint(final int level, final char pChar) {
    if (sDebugLevel >= level) {
      sDebugStream.print(pChar);
    }

    try {
      sDebugStream.flush();
    } catch (final Exception e) {
      System.err.println(e.getMessage());
    }
  }

  /**
   * Print the specified character. Do not print a line break.
   * 
   * @param pChar the character to be printed
   */
  public static void dPrint(final char pChar) {
    dPrint(_DEF_DEBUG_LEVEL, pChar);
  }

  /**
   * Print a message with the default debug level. Do not print a line break.
   * 
   * @param message The message to print.
   */
  public static void dPrint(final CharSequence message) {
    dPrint(_DEF_DEBUG_LEVEL, message);
  }

  /**
   * Print a message with the given level. Do not print a line break.
   * 
   * @param level The debug level.
   * @param message The message to print.
   */
  public static void dPrint(final int level, final CharSequence message) {
    if (sDebugLevel >= level) {
      sDebugStream.print(message);
    }
  }

  /**
   * This function will set the debug level based on the arguments. It will then
   * return a new list of arguments without the debug parameter for further
   * processing.
   * 
   * @param pArgs The arguments to the program
   * @return A new list of arguments without the debug option
   */
  public static String[] parseArgs(final String[] pArgs) {
    DebugTool.ensureParamNotNull(pArgs);
    final List<String> list = new ArrayList<>(pArgs.length);
    int i = 0;
    while (i < pArgs.length) {
      if (pArgs[i].toLowerCase().startsWith("--debug")) {

        try {
          if ((i + 1) < pArgs.length) {
            final int j = Integer.parseInt(pArgs[i + 1]);
            DebugTool.setDebugLevel(j);
            i++;
          } else {
            throw new Exception("error");
          }
        } catch (final Exception e) {
          System.out.println("The debug option needs to be followed by a number specifying the debug level");
          System.exit(1);
        }
      } else {
        list.add(pArgs[i]);
      }
      i++;
    }
    return list.toArray(new String[list.size()]);
  }

  /**
   * Print with the default debug level. If the level is lower than the current
   * level the message is printed, else it is ignored.
   * 
   * @param pException The exception to be printed
   */
  public static void handle(final Throwable pException) {
    handle(_DEF_DEBUG_LEVEL, pException);
  }

  /**
   * Print with the specified debug level. If the level is lower than the
   * current level the message's stacktrace is printed, else it is ignored.
   * 
   * @param level The debug level of the message.
   * @param pException The exception to be printed.
   */
  public static void handle(final int level, final Throwable pException) {
    if (sDebugLevel >= level) {
      pException.printStackTrace(sDebugStream);
    }
  }

  /**
   * A utility function that helps checking that a parameter is not
   * {@code null}.
   * 
   * @param pParam The parameter to check
   */
  public static void ensureParamNotNull(final Object pParam) {
    ensureParamNotNull(pParam, _DEFAULT_NOT_NULL_PARAM_MESSAGE);
  }

  /**
   * A utility function that helps checking that a parameter is not null.
   * 
   * @param param The parameter to check
   * @param message The exception description
   */
  public static void ensureParamNotNull(final Object param, final String message) {
    if (param == null) {
      throw new IllegalArgumentException(message);
    }
  }

  /**
   * A utility function that helps checking parameter conditions. This should
   * throw IllegalArgumentExceptions, so assert can not be used.
   * 
   * @param condition The boolean result of the condition
   * @param message The message for the exception when it doesn't
   */
  public static void ensureParamValid(final boolean condition, final String message) {
    if (!condition) {
      throw new IllegalArgumentException(message);
    }
  }

}
