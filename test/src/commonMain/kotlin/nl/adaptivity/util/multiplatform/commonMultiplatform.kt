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

package nl.adaptivity.util.multiplatform

import kotlin.reflect.KClass

expect class URI {
    fun getPath(): String
}

expect class UUID {
    override fun equals(other: Any?): Boolean
}

expect fun randomUUID(): UUID

expect fun String.toUUID(): UUID

inline val URI.path get() = getPath()

expect inline fun createUri(s: String): URI

@Suppress("NOTHING_TO_INLINE")
inline fun String.toUri(): URI = createUri(this)

fun Appendable.append(d: Double) = append(d.toString())
fun Appendable.append(i: Int) = append(i.toString())

expect val KClass<*>.nameCompat: String

expect fun assert(value: Boolean, lazyMessage: () -> String)

expect fun assert(value: Boolean)

expect interface AutoCloseable {
    fun close()
}

expect interface Closeable : AutoCloseable

expect fun interface Runnable {
    fun run()
}

@Suppress("unused")
expect inline fun <reified T : Any> isTypeOf(value: Any): Boolean

expect fun Throwable.addSuppressedCompat(suppressed: Throwable): Unit
expect fun Throwable.initCauseCompat(cause: Throwable): Throwable
