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

actual class Class<T:Any?>(val name:String)

actual val KClass<*>.nameCompat get() = js.name

actual class UUID(val text:String) {
    override fun toString() = text
}

actual fun randomUUID(): UUID {
    throw UnsupportedOperationException("Javascript cannot generate random uuids yet")
}

actual fun String.toUUID(): UUID = UUID(this)

actual fun assert(value: Boolean, lazyMessage: () -> String) {
    if (!value) console.error("Assertion failed: ${lazyMessage()}")
}

actual fun assert(value: Boolean) {
    if (!value) console.error("Assertion failed")
}

actual interface AutoCloseable {
    actual fun close()
}

actual interface Closeable: AutoCloseable

actual fun interface Runnable {
    actual fun run()
}

actual inline fun <reified T:Any> isTypeOf(value: Any):Boolean = jsTypeOf(value) == T::class.js.name

actual fun Throwable.addSuppressedCompat(suppressed: Throwable):Unit {
    if(js("suppressed == undefined") as Boolean) {
        js("suppressed = []")
    }
    asDynamic().suppressed.push(suppressed)
}

actual fun Throwable.initCauseCompat(cause: Throwable):Throwable = apply {
    asDynamic().cause = cause
}
