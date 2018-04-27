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

package net.devrieze.test;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;

import static org.junit.Assert.*;

import net.devrieze.util.DebugTool;
import net.devrieze.util.FastStack;


/**
 * @author Paul de Vrieze
 * @version 1.0 $Revision$
 */
public class FastStackTest {

  private static final int _STREAMBUFFERSIZE = 32 * 1024;

  /**
   * Test that creation of FastStacks is correct.
   */
  @Test
  public void testCreate() {
    final Object a = "a";
    final FastStack<Object> s1 = new FastStack<>(a);
    assertTrue(s1.getLast() == a);
  }

  /**
   * Test the append function.
   */
  @Test
  public void testAppend() {
    final Object a = "a";
    final Object b = "b";
    final FastStack<Object> s1 = new FastStack<>(a);
    final FastStack<Object> s2 = s1.append(b);
    assertTrue(s2.getLast() == b);
  }

  /**
   * Test that the contains function works correctly.
   */
  @Test
  public void testContains() {
    final Object a = "a";
    final Object b = "b";
    final Object c = "c";
    final FastStack<Object> s1 = new FastStack<>(a);
    final FastStack<Object> s2 = s1.append(b);
    assertTrue(s2.contains(a));
    assertTrue(s2.contains(b));
    assertTrue(!s2.contains(c));
  }

  /**
   * Test the containsAll function.
   */
  @Test
  public void testContainsAll() {
    final Object a = "a";
    final Object b = "b";
    final Object c = "c";
    final Object d = "d";
    final FastStack<Object> s1 = new FastStack<>(a);
    final FastStack<Object> s2 = s1.append(b).append(c);
    final List<Object> l = new ArrayList<>();
    l.add(a);
    l.add(b);

    assertTrue(s2.containsAll(l));
    l.add(d);
    assertTrue(!s2.containsAll(l));
  }

  /**
   * Thest the correctness of the size function.
   */
  @Test
  public void testSize() {
    final Object a = "a";
    final Object b = "b";
    final Object c = "c";
    final FastStack<Object> s1 = new FastStack<>(a);
    assertEquals(1, s1.size());
    final FastStack<Object> s2 = s1.append(b).append(c);
    assertEquals(3, s2.size());
  }

  /**
   * Test that creation based on an iterator works.
   */
  @Test
  public void testCreateIterator() {
    final Object a = "a";
    final Object b = "b";
    final Object c = "c";
    final Object d = "d";
    final List<Object> l = new ArrayList<>();
    l.add(a);
    l.add(b);
    l.add(c);
    l.add(d);
    final FastStack<Object> s1 = new FastStack<>(l);

    assertTrue(s1.containsAll(l));
    assertTrue(l.containsAll(s1));
  }

  /**
   * Test that the equals function works correctly.
   */
  @Test
  public void testEquals() {
    final Object a = "a";
    final Object b = "b";
    final Object c = "c";
    final Object d = "d";
    final List<Object> l = new ArrayList<>();
    l.add(a);
    l.add(b);
    l.add(c);
    l.add(d);
    final FastStack<Object> s1 = new FastStack<>(l);
    assertTrue(l.equals(s1));
    assertTrue(s1.equals(l));
  }

  private static boolean __StandAlone = false;

  private static final int _DEBUG_LEVEL = 150;

  /**
   * Main execution system.
   *
   * @param pArgs the arguments
   */
  public static void main(final String[] pArgs) {
    DebugTool.parseArgs(pArgs);
    JUnitCore core = new JUnitCore();
    __StandAlone = true;
    core.run(FastStackTest.class);
  }

  /**
   * The suite method returning the test suite for a test runner.
   *
   * @return The test suite
   */
  @SuppressWarnings("resource")
  @BeforeClass
  public static void beforeClass() throws IOException {
    if (!__StandAlone) {
      DebugTool.setDebugLevel(_DEBUG_LEVEL);
    }
    final OutputStream out;
    try {
      out = new BufferedOutputStream(new FileOutputStream("testOutput.txt"), _STREAMBUFFERSIZE);
    } catch (final IOException e) {
      e.printStackTrace();
      throw e;
    }
    DebugTool.setDebugStream(new PrintStream(out));
  }

  @AfterClass()
  public static void afterClass() {
    DebugTool.getDebugStream().close();
    DebugTool.setDebugStream(System.err);
  }

}
