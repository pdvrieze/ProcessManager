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

package net.devrieze.util

import org.jetbrains.annotations.Contract

import java.io.OutputStreamWriter
import java.io.PrintStream
import java.io.PrintWriter
import java.io.Writer
import java.util.ArrayList
import java.util.Collections


/**
 * @constructor Create a new MultiException
 * @param error The initial error to record in the exception
 */
class MultiException(error: Throwable) : RuntimeException() {

    private val _exceptions: MutableList<Throwable>

    val exceptions: List<Throwable>
        get() = Collections.unmodifiableList(_exceptions)

    override fun getLocalizedMessage(): String {
        val result = StringBuilder()
        result.append("MultiException:\n")
        for (e in _exceptions) {
            StringUtil.indentTo(result, 4, e.localizedMessage)
        }
        result.trimToSize()
        return result.toString()
    }

    override val message: String
        get() {
            val result = StringBuilder()
            result.append("MultiException:\n")
            for (e in _exceptions) {
                e.message?.let { StringUtil.indentTo(result, 4, it) }
            }
            result.trimToSize()
            return result.toString()
        }

    override fun printStackTrace() {
        printStackTrace(System.err)
    }

    override fun printStackTrace(pS: PrintStream) {
        printStackTrace(OutputStreamWriter(pS))
    }

    fun printStackTrace(writer: Writer) {
        if (writer is PrintWriter) {
            printStackTrace(writer)
        } else {
            printStackTrace(PrintWriter(writer))
        }
    }

    override fun printStackTrace(pS: PrintWriter) {
        super.printStackTrace(pS)
        pS.println("Contained exceptions:")
        for (e in _exceptions) {
            e.printStackTrace(PrintWriter(StringUtil.indent(4, pS)))
        }
    }

    init {
        _exceptions = ArrayList()
        _exceptions.add(error)
    }

    private fun add(pElement: Throwable) {
        _exceptions.add(pElement)
    }

    companion object {

        private val serialVersionUID = -4815858709758051717L

        @JvmStatic
        fun add(pTarget: MultiException?, pElement: Throwable): MultiException {
            val error: MultiException
            if (pTarget == null) {
                error = MultiException(pElement)
            } else {
                error = pTarget
                error.add(pElement)
            }
            return error
        }

        /**
         * When there is at least one wrapped exception, throw the first one. This method handles nulls.
         * @param target The multiException to evaluate
         */
        @JvmStatic
        @Contract(pure = true, value = "!null -> fail")
        fun throwIfError(target: MultiException?) {
            if (target != null) {
                if (target._exceptions.size == 1) {
                    val e = target._exceptions[0]
                    throw e as? RuntimeException ?: RuntimeException(e)
                } else {
                    throw target
                }
            }
        }
    }


}
