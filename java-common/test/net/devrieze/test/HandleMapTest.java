package net.devrieze.test;

import java.util.ArrayList;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import net.devrieze.util.MemHandleMap;


public class HandleMapTest {

  private MemHandleMap<String> mMap;

  @Before
  public void setUp() {
    mMap = new MemHandleMap<>(5);
  }

  @Test
  public void testPut1() {
    @SuppressWarnings("unused")
    final long a = mMap.put("A");
    assertEquals(1, mMap.size());
    @SuppressWarnings("unused")
    final long b = mMap.put("B");
    assertEquals(2, mMap.size());
  }

  @Test
  public void testPut2() {
    // This depends on the implementation
    final long a = mMap.put("A");
    assertEquals(0l, a);
    final long b = mMap.put("B");
    assertEquals(1l, b);
  }

  @Test
  public void testPut3() {
    @SuppressWarnings("unused")
    final long a = mMap.put("A");
    //    assertEquals("A", mMap.get(a));
    final long b = mMap.put("B");
    assertEquals("B", mMap.get(b));
  }

  @Test
  public void testPutGet() {
    final long a = mMap.put("A");
    final long b = mMap.put("B");
    final long c = mMap.put("C");
    final long d = mMap.put("D");
    final long e = mMap.put("E");
    final long f = mMap.put("F");
    final long g = mMap.put("G");
    final long h = mMap.put("H");
    final long i = mMap.put("I");
    final long j = mMap.put("J");
    assertEquals(10, mMap.size());

    assertEquals("Result of getting", "A", mMap.get(a));
    assertEquals("Result of getting", "B", mMap.get(b));
    assertEquals("Result of getting", "C", mMap.get(c));
    assertEquals("Result of getting", "D", mMap.get(d));
    assertEquals("Result of getting", "E", mMap.get(e));
    assertEquals("Result of getting", "F", mMap.get(f));
    assertEquals("Result of getting", "G", mMap.get(g));
    assertEquals("Result of getting", "H", mMap.get(h));
    assertEquals("Result of getting", "I", mMap.get(i));
    assertEquals("Result of getting", "J", mMap.get(j));
  }

  @Test
  public void testRemove() {
    final long a = mMap.put("A");
    @SuppressWarnings("unused")
    final long b = mMap.put("B");
    @SuppressWarnings("unused")
    final long c = mMap.put("C");
    @SuppressWarnings("unused")
    final long d = mMap.put("D");
    @SuppressWarnings("unused")
    final long e = mMap.put("E");
    mMap.remove(a);
    final long a2 = mMap.put("A2");
    assertEquals("A2", mMap.get(a2));
    assertEquals(5l, a2);
  }

  @Test
  public void testRemove2() {
    final long a = mMap.put("A");
    final long b = mMap.put("B");
    final long c = mMap.put("C");
    final long d = mMap.put("D");
    final long e = mMap.put("E");
    mMap.remove(c);
    mMap.remove(b);
    mMap.remove(a);
    final long a2 = mMap.put("A2");
    final long b2 = mMap.put("B2");
    final long c2 = mMap.put("C2");
    assertEquals("A2", mMap.get(a2));
    assertEquals("B2", mMap.get(b2));
    assertEquals("C2", mMap.get(c2));
    assertEquals(5l, a2);
    assertEquals(6l, b2);
    assertEquals(7l, c2);
    final long f = mMap.put("F");
    assertEquals("F", mMap.get(f));
    assertEquals("D", mMap.get(d));
    assertEquals("E", mMap.get(e));
  }

  @Test
  public void testRemove3() {
    final long a = mMap.put("A");
    final long b = mMap.put("B");
    final long c = mMap.put("C");
    final long d = mMap.put("D");
    final long e = mMap.put("E");
    mMap.remove(c);
    mMap.remove(b);
    final long b2 = mMap.put("B2");
    final long c2 = mMap.put("C2");
    mMap.remove(a);
    final long a2 = mMap.put("A2");
    assertEquals("A2", mMap.get(a2));
    assertEquals("B2", mMap.get(b2));
    assertEquals("C2", mMap.get(c2));
    assertEquals(5l, a2);
    assertEquals(0x100000001l, b2);
    assertEquals(0x100000002l, c2);
    final long f = mMap.put("F");
    assertEquals("F", mMap.get(f));
    assertEquals("D", mMap.get(d));
    assertEquals("E", mMap.get(e));
  }

  @Test
  public void testRemoveAll() {
    final long a = mMap.put("A");
    final long b = mMap.put("B");
    final long c = mMap.put("C");
    final long d = mMap.put("D");
    final long e = mMap.put("E");
    mMap.remove(c);
    mMap.remove(b);
    mMap.remove(a);
    mMap.remove(e);
    mMap.remove(d);
    assertEquals(0, mMap.size());
  }

  @Test
  public void testEmptyIterator() {
    final ArrayList<String> list = new ArrayList<>();
    for (final String s : mMap) {
      list.add(s);
    }
    assertEquals(0, list.size());
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
    assertEquals(5, list.size());
    assertEquals("Result of getting", "A", list.get(0));
    assertEquals("Result of getting", "B", list.get(1));
    assertEquals("Result of getting", "C", list.get(2));
    assertEquals("Result of getting", "D", list.get(3));
    assertEquals("Result of getting", "E", list.get(4));
  }

  @Test
  public void testIterator2() {
    final long a = mMap.put("A");
    mMap.put("B");
    mMap.put("C");
    mMap.remove(a);
    final Iterator<String> it = mMap.iterator();

    assertEquals(2, mMap.size());


    assertEquals("Result of getting", "B", it.next());
    assertEquals("Result of getting", "C", it.next());
    assertFalse("HasNext should not offer more elements", it.hasNext());
  }

}
