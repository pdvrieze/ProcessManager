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

/*
 * Created on Nov 3, 2003
 *
 */

package net.devrieze.util;

import java.util.Comparator;


/**
 * The Named interface groups classes that have a name. As result of that name
 * they can be elements of a nameSet, the can be sorted based on their name,
 * etc.
 * 
 * @author Paul de Vrieze
 * @version 1.1 $Revision$
 */
public interface Named {

  /**
   * This class provides a comparator for objects that implement the Named
   * interface.
   * 
   * @param <T> Type of the objects to compare
   * @author Paul de Vrieze
   * @version 1.1 $Revision$
   */
  class NameCompare<T extends Named> implements Comparator<T> {

    /**
     * This comparator compares the name according to simple string comparison.
     * 
     * @param pObj1 The first Item that should be compared
     * @param pObj2 The item that pObj1 needs to be compared to
     * @return &lt;0 iff pObj1 < pObj2 <br />
     *         ==0 iff pObj1 == pObj2 <br />
     *         &gt;0 iff pObj1 > pObj2
     */
    @Override
    public int compare(final T pObj1, final T pObj2) {
      return pObj1.getName().compareTo(pObj2.getName());
    }
  }

  /**
   * Create a new object that is a clone of this one, except that it has the
   * given name.
   * 
   * @param pName The new attribute of the name property.
   * @return The new object
   */
  Named newWithName(String pName);

  /**
   * Get the name of the object.
   * 
   * @return The name of the object
   */
  String getName();
}
