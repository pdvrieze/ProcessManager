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

import net.devrieze.util.PrefixMap
import net.devrieze.util.PrefixMap.Entry
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import java.util.*


class PrefixMapTest {

    private lateinit var map: PrefixMap<String>

    private lateinit var entryList: ArrayList<Entry<String>>

    private lateinit var expected: Array<Any>

    private val entryComparator: Comparator<Entry<String>> = Comparator { o1, o2 ->
        o1.prefix.toString().compareTo(o2.prefix.toString())
    }

    @BeforeEach
    fun setUp() {
        map = PrefixMap()
        entryList = ArrayList()
        entryList.add(Entry("bb", "bb"))
        entryList.add(Entry("aaa", "aaa"))
        entryList.add(Entry("ccc", "ccc"))
        entryList.add(Entry("bba", "bba"))
        entryList.add(Entry("ba", "ba"))

        val expectedList = ArrayList<Entry<String>>(entryList.size)
        expectedList.addAll(entryList)
        Collections.sort(expectedList, entryComparator)

        expected = expectedList.toTypedArray()
    }

    @Test
    fun testPut() {
        for (entry in entryList) {
            map.put(entry.prefix, entry.value)
            assertTrue(map.contains(entry), "Contains " + entry.prefix)
        }
        for (entry in entryList) {
            assertTrue(map.contains(entry), "Contains " + entry.prefix)
        }
        assertEquals(EXPECTEDMAPTESTSTRING, map.toTestString())
    }

    @Test
    fun testToList() {
        map.addAll(entryList)
        assertArrayEquals(expected, map.toList().toTypedArray())
    }

    @Test
    fun testIterator() {
        map.addAll(entryList)
        assertArrayEquals(expected, ArrayList(map).toTypedArray())
    }

    @Test
    fun testPrefix1() {
        map.addAll(entryList)
        val result = map.getLonger("b")

        val expected = arrayOf<Entry<*>>(Entry("ba", "ba"), Entry("bb", "bb"), Entry("bba", "bba"))

        assertArrayEquals(expected, result.toTypedArray())

    }

    @Test
    fun testPrefix2() {
        map.addAll(entryList)
        val result = map.getLonger("cc")

        val expected = arrayOf<Entry<*>>(Entry("ccc", "ccc"))

        assertArrayEquals(expected, result.toTypedArray())

    }

    @Test
    fun testPrefix3() {
        map.addAll(entryList)
        val result = map.getLonger("bbax")

        val expected = arrayOf<Entry<*>>()

        assertArrayEquals(expected, result.toTypedArray())

    }

    @Test
    fun testSuffix() {
        map.addAll(entryList)
        val result = map.getPrefixes("bbax")

        val expected = arrayOf<Entry<*>>(Entry("bb", "bb"), Entry("bba", "bba"))

        assertArrayEquals(expected, result.toTypedArray())

    }

    @Test
    fun testPutOthers() {
        map.put("/processInstances/", "a")
        assertEquals("a", first(map.get("/processInstances/")))

        map.put("/processModels", "b")
        assertEquals("a", first(map.get("/processInstances/")))
        assertEquals("b", first(map.get("/processModels")))

        map.put("/processModels/", "c")
        assertEquals("a", first(map.get("/processInstances/")))
        assertEquals("b", first(map.get("/processModels")))
        assertEquals("c", first(map.get("/processModels/")))

        map.put("/processInstances", "d")
        assertEquals("a", first(map.get("/processInstances/")))
        assertEquals("b", first(map.get("/processModels")))
        assertEquals("c", first(map.get("/processModels/")))
        assertEquals("d", first(map.get("/processInstances")))

        map.put("/procvalues/", "e")
        assertEquals("a", first(map.get("/processInstances/")))
        assertEquals("b", first(map.get("/processModels")))
        assertEquals("c", first(map.get("/processModels/")))
        assertEquals("d", first(map.get("/processInstances")))
        assertEquals("e", first(map.get("/procvalues/")))

        map.put("/procvalues", "f")
        assertEquals("a", first(map.get("/processInstances/")))
        assertEquals("b", first(map.get("/processModels")))
        assertEquals("c", first(map.get("/processModels/")))
        assertEquals("d", first(map.get("/processInstances")))
        assertEquals("e", first(map.get("/procvalues/")))
        assertEquals("f", first(map.get("/procvalues")))

        map.put("/", "g")
        assertEquals("a", first(map.get("/processInstances/")))
        assertEquals("b", first(map.get("/processModels")))
        assertEquals("c", first(map.get("/processModels/")))
        assertEquals("d", first(map.get("/processInstances")))
        assertEquals("e", first(map.get("/procvalues/")))
        assertEquals("f", first(map.get("/procvalues")))
        assertEquals("g", first(map.get("/")))

        if (map.isEmpty()) {
            assertEquals(OTHERSEXPECTED, map.toTestString())
        }
    }

    @Test
    fun testRemove() {
        val falseEntry = PrefixMap.Entry("Foo", "Bar")
        map.addAll(entryList)
        val index = 0
        while (entryList.size > 0) {
            assertFalse(map.remove(falseEntry))
            assertTrue(map.contains(entryList[index]))
            map.remove(entryList[index])
            assertFalse(map.contains(entryList[index]))
            entryList.removeAt(index)
            Collections.sort(entryList, entryComparator)
            assertArrayEquals(entryList.toTypedArray(), map.toTypedArray())
        }
    }

    @Test
    fun testRemoveRandom() {
        val falseEntry = PrefixMap.Entry("Foo", "Bar")

        for (i in 20 downTo 1) {
            val entryList = ArrayList(entryList)
            entryList.shuffle()
            map.addAll(entryList)
            entryList.shuffle()
            val index = 0
            while (entryList.size > 0) {
                assertFalse(map.remove(falseEntry))
                assertTrue(map.contains(entryList[index]))
                map.remove(entryList[index])
                assertFalse(map.contains(entryList[index]))
                entryList.removeAt(index)
                Collections.sort(entryList, entryComparator)
                assertArrayEquals(entryList.toTypedArray(), map.toTypedArray())
            }
            assertTrue(map.isEmpty())
        }
    }

    @Test
    fun testBalance() {
        for (s in arrayOf("a", "b", "c", "d", "e", "f", "g")) {
            map.put(s, s)
        }
        assertEquals(EXPECTEDBALANCERESULT, map.toTestString())
    }

    @Test
    fun testBalanceReverse() {
        for (s in arrayOf("g", "f", "e", "d", "c", "b", "a")) {
            map.put(s, s)
        }
        assertEquals(EXPECTEDBALANCERESULT, map.toTestString())
    }

    @Test
    fun testBalanceShuffle() {
        for (s in arrayOf("a", "f", "d", "b", "g", "e", "c")) {
            map.put(s, s)
        }
        assertEquals(EXPECTEDBALANCERESULT, map.toTestString())
    }

    companion object {

        init {
            PrefixMapTest::class.java.classLoader.setDefaultAssertionStatus(true)
        }

        private const val EXPECTEDBALANCERESULT = "     [===== (7)\n" +
                                                  "     |  [===== (7) value=\"d\"\n" +
                                                  "     |  |  [===== (3) value=\"b\"\n" +
                                                  "     |  |  |       [===== (1)\n" +
                                                  "     |  |  | l=\"a\" ] value=\"a\"\n" +
                                                  "     |  |  |       [=====\n" +
                                                  "     |  |  \\----\\\n" +
                                                  "     |  | l=\"b\" ]\n" +
                                                  "     |  |  /----/\n" +
                                                  "     |  |  |       [===== (1)\n" +
                                                  "     |  |  | r=\"c\" ] value=\"c\"\n" +
                                                  "     |  |  |       [=====\n" +
                                                  "     |  |  [=====\n" +
                                                  "     |  \\----\\\n" +
                                                  "\"\"   ] b=\"d\" ]\n" +
                                                  "     |  /----/\n" +
                                                  "     |  |  [===== (3) value=\"f\"\n" +
                                                  "     |  |  |       [===== (1)\n" +
                                                  "     |  |  | l=\"e\" ] value=\"e\"\n" +
                                                  "     |  |  |       [=====\n" +
                                                  "     |  |  \\----\\\n" +
                                                  "     |  | r=\"f\" ]\n" +
                                                  "     |  |  /----/\n" +
                                                  "     |  |  |       [===== (1)\n" +
                                                  "     |  |  | r=\"g\" ] value=\"g\"\n" +
                                                  "     |  |  |       [=====\n" +
                                                  "     |  |  [=====\n" +
                                                  "     |  [=====\n" +
                                                  "     [====="

        private const val EXPECTEDMAPTESTSTRING = "     [===== (5)\n" +
                                                  "     |  [===== (5)\n" +
                                                  "     |  |         [===== (1)\n" +
                                                  "     |  | l=\"aaa\" ] value=\"aaa\"\n" +
                                                  "     |  |         [=====\n" +
                                                  "     |  \\----\\\n" +
                                                  "     |       |   [===== (3) value=\"bb\"\n" +
                                                  "     |       |   |        [===== (1)\n" +
                                                  "     |       |   | l=\"ba\" ] value=\"ba\"\n" +
                                                  "     |       |   |        [=====\n" +
                                                  "     |       |   \\----\\\n" +
                                                  "     |       |        |         [===== (1)\n" +
                                                  "\"\"   ] b=\"b\" ] b=\"bb\" ] b=\"bba\" ] value=\"bba\"\n" +
                                                  "     |       |        |         [=====\n" +
                                                  "     |       |        [=====\n" +
                                                  "     |  /----/\n" +
                                                  "     |  |         [===== (1)\n" +
                                                  "     |  | r=\"ccc\" ] value=\"ccc\"\n" +
                                                  "     |  |         [=====\n" +
                                                  "     |  [=====\n" +
                                                  "     [====="

        private val OTHERSEXPECTED = ""

        private fun first(collection: Collection<Entry<String>>?): String? {
            return if (collection == null || collection.isEmpty()) {
                null
            } else collection.iterator().next().value
        }
    }

}

