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
