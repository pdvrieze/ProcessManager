/*
 * Created on Jan 20, 2004
 *
 */

package net.devrieze.util;

/**
 * An iterator for walking over string parts consequtively.
 * 
 * @author Paul de Vrieze
 * @version 0.1 $Revision$
 */
public interface StringRepIterator {

  /**
   * Does the iterator have more elements.
   * 
   * @return <code>true</code> if there is more
   */
  boolean hasNext();

  /**
   * Get the item that is currently being displayed.
   * 
   * @return The current item
   */
  StringRep last();

  /**
   * The next string item.
   * 
   * @return if the list is full
   */
  StringRep next();
}
