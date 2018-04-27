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
 * Created on Nov 4, 2003
 *
 */

package net.devrieze.util


/**
 * The ReadMap interface is an interface that tries to maximally add the reading
 * methods from the [Map] interface to the combination of the [Set] and [Collection]
 * interfaces. This interface aims to provide the possibility to use the fact
 * that elements of the set are uniquely adressable by a key which is a subset
 * of the value type [V].
 *
 * @param K The K generic specifies the type of the keys of the map. This cannot be null.
 *
 * @param V The V generic specifies the type of the values.
 *
 * @author Paul de Vrieze
 *
 * @version 1.1 $Revision$
 */
interface ReadMap<K:Any, out V> : Set<V> {

  /**
   * Check whether there are elements in the ReadMap.

   * @return `true` if empty, `false` if not
   *
   * @see Map.isEmpty
   */
  override fun isEmpty(): Boolean

  /**
   * Checks whether the specified key is available in the map.

   * @param pKey The key to check.
   *
   * @return `true` if the key is contained
   *
   * @see Map.containsKey
   */
  fun containsKey(pKey: K): Boolean

  /**
   * Check whether the value is contained in the ReadMap.

   * @param pValue The value to check
   *
   * @return `true` if contained, `false` if not.
   *
   * @see Map.containsValue
   */
  fun containsValue(pValue: @kotlin.UnsafeVariance V): Boolean

  /**
   * Check whether the map equals another map.

   * @param other The element to compare to
   *
   * @return `true` if equal, `false` if not
   *
   * @see Map.equals
   */
  override fun equals(other: Any?): Boolean

  /**
   * Get the value corresponding to the specified key.

   * @param pKey The key of the value that needs to be retrieved.
   *
   * @return the value corresponding to the key, or null if it does not exist.
   *
   * @see Map.get
   */
  operator fun get(pKey: K): V?

  /**
   * Get a set of all keys that are used in this ReadMap.

   * @return The set of all keys
   *
   * @see Map.keySet
   */
  @Deprecated("Use property instead of the function", ReplaceWith("keys"))
  fun keySet(): Set<K> = keys

  val keys: Set<K>

  /**
   * Get the amount of elements in this ReadMap.

   * @return the amount of elements
   *
   * @see Map.size
   */
  override val size:Int

  /**
   * Get the set of all values contained in this ReadMap. As ReadMap itself
   * implements Collection, returning this should be a sufficient
   * implementation. If that is not the case, the implementation is wrong.

   * @return A collection of all values in this map.
   *
   * @see Map.values
   */
  fun values(): Set<V> = this

}

val <V> ReadMap<*,V>.values get() = this

interface MutableReadMap<K:Any, V>: ReadMap<K, V>, MutableSet<V>