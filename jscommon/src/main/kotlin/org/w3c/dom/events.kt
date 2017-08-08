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

package org.w3c.dom.events

import org.w3c.dom.Element

inline val Event.bubbles get() = asDynamic().bubbles as Boolean
inline val Event.cancelable get() = asDynamic().cancelable as Boolean
inline val Event.currentTarget get() = asDynamic().currentTarget as Boolean
inline val Event.defaultPrevented get() = asDynamic().defaultPrevented as Boolean
inline val Event.eventPhase get() = asDynamic().eventPhase as Number
inline val Event.isTrusted get() = asDynamic().isTrusted as Boolean
inline val Event.srcElement: Element? get() = asDynamic().srcElement as Element?
inline val Event.target get() = asDynamic().target as EventTarget?
inline val Event.timeStamp get() = asDynamic().target as Number

fun myTest() = true