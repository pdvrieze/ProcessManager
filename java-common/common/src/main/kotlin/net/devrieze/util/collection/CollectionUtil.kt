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

package net.devrieze.util.collection


/**
 * Helper function to reset and set values in a set.
 */
fun <T, C: MutableCollection<in T>> C.replaceBy(vararg elements: T) {
  clear()
  addAll(elements)
}

/**
 * Helper function to reset and set values in a set.
 */
fun <T, C: MutableCollection<in T>> C.replaceByNotNull(element: T?) {
  clear()
  element?.let { add(it) }
}

/**
 * Helper function to reset and set values in a set.
 */
fun <T, C: MutableCollection<in T>> C.replaceBy(elements: Sequence<T>) {
  clear()
  addAll(elements)
}

/**
 * Helper function to reset and set values in a set.
 */
fun <T, C: MutableCollection<in T>> C.replaceBy(elements: Iterable<T>) {
  clear()
  addAll(elements)
}

/**
 * Helper function to reset and set values in a set.
 */
fun <T, C: MutableCollection<in T>> C.replaceBy(elements: Iterator<T>) {
  replaceBy(elements.asSequence())
}

fun <T, R:Comparable<R>> List<T>.maxIndexBy(compare : (T)->R):Int {
  var maxIdx = -1
  var maxValue: R? = null

  for (i in indices) {
    val value = compare(get(i))
    if (maxValue==null || maxValue<=value) {
      maxIdx = i
      maxValue = value
    }
  }
  return maxIdx
}

fun <T, R:Comparable<R>> Array<T>.maxIndexBy(compare : (T)->R):Int {
  var maxIdx = -1
  var maxValue: R? = null

  for (i in indices) {
    val value = compare(get(i))
    if (maxValue==null || maxValue<=value) {
      maxIdx = i
      maxValue = value
    }
  }
  return maxIdx
}

fun <R:Comparable<R>> IntArray.maxIndexBy(compare : (Int)->R):Int {
  var maxIdx = -1
  var maxValue: R? = null

  for (i in indices) {
    val value = compare(get(i))
    if (maxValue==null || maxValue<=value) {
      maxIdx = i
      maxValue = value
    }
  }
  return maxIdx
}

fun <R:Comparable<R>> LongArray.maxIndexBy(compare : (Long)->R):Int {
  var maxIdx = -1
  var maxValue: R? = null

  for (i in indices) {
    val value = compare(get(i))
    if (maxValue==null || maxValue<=value) {
      maxIdx = i
      maxValue = value
    }
  }
  return maxIdx
}

fun <R:Comparable<R>> DoubleArray.maxIndexBy(compare : (Double)->R):Int {
  var maxIdx = -1
  var maxValue: R? = null

  for (i in indices) {
    val value = compare(get(i))
    if (maxValue==null || maxValue<=value) {
      maxIdx = i
      maxValue = value
    }
  }
  return maxIdx
}

fun <R:Comparable<R>> FloatArray.maxIndexBy(compare : (Float)->R):Int {
  var maxIdx = -1
  var maxValue: R? = null

  for (i in indices) {
    val value = compare(get(i))
    if (maxValue==null || maxValue<=value) {
      maxIdx = i
      maxValue = value
    }
  }
  return maxIdx
}

fun <T, R:Comparable<R>> List<T>.minIndexBy(compare : (T)->R):Int {
  var minIdx = -1
  var minValue: R? = null

  for (i in indices) {
    val value = compare(get(i))
    if (minValue==null || minValue>value) {
      minIdx = i
      minValue = value
    }
  }
  return minIdx
}

fun <T, R:Comparable<R>> Array<T>.minIndexBy(compare : (T)->R):Int {
  var minIdx = -1
  var minValue: R? = null

  for (i in indices) {
    val value = compare(get(i))
    if (minValue==null || minValue>value) {
      minIdx = i
      minValue = value
    }
  }
  return minIdx
}

fun <R:Comparable<R>> IntArray.minIndexBy(compare : (Int)->R):Int {
  var minIdx = -1
  var minValue: R? = null

  for (i in indices) {
    val value = compare(get(i))
    if (minValue==null || minValue>value) {
      minIdx = i
      minValue = value
    }
  }
  return minIdx
}

fun <R:Comparable<R>> LongArray.minIndexBy(compare : (Long)->R):Int {
  var minIdx = -1
  var minValue: R? = null

  for (i in indices) {
    val value = compare(get(i))
    if (minValue==null || minValue>value) {
      minIdx = i
      minValue = value
    }
  }
  return minIdx
}

fun <R:Comparable<R>> DoubleArray.minIndexBy(compare : (Double)->R):Int {
  var minIdx = -1
  var minValue: R? = null

  for (i in indices) {
    val value = compare(get(i))
    if (minValue==null || minValue>value) {
      minIdx = i
      minValue = value
    }
  }
  return minIdx
}

fun <R:Comparable<R>> FloatArray.minIndexBy(compare : (Float)->R):Int {
  var minIdx = -1
  var minValue: R? = null

  for (i in indices) {
    val value = compare(get(i))
    if (minValue==null || minValue>value) {
      minIdx = i
      minValue = value
    }
  }
  return minIdx
}

fun IntArray.maxIndex():Int {
  var maxIdx = -1
  var maxValue = Int.MIN_VALUE
  
  for (i in indices) {
    val value = get(i)
    if (maxValue<=value) {
      maxIdx = i
      maxValue = value
    }
  }
  return maxIdx
}

fun LongArray.maxIndex():Int {
  var maxIdx = -1
  var maxValue = Long.MIN_VALUE
  
  for (i in indices) {
    val value = get(i)
    if (maxValue<=value) {
      maxIdx = i
      maxValue = value
    }
  }
  return maxIdx
}

fun DoubleArray.maxIndex():Int {
  var maxIdx = -1
  var maxValue = Double.NEGATIVE_INFINITY

  for (i in indices) {
    val value = get(i)
    if (maxValue<=value) {
      maxIdx = i
      maxValue = value
    }
  }
  return maxIdx
}

fun FloatArray.maxIndex():Int {
  var maxIdx = -1
  var maxValue = Float.NEGATIVE_INFINITY

  for (i in indices) {
    val value = get(i)
    if (maxValue<=value) {
      maxIdx = i
      maxValue = value
    }
  }
  return maxIdx
}

fun IntArray.minIndex():Int {
  var minIdx = -1
  var minValue = Int.MAX_VALUE
  
  for (i in indices.reversed()) {
    val value = get(i)
    if (minValue>=value) {
      minIdx = i
      minValue = value
    }
  }
  return minIdx
}

fun LongArray.minIndex():Int {
  var minIdx = -1
  var minValue = Long.MAX_VALUE

  for (i in indices.reversed()) {
    val value = get(i)
    if (minValue>=value) {
      minIdx = i
      minValue = value
    }
  }
  return minIdx
}

fun DoubleArray.minIndex():Int {
  var minIdx = -1
  var minValue = Double.POSITIVE_INFINITY

  for (i in indices.reversed()) {
    val value = get(i)
    if (minValue>=value) {
      minIdx = i
      minValue = value
    }
  }
  return minIdx
}

fun FloatArray.minIndex():Int {
  var minIdx = -1
  var minValue = Float.POSITIVE_INFINITY

  for (i in indices.reversed()) {
    val value = get(i)
    if (minValue>=value) {
      minIdx = i
      minValue = value
    }
  }
  return minIdx
}

interface ArrayAccess<K,V> {
  operator fun get(key:K):V?
}