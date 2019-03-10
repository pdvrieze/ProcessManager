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
 * @param <S> The type of the first element.
 * @param <T> The type of the second element.
 * @param <U> The type of the third element.
 * @see net.devrieze.util.Tupple
 * @author Paul de Vrieze
 * @version 1.0 $Revision$
 */
@SuppressWarnings("deprecation")
public final class Tripple<S, T, U> {

  private S mElem1;

  private T mElem2;

  private U mElem3;

  /**
   * Create a tripple with the specified elements.
   * 
   * @param pElem1 The first element
   * @param pElem2 The second element
   * @param pElem3 The third element
   */
  public Tripple(final S pElem1, final T pElem2, final U pElem3) {
    mElem1 = pElem1;
    mElem2 = pElem2;
    mElem3 = pElem3;
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
   * @return the second element
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

  /**
   * Get the third element.
   * 
   * @return The third element
   */
  public U getElem3() {
    return mElem3;
  }

  /**
   * Set the third element.
   * 
   * @param pElem3 the third element
   */
  public void setElem3(final U pElem3) {
    mElem3 = pElem3;
  }

  @Override
  public String toString() {
    return "(" + mElem1 + ", " + mElem2 + ", " + mElem3 + ")";
  }

  public static <X, Y, Z> Tripple<X, Y, Z> tripple(final X elem1, final Y elem2, final Z elem3) {
    return new Tripple<>(elem1, elem2, elem3);
  }
}
