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

import net.devrieze.util.PrefixMap;
import net.devrieze.util.PrefixMap.Entry;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;


public class PrefixMapTest {

  static {
    PrefixMapTest.class.getClassLoader().setDefaultAssertionStatus(true);
  }

  private static final String EXPECTEDBALANCERESULT =
      "     [===== (7)\n" +
      "     |  [===== (7) value=\"d\"\n" +
      "     |  |  [===== (3) value=\"b\"\n" +
      "     |  |  |       [===== (1)\n" +
      "     |  |  | l=\"a\" ] value=\"a\"\n" +
      "     |  |  |       [=====\n" +
      "     |  |  \\----\\\n" +
      "     |  | l=\"b\" ]\n" +
      "     |  |  /----/\n" +
      "     |  |  |       [===== (1)\n" +
      "     |  |  | r=\"c\" ] value=\"c\"\n" +
      "     |  |  |       [=====\n" +
      "     |  |  [=====\n" +
      "     |  \\----\\\n" +
      "\"\"   ] b=\"d\" ]\n" +
      "     |  /----/\n" +
      "     |  |  [===== (3) value=\"f\"\n" +
      "     |  |  |       [===== (1)\n" +
      "     |  |  | l=\"e\" ] value=\"e\"\n" +
      "     |  |  |       [=====\n" +
      "     |  |  \\----\\\n" +
      "     |  | r=\"f\" ]\n" +
      "     |  |  /----/\n" +
      "     |  |  |       [===== (1)\n" +
      "     |  |  | r=\"g\" ] value=\"g\"\n" +
      "     |  |  |       [=====\n" +
      "     |  |  [=====\n" +
      "     |  [=====\n" +
      "     [=====";

  private static final String EXPECTEDMAPTESTSTRING =
      "     [===== (5)\n" +
      "     |  [===== (5)\n" +
      "     |  |         [===== (1)\n" +
      "     |  | l=\"aaa\" ] value=\"aaa\"\n" +
      "     |  |         [=====\n" +
      "     |  \\----\\\n" +
      "     |       |   [===== (3) value=\"bb\"\n" +
      "     |       |   |        [===== (1)\n" +
      "     |       |   | l=\"ba\" ] value=\"ba\"\n" +
      "     |       |   |        [=====\n" +
      "     |       |   \\----\\\n" +
      "     |       |        |         [===== (1)\n" +
      "\"\"   ] b=\"b\" ] b=\"bb\" ] b=\"bba\" ] value=\"bba\"\n" +
      "     |       |        |         [=====\n" +
      "     |       |        [=====\n" +
      "     |  /----/\n" +
      "     |  |         [===== (1)\n" +
      "     |  | r=\"ccc\" ] value=\"ccc\"\n" +
      "     |  |         [=====\n" +
      "     |  [=====\n" +
      "     [=====";

  private static final String OTHERSEXPECTED = "";

  private PrefixMap<String> mMap;

  private ArrayList<Entry<String>> mEntryList;

  private Object[] mExpected;

  private Comparator<Entry<String>> mEntryComparator;

  @BeforeMethod
  public void setUp() {
    mMap = new PrefixMap<>();
    mEntryList = new ArrayList<>();
    mEntryList.add(new Entry<>("bb", "bb"));
    mEntryList.add(new Entry<>("aaa", "aaa"));
    mEntryList.add(new Entry<>("ccc", "ccc"));
    mEntryList.add(new Entry<>("bba", "bba"));
    mEntryList.add(new Entry<>("ba", "ba"));

    final List<Entry<String>> expectedList = new ArrayList<>(mEntryList.size());
    expectedList.addAll(mEntryList);
    mEntryComparator = new Comparator<Entry<String>>() {

      @Override
      public int compare(final Entry<String> o1, final Entry<String> o2) {
        return o1.getPrefix().toString().compareTo(o2.getPrefix().toString());
      }

    };
    Collections.sort(expectedList, mEntryComparator);

    mExpected = expectedList.toArray();
  }

  @Test
  public void testPut() {
    for (final Entry<String> entry : mEntryList) {
      mMap.put(entry.getPrefix(), entry.getValue());
      assertTrue(mMap.contains(entry), "Contains " + entry.getPrefix());;
    }
    for (final Entry<String> entry : mEntryList) {
      assertTrue(mMap.contains(entry), "Contains " + entry.getPrefix());;
    }
    assertEquals(mMap.toTestString(), EXPECTEDMAPTESTSTRING);
  }

  @Test
  public void testToList() {
    mMap.addAll(mEntryList);
    assertEquals(mMap.toList().toArray(), mExpected);;
  }

  @Test
  public void testIterator() {
    mMap.addAll(mEntryList);
    assertEquals(new ArrayList<>(mMap).toArray(), mExpected);;
  }

  @Test
  public void testPrefix1() {
    mMap.addAll(mEntryList);
    final Collection<Entry<String>> result = mMap.getLonger("b");

    @SuppressWarnings("unchecked")
    final Entry<String>[] expected = new Entry[] { new Entry<>("ba", "ba"), new Entry<>("bb", "bb"),
                                                  new Entry<>("bba", "bba"), };

    assertEquals(result.toArray(), expected);;

  }

  @Test
  public void testPrefix2() {
    mMap.addAll(mEntryList);
    final Collection<Entry<String>> result = mMap.getLonger("cc");

    @SuppressWarnings("unchecked")
    final Entry<String>[] expected = new Entry[] { new Entry<>("ccc", "ccc"), };

    assertEquals(result.toArray(), expected);;

  }

  @Test
  public void testPrefix3() {
    mMap.addAll(mEntryList);
    final Collection<Entry<String>> result = mMap.getLonger("bbax");

    @SuppressWarnings("unchecked")
    final Entry<String>[] expected = new Entry[] {};

    assertEquals(result.toArray(), expected);;

  }

  @Test
  public void testSuffix() {
    mMap.addAll(mEntryList);
    final Collection<Entry<String>> result = mMap.getPrefixes("bbax");

    @SuppressWarnings("unchecked")
    final Entry<String>[] expected = new Entry[] { new Entry<>("bb", "bb"), new Entry<>("bba", "bba"), };

    assertEquals(result.toArray(), expected);;

  }

  @Test
  public void testPutOthers() {
    mMap.put("/processInstances/", "a");
    assertEquals(first(mMap.get("/processInstances/")), "a");

    mMap.put("/processModels", "b");
    assertEquals(first(mMap.get("/processInstances/")), "a");
    assertEquals(first(mMap.get("/processModels")), "b");

    mMap.put("/processModels/", "c");
    assertEquals(first(mMap.get("/processInstances/")), "a");
    assertEquals(first(mMap.get("/processModels")), "b");
    assertEquals(first(mMap.get("/processModels/")), "c");

    mMap.put("/processInstances", "d");
    assertEquals(first(mMap.get("/processInstances/")), "a");
    assertEquals(first(mMap.get("/processModels")), "b");
    assertEquals(first(mMap.get("/processModels/")), "c");
    assertEquals(first(mMap.get("/processInstances")), "d");

    mMap.put("/procvalues/", "e");
    assertEquals(first(mMap.get("/processInstances/")), "a");
    assertEquals(first(mMap.get("/processModels")), "b");
    assertEquals(first(mMap.get("/processModels/")), "c");
    assertEquals(first(mMap.get("/processInstances")), "d");
    assertEquals(first(mMap.get("/procvalues/")), "e");

    mMap.put("/procvalues", "f");
    assertEquals(first(mMap.get("/processInstances/")), "a");
    assertEquals(first(mMap.get("/processModels")), "b");
    assertEquals(first(mMap.get("/processModels/")), "c");
    assertEquals(first(mMap.get("/processInstances")), "d");
    assertEquals(first(mMap.get("/procvalues/")), "e");
    assertEquals(first(mMap.get("/procvalues")), "f");

    mMap.put("/", "g");
    assertEquals(first(mMap.get("/processInstances/")), "a");
    assertEquals(first(mMap.get("/processModels")), "b");
    assertEquals(first(mMap.get("/processModels/")), "c");
    assertEquals(first(mMap.get("/processInstances")), "d");
    assertEquals(first(mMap.get("/procvalues/")), "e");
    assertEquals(first(mMap.get("/procvalues")), "f");
    assertEquals(first(mMap.get("/")), "g");

    if (mMap.isEmpty()) { assertEquals(mMap.toTestString(), OTHERSEXPECTED); }
  }

  private static String first(Collection<Entry<String>> collection) {
    if (collection==null || collection.isEmpty()) { return null; }
    return collection.iterator().next().getValue();
  }

  @Test
  public void testRemove() {
    final Entry<String> falseEntry = new PrefixMap.Entry<>("Foo", "Bar");
    mMap.addAll(mEntryList);
    final int index = 0;
    while (mEntryList.size()>0) {
      assertFalse(mMap.remove(falseEntry));
      assertTrue(mMap.contains(mEntryList.get(index)));
      mMap.remove(mEntryList.get(index));
      assertFalse(mMap.contains(mEntryList.get(index)));
      mEntryList.remove(index);
      Collections.sort(mEntryList, mEntryComparator);
      assertEquals(mMap.toArray(), mEntryList.toArray());;
    }
  }

  @Test
  public void testRemoveRandom() {
    final Entry<String> falseEntry = new PrefixMap.Entry<>("Foo", "Bar");

    for(int i=20; i>0; --i) {
      ArrayList<Entry<String>> entryList = new ArrayList<>(mEntryList);
      Collections.shuffle(entryList);
      mMap.addAll(entryList);
      Collections.shuffle(entryList);
      final int index = 0;
      while (entryList.size()>0) {
        assertFalse(mMap.remove(falseEntry));
        assertTrue(mMap.contains(entryList.get(index)));
        mMap.remove(entryList.get(index));
        assertFalse(mMap.contains(entryList.get(index)));
        entryList.remove(index);
        Collections.sort(entryList, mEntryComparator);
        assertEquals(mMap.toArray(), entryList.toArray());;
      }
      assertTrue(mMap.isEmpty());
    }
  }

  @Test
  public void testBalance() {
    for(String s: new String[] {"a","b","c","d","e","f","g"}) {
      mMap.put(s, s);
    }
    assertEquals(mMap.toTestString(), EXPECTEDBALANCERESULT);
  }

  @Test
  public void testBalanceReverse() {
    for(String s: new String[] {"g","f","e","d","c","b","a"}) {
      mMap.put(s, s);
    }
    assertEquals(mMap.toTestString(), EXPECTEDBALANCERESULT);
  }

  @Test
  public void testBalanceShuffle() {
    for(String s: new String[] {"a","f","d","b","g","e","c"}) {
      mMap.put(s, s);
    }
    assertEquals(mMap.toTestString(), EXPECTEDBALANCERESULT);
  }

}
