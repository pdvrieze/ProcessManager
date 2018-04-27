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

/*
 * Created on Oct 9, 2004
 *
 */

package net.devrieze.util;

/**
 * This class implements a tupple as is commonly used in many code snippets.
 * Such functionality could be implemented using
 * 
 * @param <S> The type of the first element in the tupple
 * @param <T> The type of the second element in the tupple
 * @author Paul de Vrieze
 * @version 1.0 $Revision$
 */
public final class Tupple<S, T> {

  private S mElem1;

  private T mElem2;

  /**
   * Create a new tupple.
   * 
   * @param pElem1 The first element
   * @param pElem2 The second element
   */
  public Tupple(final S pElem1, final T pElem2) {
    mElem1 = pElem1;
    mElem2 = pElem2;
  }

  /**
   * Get the first element.
   * 
   * @return the first element
   */
  public S getElem1() {
    return mElem1;
  }

  /**
   * Set the first element.
   * 
   * @param pElem1 the first element
   */
  public void setElem1(final S pElem1) {
    mElem1 = pElem1;
  }

  /**
   * Get the second element.
   * 
   * @return The second element
   */
  public T getElem2() {
    return mElem2;
  }

  /**
   * Set the second element.
   * 
   * @param pElem2 the second element
   */
  public void setElem2(final T pElem2) {
    mElem2 = pElem2;
  }

  @Override
  public String toString() {
    return "(" + mElem1 + ", " + mElem2 + ")";
  }

  public static <S, T> Tupple<S, T> tupple(final S pElem1, final T pElem2) {
    return new Tupple<>(pElem1, pElem2);
  }

  /**
   * Create a new array that has the value from pElem1 at each first postition
   * 
   * @param pElem1
   * @param pElem2
   */
  @SuppressWarnings("unchecked")
  public static <S, T> Tupple<S, T>[] pack1st(final S pElem1, final T[] pElem2) {
    final Tupple<S, T>[] result = new Tupple[pElem2.length];
    for (int i = result.length - 1; i >= 0; --i) {
      result[i] = Tupple.<S, T> tupple(pElem1, pElem2[i]);
    }
    return result;
  }
}
