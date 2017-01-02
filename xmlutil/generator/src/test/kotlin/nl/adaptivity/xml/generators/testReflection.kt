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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xml.generators

import nl.adaptivity.xml.XmlSerializable
import nl.adaptivity.xml.XmlWriter
import org.testng.Assert
import org.testng.annotations.Test
import java.lang.reflect.ParameterizedType
import java.lang.reflect.TypeVariable

/**
 * Created by pdvrieze on 19/04/16.
 */

class TestTarget<out W : XmlSerializable>(): XmlSerializable {
  override fun serialize(out: XmlWriter) {
    throw UnsupportedOperationException()
  }

  fun getBody():Iterable<W> { return emptyList()}

  fun myMap():Map<String, W> { return emptyMap() }

}

class TestReflection {

  @Test
  fun testGetTypeVars() {
    val result = getTypeVars(TestTarget::class.java.typeParameters.asList())

    Assert.assertEquals(result.size, 1)
    val expected = SimpleTypeVar("W", arrayOf(XmlSerializable::class.java))
    Assert.assertEquals(result.get(0), expected)
    Assert.assertEquals(result, listOf(expected))
  }

  @Test
  fun resolveType2() {
    val target = TestTarget::class.java
    val rt = target.getDeclaredMethod("getBody").genericReturnType

    val varLookup = { tv: TypeVariable<*> -> if (tv.genericDeclaration==target){ tv.name } else {null}}
    val resolved = resolveType(rt, varLookup, ::simpleNewType)
    Assert.assertEquals(resolved, "Iterable<W>")
  }

  @Test
  fun resolveType1() {
    val rt = TestTarget::class.java.getDeclaredMethod("getBody").genericReturnType

    val resolved = resolveType(rt, newType = ::simpleNewType)
    Assert.assertEquals(resolved, "Iterable<? extends XmlSerializable>")
  }

  @Test
  fun resolveTypeVarsDirect() {
    val rt = TestTarget::class.java.getDeclaredMethod("myMap").genericReturnType

    val resolved = resolveType(rt, newType = ::simpleNewType)
    Assert.assertEquals(resolved, "Map<String, ? extends XmlSerializable>")
  }

  @Test
  fun resolveTypeVarsInDirect() {
    val mrt = TestTarget::class.java.getDeclaredMethod("myMap").genericReturnType as ParameterizedType
    val paramValues = mrt.actualTypeArguments

    val resolved = resolveType(Map.Entry::class.java.withParams(paramValues[0], paramValues[1]), newType = ::simpleNewType)
    Assert.assertEquals(resolved, "Map.Entry<String, ? extends XmlSerializable>")
  }

  @Test
  fun resolveTypeVarsInDirect2() {
    val target = TestTarget::class.java
    val mrt = TestTarget::class.java.getDeclaredMethod("myMap").genericReturnType as ParameterizedType
    val paramValues = mrt.actualTypeArguments

    val varLookup = { tv: TypeVariable<*> ->
      if (tv.genericDeclaration==target){ tv.name } else {null}
    }

    val resolved = resolveType(Map.Entry::class.java.withParams(paramValues[0], paramValues[1]), varLookup, newType = ::simpleNewType)
    Assert.assertEquals(resolved, "Map.Entry<String, W>")
  }
}

fun simpleNewType(clazz:Class<*>):String {
  return clazz.simpleName
}