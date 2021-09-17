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

package net.devrieze.util

import kotlin.reflect.KProperty

/**
 * Created by pdvrieze on 21/11/16.
 */
class OverlayProp<T>(private val update:(T, T)->T = { _, new -> new }, private val base: () -> T) {

    constructor(update: (T) -> T, base: () -> T): this( { _, new -> update(new) }, base)

    private var set=false
    private var value:T? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>):T {
        @Suppress("UNCHECKED_CAST")
        return if (set) value as T else base()
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val oldValue = getValue(thisRef, property)
        if(oldValue!=value) {
            set = true
            this.value = update(oldValue, value)
        }
    }

    override fun toString(): String = when {
        set -> "= $value"
        else -> "<unset>"
    }
}

fun <T> overlay(update:(T)->T = {it}, base: () ->T) = OverlayProp<T>(update, base)

fun <T> overlay2(update:(T, T)->T = { _, new -> new }, base: () ->T) = OverlayProp<T>(update, base)
