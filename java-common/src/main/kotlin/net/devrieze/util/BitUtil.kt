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

package net.devrieze.util

@Suppress("NOTHING_TO_INLINE")
inline infix fun Byte.hasFlag(flag:Byte):Boolean = this and flag == flag
@Suppress("NOTHING_TO_INLINE")
inline infix fun Short.hasFlag(flag:Short):Boolean = this and flag == flag
@Suppress("NOTHING_TO_INLINE")
inline infix fun Int.hasFlag(flag:Int):Boolean = this and flag == flag
@Suppress("NOTHING_TO_INLINE")
inline infix fun Long.hasFlag(flag:Long):Boolean = this and flag == flag