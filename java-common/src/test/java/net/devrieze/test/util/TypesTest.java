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

package net.devrieze.test.util;

import net.devrieze.util.Types;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

import static org.testng.Assert.assertEquals;


public class TypesTest {

  interface A <T1, T2, T3> extends Collection<T3> {

  }

  interface B <T4, T5, T6> extends A<T4, Void, T5> {

  }

  interface C <T7> extends B<String, Integer, T7> {
    C<Boolean> test();
  }



  public List<String> getStringList() { return null; }

  @Test
  public void testGetTypeParametersFor1() throws NoSuchMethodException, SecurityException {
    Type returnType = getClass().getMethod("getStringList").getGenericReturnType();

    Type[] paramType = Types.getTypeParametersFor(Collection.class, returnType);

    assertEquals(paramType.length, 1);
    Assert.assertTrue(paramType[0] instanceof Class);;
    assertEquals(paramType[0], String.class);
  }

  @Test
  public void testGetTypeParametersFor2() throws NoSuchMethodException, SecurityException {
    Type returnType = C.class.getMethod("test").getGenericReturnType();

    {
      Type[] paramTypesA = Types.getTypeParametersFor(A.class, returnType);

      assertEquals(paramTypesA.length, 3);
      assertEquals(paramTypesA[0], String.class);
      assertEquals(paramTypesA[1], Void.class);
      assertEquals(paramTypesA[2], Integer.class);
    }

    {
      Type[] paramTypesB = Types.getTypeParametersFor(B.class, returnType);
      assertEquals(paramTypesB.length, 3);
      assertEquals(paramTypesB[0], String.class);
      assertEquals(paramTypesB[1], Integer.class);
      assertEquals(paramTypesB[2], Boolean.class);
    }

    {
      Type[] paramTypesCol = Types.getTypeParametersFor(Collection.class, returnType);
      assertEquals(paramTypesCol.length, 1);
      assertEquals(paramTypesCol[0], Integer.class);
    }
  }

}
