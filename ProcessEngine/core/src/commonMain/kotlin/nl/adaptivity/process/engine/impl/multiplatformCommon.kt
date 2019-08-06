/*
 * Copyright (c) 2019.
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

package nl.adaptivity.process.engine.impl

import nl.adaptivity.util.multiplatform.Class
import nl.adaptivity.xmlutil.XmlStreaming
import nl.adaptivity.xmlutil.XmlWriter

expect enum class TimeUnit
expect interface Future<T> {
    fun isCancelled():Boolean
    fun cancel(mayInterruptIfRunning: Boolean): Boolean
    fun get(): T
    fun get(timeout: Long, unit: TimeUnit): T
    fun isDone(): Boolean
}
expect class CancellationException: IllegalStateException {
    constructor()
}

expect class Logger {
    fun log(level: Level, message: String, cause: Throwable)
}

expect class Level
expect object LogLevel {
    val WARNING: Level
}

expect class JAXBElement<T>
expect interface Source
expect interface Result

expect fun JAXBmarshal(jaxbObject: Any, xml: Result)
expect inline fun <T:Any> T.getClass(): Class<T>

expect inline fun generateXmlString(
    repairNamespaces: Boolean,
    generator: (XmlWriter) -> Unit
                                   ): CharArray
