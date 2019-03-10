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

expect class Locale

expect object Locales {
    val DEFAULT: Locale
    val ENGLISH: Locale
}

fun CharSequence.toLowercase(locale: Locale): String =
    toString().toLowercase(locale)

@Suppress("NOTHING_TO_INLINE")
inline fun CharSequence.toLowercase(): String = toString().toLowerCase()

@Suppress("NOTHING_TO_INLINE")
@Deprecated("Use builtin version", ReplaceWith("toString().toLowerCase()"))
inline fun String.toLowercase(): String = toString().toLowerCase()

expect fun String.toLowercase(locale: Locale):String

expect fun Int.toHex():String

fun CharSequence.toCharArray(): CharArray = CharArray(length) { get(it) }

expect fun String.toCharArray(): CharArray