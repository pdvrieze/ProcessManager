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

import java.util.UUID
import kotlin.reflect.KClass

actual val KClass<*>.nameCompat get() = java.canonicalName

actual typealias UUID = java.util.UUID
actual fun randomUUID(): UUID = UUID.randomUUID()

actual typealias URI = java.net.URI

@Suppress("NOTHING_TO_INLINE")
actual inline fun createUri(s: String): URI = URI.create(s)

actual fun String.toUUID(): UUID = UUID.fromString(this)

actual inline fun <reified T:Any> isTypeOf(value: Any):Boolean = value::class.java == T::class.java

actual fun Throwable.addSuppressedCompat(suppressed: Throwable): Unit = addSuppressed(suppressed)

actual fun Throwable.initCauseCompat(cause: Throwable): Throwable = initCause(cause)
