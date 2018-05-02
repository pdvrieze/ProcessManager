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

package net.devrieze.test.util

import net.devrieze.util.Types
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test


class TypesTest {


    val stringList: List<String>?
        get() = null

    internal interface A<T1, T2, T3> : Collection<T3>

    internal interface B<T4, T5, T6> : A<T4, Void, T5>

    internal interface C<T7> : B<String, Int, T7> {
        fun test(): C<Boolean>
    }

    @Test
    @Throws(NoSuchMethodException::class, SecurityException::class)
    fun testGetTypeParametersFor1() {
        val returnType = javaClass.getMethod("getStringList").genericReturnType

        val paramType = Types.getTypeParametersFor(Collection::class.java, returnType)

        assertEquals(1, paramType.size)
        assertTrue(paramType[0] is Class<*>)
        assertEquals(String::class.java, paramType[0])
    }

    @Test
    @Throws(NoSuchMethodException::class, SecurityException::class)
    fun testGetTypeParametersFor2() {
        val returnType = C::class.java.getMethod("test").genericReturnType

        run {
            val paramTypesA = Types.getTypeParametersFor(A::class.java, returnType)

            assertEquals(3, paramTypesA.size)
            assertEquals(String::class.java, paramTypesA[0])
            assertEquals(Void::class.java, paramTypesA[1])
            assertEquals(Int::class.javaObjectType, paramTypesA[2])
        }

        run {
            val paramTypesB = Types.getTypeParametersFor(B::class.java, returnType)
            assertEquals(3, paramTypesB.size)
            assertEquals(String::class.java, paramTypesB[0])
            assertEquals(Int::class.javaObjectType, paramTypesB[1])
            assertEquals(Boolean::class.javaObjectType, paramTypesB[2])
        }

        run {
            val paramTypesCol = Types.getTypeParametersFor(Collection::class.java, returnType)
            assertEquals(1, paramTypesCol.size)
            assertEquals(Int::class.javaObjectType, paramTypesCol[0])
        }
    }

}

