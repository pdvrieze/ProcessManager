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

infix fun Byte.hasFlag(flag:Byte):Boolean = (this.toInt() and flag.toInt()) == flag.toInt()
infix fun Short.hasFlag(flag:Short):Boolean = (this.toInt() and flag.toInt()) == flag.toInt()
infix fun Int.hasFlag(flag:Int):Boolean = (this and flag) == flag
infix fun Long.hasFlag(flag:Long):Boolean = (this and flag) == flag
