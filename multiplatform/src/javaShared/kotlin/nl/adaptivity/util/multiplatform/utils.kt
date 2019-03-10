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

import java.lang.AutoCloseable

actual fun assert(value: Boolean, lazyMessage: () -> String) {
    kotlin.assert(value, lazyMessage)
}

actual fun assert(value: Boolean) = kotlin.assert(value)

actual typealias AutoCloseable = java.lang.AutoCloseable

actual typealias Closeable = java.io.Closeable