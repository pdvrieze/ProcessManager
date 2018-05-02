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

package net.devrieze.util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*


/**
 * Created by pdvrieze on 16/10/16.
 */
class ArraySetTest {

    @Test
    fun testSize() {
        val set = ArraySet<Int>()
        Assertions.assertEquals(set.size, 0)
        set.add(5)
        Assertions.assertEquals(set.size, 1)
        set.add(3)
        Assertions.assertEquals(set.size, 2)
    }

    @Test
    fun testIterator() {
        val set = ArraySet<Int>()
        val elements = arrayOf(7, 2, 8, 1, 5, 6)
        set.addAll(elements)
        val iterator = set.iterator()
        Assertions.assertEquals(iterator.next(), 7)
        Assertions.assertEquals(iterator.next(), 2)
        Assertions.assertEquals(iterator.next(), 8)
        Assertions.assertEquals(iterator.next(), 1)
        Assertions.assertEquals(iterator.next(), 5)
        Assertions.assertEquals(iterator.next(), 6)
        assertFalse(iterator.hasNext())

        Assertions.assertEquals(set.asSequence().toList(), elements.toList())

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
    fun testAddReservation() {
        val set = ArraySet<Int>(2)
        for (i in 1..100) {
            set += i
            Assertions.assertEquals(set.size, i)
            for (j in 1..i)
                assertTrue(j in set)
        }
    }

    @Test
    fun testRemoveFirst() {
        val set = ArraySet<Char?>()
        set.addAll(arrayOf('7', '2', '8', '1', '5', '6'))
        assertTrue(set.remove('7'))
        Assertions.assertEquals(set.size, 5)
        assertFalse(set.remove('7'))
        assertFalse(set.contains(null))
    }

    @Test
    fun testRemoveMiddle() {
        val set = ArraySet<Char?>()
        set.addAll(arrayOf('7', '2', '8', '1', '5', '6'))
        assertTrue(set.remove('1'))
        Assertions.assertEquals(set.size, 5)
        assertFalse(set.remove('1'))
        assertFalse(set.contains(null))
    }

    @Test
    fun testRemoveLast() {
        val set = ArraySet<Char?>()
        set.addAll(arrayOf('7', '2', '8', '1', '5', '6'))
        assertTrue(set.remove('6'))
        Assertions.assertEquals(set.size, 5)
        assertFalse(set.remove('6'))
        assertFalse(set.contains(null))
    }

    @Test
    fun testRemovePosFirst() {
        val set = ArraySet<Char?>()
        set.addAll(arrayOf('7', '2', '8', '1', '5', '6'))
        Assertions.assertEquals(set.removeAt(0), '7')
        Assertions.assertEquals(set.size, 5)
        assertFalse(set.remove('7'))
        assertFalse(set.contains(null))
    }

    @Test
    fun testRemovePosMiddle() {
        val set = ArraySet<Char?>()
        set.addAll(arrayOf('7', '2', '8', '1', '5', '6'))
        Assertions.assertEquals(set.removeAt(3), '1')
        Assertions.assertEquals(set.size, 5)
        assertFalse(set.remove('1'))
        assertFalse(set.contains(null))
    }

    @Test
    fun testRemovePosLast() {
        val set = ArraySet<Char?>()
        set.addAll(arrayOf('7', '2', '8', '1', '5', '6'))
        Assertions.assertEquals(set.removeAt(5), '6')
        Assertions.assertEquals(set.size, 5)
        assertFalse(set.remove('6'))
        assertFalse(set.contains(null))
    }

    @Test
    fun testIndexOf() {
        val set = ArraySet<Char>()
        set.addAll(arrayOf('7', '2', '8', '1', '5', '6'))
        Assertions.assertEquals(set.indexOf('1'), 3)
    }

    @Test
    fun testClear() {
        val set = ArraySet<Char>()
        set.addAll(arrayOf('7', '2', '8', '1', '5', '6'))
        Assertions.assertEquals(set.size, 6)
        set.clear()
        Assertions.assertEquals(set.size, 0)
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
        Assertions.assertEquals(set.indexOf('6'), 7)

        val it = set.iterator()
        Assertions.assertEquals(it.next(), '7')
        Assertions.assertEquals(it.next(), '8')
        Assertions.assertEquals(it.next(), '1')
        Assertions.assertEquals(it.next(), '2')
        Assertions.assertEquals(it.next(), '3')
        Assertions.assertEquals(it.next(), '4')
        Assertions.assertEquals(it.next(), '5')
        Assertions.assertEquals(it.next(), '6')
        assertFalse(it.hasNext())
        try {
            it.next()
            fail("The iterator should be at the end and throw an exception")
        } catch (e: NoSuchElementException) {
            Assertions.assertEquals(e.message, "The iterator is at the end")
        }
    }

    @Test//(dependsOnMethods = arrayOf("testWrap"))
    fun testWrapRemove1() {
        val set = ArraySet<Char?>()
        val array1 = arrayOf('1', '2', '3', '4', '5', '6')
        val array2 = arrayOf('7', '8')
        set.addAll(array1)
        set.addAll(array2)
        set.removeAll(array1)
        set.addAll(array1)
        Assertions.assertEquals(set.removeAt(4), '3')
        assertFalse(set.contains('3'))
        assertFalse(set.contains(null))
        Assertions.assertEquals(set.size, 7)

        Assertions.assertEquals(set.removeAt(5), '5')
        assertFalse(set.contains('5'))
        assertFalse(set.contains(null))
        Assertions.assertEquals(set.size, 6)
    }

    @Test//(dependsOnMethods = arrayOf("testWrap"))
    fun testWrapRemove2() {
        val set = ArraySet<Char?>()
        val array1 = arrayOf('1', '2', '3', '4', '5', '6')
        val array2 = arrayOf('7', '8')
        set.addAll(array1)
        set.addAll(array2)
        set.removeAll(array1)
        set.addAll(array1)
        Assertions.assertEquals(set.removeAt(3), '2')
        assertFalse(set.contains('2'))
        assertFalse(set.contains(null))
        Assertions.assertEquals(set.size, 7)

        Assertions.assertEquals(set.removeAt(2), '1')
        assertFalse(set.contains('1'))
        assertFalse(set.contains(null))
        Assertions.assertEquals(set.size, 6)
    }

}


