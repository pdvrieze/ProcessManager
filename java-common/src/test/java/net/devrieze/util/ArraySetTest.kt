/*
 * Copyright (c) 2016.
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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package net.devrieze.util

import org.testng.annotations.Test

import org.testng.Assert.*


/**
 * Created by pdvrieze on 16/10/16.
 */
class ArraySetTest {

  @Test
  fun testSize() {
    val set = ArraySet<Int>()
    assertEquals(set.size, 0)
    set.add(5)
    assertEquals(set.size, 1)
    set.add(3)
    assertEquals(set.size, 2)
  }

  @Test
  fun testIterator() {
    val set = ArraySet<Int>()
    val elements = arrayOf(7, 2, 8, 1, 5, 6)
    set.addAll(elements)
    assertEquals(set.asSequence().toList(), listOf(elements))
  }

  @Test
  fun testContains() {
    val set = ArraySet<Int>()
    set.addAll(arrayOf(7, 2, 8, 1, 5, 6))
    assertTrue(set.contains(5))
    assertFalse(set.contains(0))
    set.add(0)
    assertTrue(set.contains(0))
  }

  @Test
  fun testAdd() {
    val set = ArraySet<Int>()
    assertTrue(set.isEmpty())
    assertTrue(set.add(5))
    assertTrue(set.contains(5))
    assertFalse(set.add(5))
    assertTrue(set.add(3))
  }

  @Test
  fun testRemoveFirst() {
    val set = ArraySet<Char>()
    set.addAll(arrayOf('7', '2', '8', '1', '5', '6'))
    assertTrue(set.remove('7'))
    assertEquals(set.size, 5)
    assertFalse(set.remove('7'))
    assertFalse(set.contains(null))
  }

  @Test
  fun testIndexOf() {
    val set = ArraySet<Char>()
    set.addAll(arrayOf('7', '2', '8', '1', '5', '6'))
    assertEquals(set.indexOf('1'), 3)
  }

  @Test
  fun testClear() {
    val set = ArraySet<Char>()
    set.addAll(arrayOf('7', '2', '8', '1', '5', '6'))
    assertEquals(set.size, 6)
    set.clear()
    assertEquals(set.size,0)
    assertFalse(set.contains('7'))
  }

  @Test
  fun testWrap() {
    val set = ArraySet<Char>()
    val array1 = arrayOf('1', '2', '3', '4', '5', '6')
    val array2 = arrayOf('7', '8')
    set.addAll(array1)
    set.addAll(array2)
    set.removeAll(array1)
    set.addAll(array1)
    (array1.asSequence() + array2.asSequence()).forEach {
      assertTrue(set.contains(it))
    }
    assertEquals(set.indexOf('6'), 7)
  }

}