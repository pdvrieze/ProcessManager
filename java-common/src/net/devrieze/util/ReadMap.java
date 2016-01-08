/*
 * Created on Nov 4, 2003
 *
 */

package net.devrieze.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;


/**
 * <p>
 * The ReadMap interface is an interface that tries to maximally add the reading
 * methods from the Map interface to the combination of the Set and Collecion
 * interfaces. This interface aims to provide the possibility to use the fact
 * that elements of the set are uniquely adressable by a key which is a subset
 * of the VALUETYPE.
 * </p>
 *
 * @param <K> The K generic specifies the type of the keys of the map.
 * @param <V> The V generic specifies the type of the values.
 * @author Paul de Vrieze
 * @version 1.1 $Revision$
 */
public interface ReadMap<K, V> extends Set<V> {

  /**
   * Check whether there are elements in the ReadMap.
   *
   * @return <code>true</code> if empty, <code>false</code> if not
   * @see Map#isEmpty()
   */
  @Override
  boolean isEmpty();

  /**
   * Checks whether the specified key is available in the map.
   *
   * @param pKey The key to check.
   * @return <code>true</code> if the key is contained
   * @see Map#containsKey(Object)
   */
  boolean containsKey(K pKey);

  /**
   * Check whether the value is contained in the ReadMap.
   *
   * @param pValue The value to check
   * @return <code>true</code> if contained, <code>false</code> if not.
   * @see Map#containsValue(Object)
   */
  boolean containsValue(V pValue);

  /**
   * Check whether the map equals another map.
   *
   * @param pObject The element to compare to
   * @return <code>true</code> if equal, <code>false</code> if not
   * @see Map#equals(Object)
   */
  @Override
  boolean equals(Object pObject);

  /**
   * Get the value corresponding to the specified key.
   *
   * @param pKey The key of the value that needs to be retrieved.
   * @return the value corresponding to the key, or null if it does not exist.
   * @see Map#get(Object)
   */
  V get(K pKey);

  /**
   * Get a set of all keys that are used in this ReadMap.
   *
   * @return The set of all keys
   * @see Map#keySet()
   */
  Set<K> keySet();

  /**
   * Get the amount of elements in this ReadMap.
   *
   * @return the amount of elements
   * @see Map#size()
   */
  @Override
  int size();

  /**
   * Get the set of all values contained in this ReadMap. As ReadMap itself
   * implements Collection, returning this should be a sufficient
   * implementation. If that is not the case, the implementation is wrong.
   *
   * @return A collection of all values in this map.
   * @see Map#values()
   */
  Collection<V> values();
}
