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

package net.devrieze.util;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.w3c.dom.Node;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;


/**
 * Created by pdvrieze on 18/04/16.
 */
public class ReflectionUtilTest {

  private static class TestTarget {
    public List<String> function1() {return null;}

    public List<Collection<Node>> function2() { return null; }
  }

  @Test
  public void testIterableParam1() throws NoSuchMethodException {
    Type in = TestTarget.class.getMethod("function1").getGenericReturnType();
    Class<?>[] iterableType = ReflectionUtil.concreteTypeParams(in, Iterable.class);
    Assert.assertEquals(iterableType, new Class<?>[] {String.class});
  }

  @Test
  public void testIterableParam2() throws NoSuchMethodException {
    Type in = TestTarget.class.getMethod("function2").getGenericReturnType();
    Class<?>[] iterableType = ReflectionUtil.concreteTypeParams(in, Iterable.class);
    Assert.assertEquals(iterableType, new Class<?>[] {Collection.class});
  }

  @Test
  public void testIterableParam3() throws NoSuchMethodException {
    Type in = TestTarget.class.getMethod("function2").getGenericReturnType();
    Type[] iterableType = ReflectionUtil.typeParams(in, Iterable.class);
    Assert.assertTrue(iterableType.length==1);
    Assert.assertTrue(iterableType[0] instanceof ParameterizedType);
    ParameterizedType t = (ParameterizedType) iterableType[0];

    Assert.assertEquals(t.getRawType(), Collection.class);
    Assert.assertEquals(t.getActualTypeArguments(), new Type[] {Node.class});
  }
}
