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

package nl.adaptivity.xml

/**
 * Created by pdvrieze on 14/04/16.
 */
internal object StringUtil {

  @Deprecated("Use asString", ReplaceWith("seq.asString()","nl.adaptivity.xml.asString"), DeprecationLevel.ERROR)
  fun toString(seq: CharSequence?) = seq?.toString()

  fun CharSequence?.asString() = this?.toString()

  @Deprecated("Use CharsequenceUtil", ReplaceWith("left matches right", "net.devrieze.util.kotlin.matches"), DeprecationLevel.ERROR)
  fun isEqual(left: CharSequence?, right: CharSequence?): Boolean {
    return if (left == null) right == null else right != null && left.toString() == right.toString()
  }
}
