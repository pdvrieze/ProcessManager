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
 * Created on Oct 28, 2004
 */

package net.devrieze.util

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.PrintStream
import java.util.ArrayList

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.BeforeAll


/**
 * @author Paul de Vrieze
 * @version 1.0 $Revision$
 */
class FastStackTest {

    /**
     * Test that creation of FastStacks is correct.
     */
    @Test
    fun testCreate() {
        val a = "a"
        val s1 = FastStack<Any>(a)
        assertTrue(s1.last() === a)
    }

    /**
     * Test the append function.
     */
    @Test
    fun testAppend() {
        val a = "a"
        val b = "b"
        val s1 = FastStack<Any>(a)
        val s2 = s1.append(b)
        assertTrue(s2.last() === b)
    }

    /**
     * Test that the contains function works correctly.
     */
    @Test
    fun testContains() {
        val a = "a"
        val b = "b"
        val c = "c"
        val s1 = FastStack<Any>(a)
        val s2 = s1.append(b)
        assertTrue(s2.contains(a))
        assertTrue(s2.contains(b))
        assertTrue(!s2.contains(c))
    }

    /**
     * Test the containsAll function.
     */
    @Test
    fun testContainsAll() {
        val a = "a"
        val b = "b"
        val c = "c"
        val d = "d"
        val s1 = FastStack<Any>(a)
        val s2 = s1.append(b).append(c)
        val l = ArrayList<Any>()
        l.add(a)
        l.add(b)

        assertTrue(s2.containsAll(l))
        l.add(d)
        assertTrue(!s2.containsAll(l))
    }

    /**
     * Thest the correctness of the size function.
     */
    @Test
    fun testSize() {
        val a = "a"
        val b = "b"
        val c = "c"
        val s1 = FastStack<Any>(a)
        assertEquals(s1.size, 1)
        val s2 = s1.append(b).append(c)
        assertEquals(s2.size, 3)
    }

    /**
     * Test that creation based on an iterator works.
     */
    @Test
    fun testCreateIterator() {
        val a = "a"
        val b = "b"
        val c = "c"
        val d = "d"
        val l = ArrayList<Any>()
        l.add(a)
        l.add(b)
        l.add(c)
        l.add(d)
        val s1 = FastStack(l)

        assertTrue(s1.containsAll(l))
        assertTrue(l.containsAll(s1))
    }

    /**
     * Test that the equals function works correctly.
     */
    @Test
    fun testEquals() {
        val a = "a"
        val b = "b"
        val c = "c"
        val d = "d"
        val l = ArrayList<Any>()
        l.add(a)
        l.add(b)
        l.add(c)
        l.add(d)
        val s1 = FastStack(l)
        assertTrue(l == s1)
        assertTrue(s1 == l)
    }

    companion object {

        private val _STREAMBUFFERSIZE = 32 * 1024

        private var __StandAlone = false

        private val _DEBUG_LEVEL = 150

        /**
         * The suite method returning the test suite for a test runner.
         *
         * @return The test suite
         */
        @BeforeAll
        @Throws(IOException::class)
        fun beforeClass() {
            if (!__StandAlone) {
                DebugTool.setDebugLevel(_DEBUG_LEVEL)
            }
            val out: OutputStream
            try {
                out = BufferedOutputStream(FileOutputStream("testOutput.txt"), _STREAMBUFFERSIZE)
            } catch (e: IOException) {
                e.printStackTrace()
                throw e
            }

            DebugTool.setDebugStream(PrintStream(out))
        }

        @AfterAll
        fun afterClass() {
            DebugTool.getDebugStream().close()
            DebugTool.setDebugStream(System.err)
        }
    }

}
