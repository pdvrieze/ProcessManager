package net.devrieze.util;

import java.lang.annotation.Annotation;

import net.devrieze.annotations.NotNull;


public final class Annotations {

  private Annotations() {} // No instantiation

  /**
   * Get an annotation from a list of annotations.
   *
   * @param <T> The type of the annotation
   * @param pAnnotations The array of the annotations to search.
   * @param pClass The type of the annotation
   * @return <code>null</code> if not found, the annotation otherwise.
   */
  public static <T> T getAnnotation(final Annotation[] pAnnotations, final Class<T> pClass) {
    for (final Annotation a : pAnnotations) {
      if (pClass.isInstance(a)) {
        return pClass.cast(a);
      }
    }
    return null;
  }

  @NotNull
  public static final <T> T notNull(T pObject) {
    assert pObject!=null;
    return pObject;
  }

}
