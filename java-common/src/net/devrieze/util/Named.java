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
  public class NameCompare<T extends Named> implements Comparator<T> {

    /**
     * This comparator compares the name according to simple string comparison.
     * 
     * @param pObj1 The first Item that should be compared
     * @param pObj2 The item that pObj1 needs to be compared to
     * @return &lt;0 iff pObj1 < pObj2 <br />
     *         ==0 iff pObj1 == pObj2 <br />
     *         &gt;0 iff pObj1 > pObj2
     * @see Comparator#compare(Object, Object)
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
