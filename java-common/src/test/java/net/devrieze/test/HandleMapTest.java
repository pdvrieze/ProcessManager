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

package net.devrieze.test;

import net.devrieze.util.Handle;
import net.devrieze.util.MemHandleMap;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Iterator;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;


public class HandleMapTest {

  private MemHandleMap<String> mMap;

  @BeforeMethod
  public void setUp() {
    mMap = new MemHandleMap<>(5);
  }

  @Test
  public void testPut1() {
    @SuppressWarnings("unused")
    final Handle<String> a = mMap.put("A");
    assertEquals(mMap.size(), 1);
    @SuppressWarnings("unused")
    final Handle<String> b = mMap.put("B");
    assertEquals(mMap.size(), 2);
  }

  @Test
  public void testPut2() {
    // This depends on the implementation
    final Handle<String> a = mMap.put("A");
    assertEquals(a.getHandleValue(), 0l);
    final Handle<String> b = mMap.put("B");
    assertEquals(b.getHandleValue(), 1l);
  }

  @Test
  public void testPut3() {
    @SuppressWarnings("unused")
    final Handle<String> a = mMap.put("A");
    //    assertEquals("A", mMap.get(a));
    final Handle<String> b = mMap.put("B");
    assertEquals(mMap.get(b), "B");
  }

  @Test
  public void testPutGet() {
    final Handle<String> a = mMap.put("A");
    final Handle<String> b = mMap.put("B");
    final Handle<String> c = mMap.put("C");
    final Handle<String> d = mMap.put("D");
    final Handle<String> e = mMap.put("E");
    final Handle<String> f = mMap.put("F");
    final Handle<String> g = mMap.put("G");
    final Handle<String> h = mMap.put("H");
    final Handle<String> i = mMap.put("I");
    final Handle<String> j = mMap.put("J");
    assertEquals(mMap.size(), 10);

    assertEquals(mMap.get(a), "A", "Result of getting");
    assertEquals(mMap.get(b), "B", "Result of getting");
    assertEquals(mMap.get(c), "C", "Result of getting");
    assertEquals(mMap.get(d), "D", "Result of getting");
    assertEquals(mMap.get(e), "E", "Result of getting");
    assertEquals(mMap.get(f), "F", "Result of getting");
    assertEquals(mMap.get(g), "G", "Result of getting");
    assertEquals(mMap.get(h), "H", "Result of getting");
    assertEquals(mMap.get(i), "I", "Result of getting");
    assertEquals(mMap.get(j), "J", "Result of getting");
  }

  @Test
  public void testRemove() {
    final Handle<String> a = mMap.put("A");
    @SuppressWarnings("unused")
    final Handle<String> b = mMap.put("B");
    @SuppressWarnings("unused")
    final Handle<String> c = mMap.put("C");
    @SuppressWarnings("unused")
    final Handle<String> d = mMap.put("D");
    @SuppressWarnings("unused")
    final Handle<String> e = mMap.put("E");
    mMap.remove(a);
    final Handle<String> a2 = mMap.put("A2");
    assertEquals(mMap.get(a2), "A2");
    assertEquals(a2.getHandleValue(), 5l);
  }

  @Test
  public void testRemove2() {
    final Handle<String> a = mMap.put("A");
    final Handle<String> b = mMap.put("B");
    final Handle<String> c = mMap.put("C");
    final Handle<String> d = mMap.put("D");
    final Handle<String> e = mMap.put("E");
    mMap.remove(c);
    mMap.remove(b);
    mMap.remove(a);
    final Handle<String> a2 = mMap.put("A2");
    final Handle<String> b2 = mMap.put("B2");
    final Handle<String> c2 = mMap.put("C2");
    assertEquals(mMap.get(a2), "A2");
    assertEquals(mMap.get(b2), "B2");
    assertEquals(mMap.get(c2), "C2");
    assertEquals(a2.getHandleValue(), 5l);
    assertEquals(b2.getHandleValue(), 6l);
    assertEquals(c2.getHandleValue(), 7l);
    final Handle<String> f = mMap.put("F");
    assertEquals(mMap.get(f), "F");
    assertEquals(mMap.get(d), "D");
    assertEquals(mMap.get(e), "E");
  }

  @Test
  public void testRemove3() {
    final Handle<String> a = mMap.put("A");
    final Handle<String> b = mMap.put("B");
    final Handle<String> c = mMap.put("C");
    final Handle<String> d = mMap.put("D");
    final Handle<String> e = mMap.put("E");
    mMap.remove(c);
    mMap.remove(b);
    final Handle<String> b2 = mMap.put("B2");
    final Handle<String> c2 = mMap.put("C2");
    mMap.remove(a);
    final Handle<String> a2 = mMap.put("A2");
    assertEquals(mMap.get(a2), "A2");
    assertEquals(mMap.get(b2), "B2");
    assertEquals(mMap.get(c2), "C2");
    assertEquals(a2.getHandleValue(), 5l);
    assertEquals(b2.getHandleValue(), 0x100000001l);
    assertEquals(c2.getHandleValue(), 0x100000002l);
    final Handle<String> f = mMap.put("F");
    assertEquals(mMap.get(f), "F");
    assertEquals(mMap.get(d), "D");
    assertEquals(mMap.get(e), "E");
  }

  @Test
  public void testRemoveAll() {
    final Handle<String> a = mMap.put("A");
    final Handle<String> b = mMap.put("B");
    final Handle<String> c = mMap.put("C");
    final Handle<String> d = mMap.put("D");
    final Handle<String> e = mMap.put("E");
    mMap.remove(c);
    mMap.remove(b);
    mMap.remove(a);
    mMap.remove(e);
    mMap.remove(d);
    assertEquals(mMap.size(), 0);
  }

  @Test
  public void testEmptyIterator() {
    final ArrayList<String> list = new ArrayList<>();
    for (final String s : mMap) {
      list.add(s);
    }
    assertEquals(list.size(), 0);
  }

  @Test
  public void testIterator() {
    final ArrayList<String> list = new ArrayList<>();
    mMap.put("A");
    mMap.put("B");
    mMap.put("C");
    mMap.put("D");
    mMap.put("E");
    for (final String s : mMap) {
      list.add(s);
    }
    assertEquals(list.size(), 5);
    assertEquals(list.get(0), "A", "Result of getting");
    assertEquals(list.get(1), "B", "Result of getting");
    assertEquals(list.get(2), "C", "Result of getting");
    assertEquals(list.get(3), "D", "Result of getting");
    assertEquals(list.get(4), "E", "Result of getting");
  }

  @Test
  public void testIterator2() {
    final Handle<String> a = mMap.put("A");
    mMap.put("B");
    mMap.put("C");
    mMap.remove(a);
    final Iterator<String> it = mMap.iterator();

    assertEquals(mMap.size(), 2);


    assertEquals(it.next(), "B", "Result of getting");
    assertEquals(it.next(), "C", "Result of getting");
    assertFalse(it.hasNext(), "HasNext should not offer more elements");
  }

}
