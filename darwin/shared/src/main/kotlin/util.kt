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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

/**
 * Generic utility methods for use in kotlin
 */
package net.devrieze.util

inline fun <T> T?.valIfNot(alternate:T, condition:T.()->Boolean):T =
    if (this!=null && condition()) this else alternate


inline fun <T> T.valIf(alternate:T, condition:T.()->Boolean):T =
    if (condition()) alternate else this

inline fun <T> T?.valIfNullOr(alternate:T, condition:T.()->Boolean):T =
    if (this==null || condition()) alternate else this

inline fun <T> T?. nullIfNot(condition:T.()->Boolean):T? =
    if (this?.condition()?:false) this else null

interface __Override__<T> {
  infix fun by(alternative:T):T
}

class __Override__T<T>():__Override__<T> {
  inline final override infix fun by(alternative: T):T {
    return alternative
  }
}

class __Override__F<T>(val value:T):__Override__<T> {
  inline final override infix fun by(alternative: T):T {
    return value
  }
}

inline fun <T> T?.overrideIf(crossinline condition: T.() -> Boolean): __Override__<T> {
  return if (this==null || condition()) __Override__T() else __Override__F(this)
}
