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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package net.devrieze.util

import kotlin.reflect.KProperty

/**
 * Created by pdvrieze on 21/11/16.
 */
class OverlayProp<T>(private val base: () -> T) {
  private var set=false
  private var value:T? = null

  operator fun getValue(thisRef: Any?, property: KProperty<*>):T {
    return if (set) value as T else base()
  }

  operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    set=true
    this.value = value
  }
}

fun <T> overlay(base: () ->T) = OverlayProp<T>(base)