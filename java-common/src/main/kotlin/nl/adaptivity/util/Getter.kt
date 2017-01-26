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

package nl.adaptivity.util

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class ObjGetter<R,T>(val getter: R.()->T) : ReadOnlyProperty<R, T> {
  override fun getValue(thisRef: R, property: KProperty<*>) = thisRef.getter()
}
fun <R,T> objGetter(getter: R.() -> T) = ObjGetter<R,T>(getter)

class Getter<T>(val getter: ()->T) : ReadOnlyProperty<Any?, T> {
  override fun getValue(thisRef: Any?, property: KProperty<*>) = getter()
  operator fun invoke() = getter()
}
fun <T> getter(getter: () -> T) = Getter<T>(getter)
