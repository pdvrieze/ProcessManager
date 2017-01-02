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

package net.devrieze.util


object Annotations {

  /**
   * Get an annotation from a list of annotations.

   * @param  The type of the annotation
   * *
   * @param annotations The array of the annotations to search.
   * *
   * @param clazz The type of the annotation
   * *
   * @return `null` if not found, the annotation otherwise.
   */
  @JvmStatic
  fun <T> getAnnotation(annotations: Array<Annotation>, clazz: Class<T>): T? {
    for (a in annotations) {
      if (clazz.isInstance(a)) {
        return clazz.cast(a)
      }
    }
    return null
  }

  @JvmStatic
  @Deprecated("Not needed with Kotlin")
  fun <T> notNull(pObject: T): T {
    return pObject
  }

}// No instantiation
