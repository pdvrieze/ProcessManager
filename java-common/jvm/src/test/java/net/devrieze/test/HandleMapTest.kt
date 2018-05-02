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

package net.devrieze.test

import net.devrieze.util.MemHandleMap
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import java.util.ArrayList


class HandleMapTest {

    private lateinit var map: MemHandleMap<String>

    @BeforeEach
    fun setUp() {
        map = MemHandleMap(5)
    }

    @Test
    fun testPut1() {
        val a = map.put("A")
        assertEquals(map.size(), 1)
        val b = map.put("B")
        assertEquals(map.size(), 2)
    }

    @Test
    fun testPut2() {
        // This depends on the implementation
        val a = map.put("A")
        assertEquals(a.handleValue, 0L)
        val b = map.put("B")
        assertEquals(b.handleValue, 1L)
    }

    @Test
    fun testPut3() {
        val a = map.put("A")
        //    assertEquals("A", mMap.get(a));
        val b = map.put("B")
        assertEquals(map[b], "B")
    }

    @Test
    fun testPutGet() {
        val a = map.put("A")
        val b = map.put("B")
        val c = map.put("C")
        val d = map.put("D")
        val e = map.put("E")
        val f = map.put("F")
        val g = map.put("G")
        val h = map.put("H")
        val i = map.put("I")
        val j = map.put("J")
        assertEquals(map.size(), 10)

        assertEquals(map[a], "A", "Result of getting")
        assertEquals(map[b], "B", "Result of getting")
        assertEquals(map[c], "C", "Result of getting")
        assertEquals(map[d], "D", "Result of getting")
        assertEquals(map[e], "E", "Result of getting")
        assertEquals(map[f], "F", "Result of getting")
        assertEquals(map[g], "G", "Result of getting")
        assertEquals(map[h], "H", "Result of getting")
        assertEquals(map[i], "I", "Result of getting")
        assertEquals(map[j], "J", "Result of getting")
    }

    @Test
    fun testRemove() {
        val a = map.put("A")
        val b = map.put("B")
        val c = map.put("C")
        val d = map.put("D")
        val e = map.put("E")
        map.remove(a)
        val a2 = map.put("A2")
        assertEquals(map[a2], "A2")
        assertEquals(a2.handleValue, 5L)
    }

    @Test
    fun testRemove2() {
        val a = map.put("A")
        val b = map.put("B")
        val c = map.put("C")
        val d = map.put("D")
        val e = map.put("E")
        map.remove(c)
        map.remove(b)
        map.remove(a)
        val a2 = map.put("A2")
        val b2 = map.put("B2")
        val c2 = map.put("C2")
        assertEquals(map[a2], "A2")
        assertEquals(map[b2], "B2")
        assertEquals(map[c2], "C2")
        assertEquals(a2.handleValue, 5L)
        assertEquals(b2.handleValue, 6L)
        assertEquals(c2.handleValue, 7L)
        val f = map.put("F")
        assertEquals(map[f], "F")
        assertEquals(map[d], "D")
        assertEquals(map[e], "E")
    }

    @Test
    fun testRemove3() {
        val a = map.put("A")
        val b = map.put("B")
        val c = map.put("C")
        val d = map.put("D")
        val e = map.put("E")
        map.remove(c)
        map.remove(b)
        val b2 = map.put("B2")
        val c2 = map.put("C2")
        map.remove(a)
        val a2 = map.put("A2")
        assertEquals(map[a2], "A2")
        assertEquals(map[b2], "B2")
        assertEquals(map[c2], "C2")
        assertEquals(a2.handleValue, 5L)
        assertEquals(b2.handleValue, 0x100000001L)
        assertEquals(c2.handleValue, 0x100000002L)
        val f = map.put("F")
        assertEquals(map[f], "F")
        assertEquals(map[d], "D")
        assertEquals(map[e], "E")
    }

    @Test
    fun testRemoveAll() {
        val a = map.put("A")
        val b = map.put("B")
        val c = map.put("C")
        val d = map.put("D")
        val e = map.put("E")
        map.remove(c)
        map.remove(b)
        map.remove(a)
        map.remove(e)
        map.remove(d)
        assertEquals(map.size(), 0)
    }

    @Test
    fun testEmptyIterator() {
        val list = ArrayList<String>()
        for (s in map) {
            list.add(s)
        }
        assertEquals(list.size, 0)
    }

    @Test
    fun testIterator() {
        val list = ArrayList<String>()
        map.put("A")
        map.put("B")
        map.put("C")
        map.put("D")
        map.put("E")
        for (s in map) {
            list.add(s)
        }
        assertEquals(list.size, 5)
        assertEquals(list[0], "A", "Result of getting")
        assertEquals(list[1], "B", "Result of getting")
        assertEquals(list[2], "C", "Result of getting")
        assertEquals(list[3], "D", "Result of getting")
        assertEquals(list[4], "E", "Result of getting")
    }

    @Test
    fun testIterator2() {
        val a = map.put("A")
        map.put("B")
        map.put("C")
        map.remove(a)
        val it = map.iterator()

        assertEquals(map.size(), 2)


        assertEquals(it.next(), "B", "Result of getting")
        assertEquals(it.next(), "C", "Result of getting")
        assertFalse(it.hasNext(), "HasNext should not offer more elements")
    }

}

