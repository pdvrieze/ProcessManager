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

package net.devrieze.util;

import java.lang.annotation.Annotation;


public final class Annotations {

  private Annotations() {} // No instantiation

  /**
   * Get an annotation from a list of annotations.
   *
   * @param <T> The type of the annotation
   * @param annotations The array of the annotations to search.
   * @param clazz The type of the annotation
   * @return <code>null</code> if not found, the annotation otherwise.
   */
  public static <T> T getAnnotation(final Annotation[] annotations, final Class<T> clazz) {
    for (final Annotation a : annotations) {
      if (clazz.isInstance(a)) {
        return clazz.cast(a);
      }
    }
    return null;
  }

  @Deprecated
  public static <T> T notNull(T pObject) {
    return pObject;
  }

}
