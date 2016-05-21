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

import static org.junit.Assert.*;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

import net.devrieze.util.Types;

import org.junit.Test;


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

    assertEquals(1, paramType.length);
    assertTrue(paramType[0] instanceof Class);
    assertEquals(String.class, paramType[0]);
  }

  @Test
  public void testGetTypeParametersFor2() throws NoSuchMethodException, SecurityException {
    Type returnType = C.class.getMethod("test").getGenericReturnType();

    {
      Type[] paramTypesA = Types.getTypeParametersFor(A.class, returnType);

      assertEquals(3, paramTypesA.length);
      assertEquals(String.class, paramTypesA[0]);
      assertEquals(Void.class, paramTypesA[1]);
      assertEquals(Integer.class, paramTypesA[2]);
    }

    {
      Type[] paramTypesB = Types.getTypeParametersFor(B.class, returnType);
      assertEquals(3, paramTypesB.length);
      assertEquals(String.class, paramTypesB[0]);
      assertEquals(Integer.class, paramTypesB[1]);
      assertEquals(Boolean.class, paramTypesB[2]);
    }

    {
      Type[] paramTypesCol = Types.getTypeParametersFor(Collection.class, returnType);
      assertEquals(1, paramTypesCol.length);
      assertEquals(Integer.class, paramTypesCol[0]);
    }
  }

}
