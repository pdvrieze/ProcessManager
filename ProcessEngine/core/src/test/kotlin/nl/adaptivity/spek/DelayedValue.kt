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

package nl.adaptivity.spek

import org.jetbrains.spek.api.dsl.SpecBody
import org.jetbrains.spek.api.lifecycle.LifecycleAware
import kotlin.reflect.KProperty

/**
 * Created by pdvrieze on 19/01/17.
 */
class DelayedValue<T>: LifecycleAware<T> {
  var value: T? = null

  fun set(value: T?) {
    this.value = value
  }

  fun get(): T {
    return value as T
  }

  override operator fun invoke() = get()

  override fun getValue(thisRef: Any?, property: KProperty<*>) = get()
}

fun <T> SpecBody.delayedGroup(factory: ()->T): DelayedValue<T> {
  return DelayedValue<T>().apply {
    beforeGroup { set(factory()) }
    afterGroup { set(null) }
  }
}