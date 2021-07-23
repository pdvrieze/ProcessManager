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

/*
 * DebugTool.java
 *
 * Created on 10 March 2001, 20:49
 */

package net.devrieze.util

import java.io.PrintStream
import java.util.*


/**
 * A tool for debugging code.
 *
 * @author Paul de Vrieze
 * @version 1.1 $Revision$
 */
object DebugTool {

    private val DEFAULT_NOT_NULL_PARAM_MESSAGE = "The given parameter is null, while not allowed"

    private var debugLevel = 0

    private val DEFAULT_ENABLE_ASSERTIONS = false

    private var debugStream = System.err

    /** The default debugging level for messages.  */
    const val DEF_DEBUG_LEVEL = 1

    /** The debugging level for abstract listeners.  */
    const val ABSTRACT_LISTENER_LEVEL = 20

    /** The debugging level for events.  */
    const val EVENT_LEVEL = 10

    /**
     * Set the debugging level for the program.
     *
     * @param debugLevel The level of debugging that is desired.
     */
    @JvmStatic
    fun setDebugLevel(debugLevel: Int) {
        DebugTool.debugLevel = debugLevel
        dPrintLn(1, "This Java program is debugged by net.devrieze.util.debugTool version 1.0")
        dPrintLn(1, " copyright 2001,2004 by Paul de Vrieze")
        if (DEFAULT_ENABLE_ASSERTIONS) {
            dPrintLn(1, "")
            dPrintLn(1, "Assertions enabled for newly loaded classes")
            ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true)
        }
    }

    /**
     * Get the level of debugging that is used.
     *
     * @return The debug level
     */
    @JvmStatic
    fun getDebugLevel(): Int {
        return debugLevel
    }

    /**
     * Set the stream to which the debugging is send.
     *
     * @param debugStream The stream to send the debugging to
     */
    @JvmStatic
    fun setDebugStream(debugStream: PrintStream) {
        ensureParamNotNull(debugStream)
        DebugTool.debugStream = debugStream
    }

    /**
     * Get the stream used for debugging.
     *
     * @return PrintStream
     */
    @JvmStatic
    fun getDebugStream(): PrintStream {
        return debugStream
    }

    /**
     * Print with the specified debug level. If the level is lower than the
     * current level the message is printed, else it is ignored.
     *
     * @param level The debug level of the message
     * @param message The message to be printed
     */
    @JvmStatic
    fun dPrintLn(level: Int, message: CharSequence) {
        if (debugLevel >= level) {
            debugStream.println(message)
        }
    }

    /**
     * Print the message with the default debug level.
     *
     * @param message The message to be printed
     * @see .dPrintLn
     */
    @JvmStatic
    fun dPrintLn(message: CharSequence) {
        dPrintLn(DEF_DEBUG_LEVEL, message)
    }

    /**
     * Print the specified character. Do not print a line break.
     *
     * @param level The debug level of the char
     * @param pChar the character to be printed
     */
    @JvmStatic
    fun dPrint(level: Int, pChar: Char) {
        if (debugLevel >= level) {
            debugStream.print(pChar)
        }

        try {
            debugStream.flush()
        } catch (e: Exception) {
            System.err.println(e.message)
        }

    }

    /**
     * Print the specified character. Do not print a line break.
     *
     * @param pChar the character to be printed
     */
    @JvmStatic
    fun dPrint(pChar: Char) {
        dPrint(DEF_DEBUG_LEVEL, pChar)
    }

    /**
     * Print a message with the default debug level. Do not print a line break.
     *
     * @param message The message to print.
     */
    @JvmStatic
    fun dPrint(message: CharSequence) {
        dPrint(DEF_DEBUG_LEVEL, message)
    }

    /**
     * Print a message with the given level. Do not print a line break.
     *
     * @param level The debug level.
     * @param message The message to print.
     */
    @JvmStatic
    fun dPrint(level: Int, message: CharSequence) {
        if (debugLevel >= level) {
            debugStream.print(message)
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
    @JvmStatic
    fun parseArgs(pArgs: Array<String>): Array<String> {
        DebugTool.ensureParamNotNull(pArgs)
        val list = ArrayList<String>(pArgs.size)
        var i = 0
        while (i < pArgs.size) {
            if (pArgs[i].lowercase().startsWith("--debug")) {

                try {
                    if (i + 1 < pArgs.size) {
                        val j = Integer.parseInt(pArgs[i + 1])
                        DebugTool.setDebugLevel(j)
                        i++
                    } else {
                        throw Exception("error")
                    }
                } catch (e: Exception) {
                    println("The debug option needs to be followed by a number specifying the debug level")
                    System.exit(1)
                }

            } else {
                list.add(pArgs[i])
            }
            i++
        }
        return list.toTypedArray()
    }

    /**
     * Print with the default debug level. If the level is lower than the current
     * level the message is printed, else it is ignored.
     *
     * @param exception The exception to be printed
     */
    @JvmStatic
    fun handle(exception: Throwable) {
        handle(DEF_DEBUG_LEVEL, exception)
    }

    /**
     * Print with the specified debug level. If the level is lower than the
     * current level the message's stacktrace is printed, else it is ignored.
     *
     * @param level The debug level of the message.
     * @param pException The exception to be printed.
     */
    @JvmStatic
    fun handle(level: Int, pException: Throwable) {
        if (debugLevel >= level) {
            pException.printStackTrace(debugStream)
        }
    }

    /**
     * A utility function that helps checking that a parameter is not
     * `null`.
     *
     * @param pParam The parameter to check
     */
    @JvmStatic
    fun ensureParamNotNull(pParam: Any) {
        ensureParamNotNull(pParam, DEFAULT_NOT_NULL_PARAM_MESSAGE)
    }

    /**
     * A utility function that helps checking that a parameter is not null.
     *
     * @param param The parameter to check
     * @param message The exception description
     */
    @JvmStatic
    fun ensureParamNotNull(param: Any?, message: String) {
        if (param == null) {
            throw IllegalArgumentException(message)
        }
    }

    /**
     * A utility function that helps checking parameter conditions. This should
     * throw IllegalArgumentExceptions, so assert can not be used.
     *
     * @param condition The boolean result of the condition
     * @param message The message for the exception when it doesn't
     */
    @JvmStatic
    fun ensureParamValid(condition: Boolean, message: String) {
        if (!condition) {
            throw IllegalArgumentException(message)
        }
    }

}// Do nothing.
