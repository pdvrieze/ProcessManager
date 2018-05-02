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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.w3c.dom.Node
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type


/**
 * Created by pdvrieze on 18/04/16.
 */
class ReflectionUtilTest {

    private class TestTarget {
        fun function1(): List<String>? {
            return null
        }

        fun function2(): List<Collection<Node>>? {
            return null
        }
    }

    @Test
    @Throws(NoSuchMethodException::class)
    fun testIterableParam1() {
        val `in` = TestTarget::class.java.getMethod("function1").genericReturnType
        val iterableType = ReflectionUtil.concreteTypeParams(`in`, Iterable::class.java)
        assertEquals(arrayOf<Class<*>>(String::class.java), iterableType)
    }

    @Test
    @Throws(NoSuchMethodException::class)
    fun testIterableParam2() {
        val `in` = TestTarget::class.java.getMethod("function2").genericReturnType
        val iterableType = ReflectionUtil.concreteTypeParams(`in`, Iterable::class.java)
        assertEquals(arrayOf<Class<*>>(Collection::class.java), iterableType)
    }

    @Test
    @Throws(NoSuchMethodException::class)
    fun testIterableParam3() {
        val `in` = TestTarget::class.java.getMethod("function2").genericReturnType
        val iterableType = ReflectionUtil.typeParams(`in`, Iterable::class.java)
        assertTrue(iterableType.size == 1)
        assertTrue(iterableType[0] is ParameterizedType)
        val t = iterableType[0] as ParameterizedType

        assertEquals(Collection::class.java, t.rawType)
        assertEquals(arrayOf<Type>(Node::class.java), t.actualTypeArguments)
    }
}

