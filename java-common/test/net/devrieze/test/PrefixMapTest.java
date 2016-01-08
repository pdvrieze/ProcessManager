package net.devrieze.test;

import java.util.*;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import net.devrieze.util.PrefixMap;
import net.devrieze.util.PrefixMap.Entry;


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

  @Before
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
      assertTrue("Contains " + entry.getPrefix(), mMap.contains(entry));
    }
    for (final Entry<String> entry : mEntryList) {
      assertTrue("Contains " + entry.getPrefix(), mMap.contains(entry));
    }
    assertEquals(EXPECTEDMAPTESTSTRING, mMap.toTestString());
  }

  @Test
  public void testToList() {
    mMap.addAll(mEntryList);
    assertArrayEquals(mExpected, mMap.toList().toArray());
  }

  @Test
  public void testIterator() {
    mMap.addAll(mEntryList);
    assertArrayEquals(mExpected, new ArrayList<>(mMap).toArray());
  }

  @Test
  public void testPrefix1() {
    mMap.addAll(mEntryList);
    final Collection<Entry<String>> result = mMap.getLonger("b");

    @SuppressWarnings("unchecked")
    final Entry<String>[] expected = new Entry[] { new Entry<>("ba", "ba"), new Entry<>("bb", "bb"),
                                                  new Entry<>("bba", "bba"), };

    assertArrayEquals(expected, result.toArray());

  }

  @Test
  public void testPrefix2() {
    mMap.addAll(mEntryList);
    final Collection<Entry<String>> result = mMap.getLonger("cc");

    @SuppressWarnings("unchecked")
    final Entry<String>[] expected = new Entry[] { new Entry<>("ccc", "ccc"), };

    assertArrayEquals(expected, result.toArray());

  }

  @Test
  public void testPrefix3() {
    mMap.addAll(mEntryList);
    final Collection<Entry<String>> result = mMap.getLonger("bbax");

    @SuppressWarnings("unchecked")
    final Entry<String>[] expected = new Entry[] {};

    assertArrayEquals(expected, result.toArray());

  }

  @Test
  public void testSuffix() {
    mMap.addAll(mEntryList);
    final Collection<Entry<String>> result = mMap.getPrefixes("bbax");

    @SuppressWarnings("unchecked")
    final Entry<String>[] expected = new Entry[] { new Entry<>("bb", "bb"), new Entry<>("bba", "bba"), };

    assertArrayEquals(expected, result.toArray());

  }

  @Test
  public void testPutOthers() {
    mMap.put("/processInstances/", "a");
    assertEquals("a",first(mMap.get("/processInstances/")));

    mMap.put("/processModels", "b");
    assertEquals("a",first(mMap.get("/processInstances/")));
    assertEquals("b",first(mMap.get("/processModels")));

    mMap.put("/processModels/", "c");
    assertEquals("a",first(mMap.get("/processInstances/")));
    assertEquals("b",first(mMap.get("/processModels")));
    assertEquals("c",first(mMap.get("/processModels/")));

    mMap.put("/processInstances", "d");
    assertEquals("a",first(mMap.get("/processInstances/")));
    assertEquals("b",first(mMap.get("/processModels")));
    assertEquals("c",first(mMap.get("/processModels/")));
    assertEquals("d",first(mMap.get("/processInstances")));

    mMap.put("/procvalues/", "e");
    assertEquals("a",first(mMap.get("/processInstances/")));
    assertEquals("b",first(mMap.get("/processModels")));
    assertEquals("c",first(mMap.get("/processModels/")));
    assertEquals("d",first(mMap.get("/processInstances")));
    assertEquals("e",first(mMap.get("/procvalues/")));

    mMap.put("/procvalues", "f");
    assertEquals("a",first(mMap.get("/processInstances/")));
    assertEquals("b",first(mMap.get("/processModels")));
    assertEquals("c",first(mMap.get("/processModels/")));
    assertEquals("d",first(mMap.get("/processInstances")));
    assertEquals("e",first(mMap.get("/procvalues/")));
    assertEquals("f",first(mMap.get("/procvalues")));

    mMap.put("/", "g");
    assertEquals("a",first(mMap.get("/processInstances/")));
    assertEquals("b",first(mMap.get("/processModels")));
    assertEquals("c",first(mMap.get("/processModels/")));
    assertEquals("d",first(mMap.get("/processInstances")));
    assertEquals("e",first(mMap.get("/procvalues/")));
    assertEquals("f",first(mMap.get("/procvalues")));
    assertEquals("g",first(mMap.get("/")));

    if (mMap.isEmpty()) { assertEquals(OTHERSEXPECTED, mMap.toTestString()); }
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
      assertArrayEquals(mEntryList.toArray(), mMap.toArray());
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
        assertArrayEquals(entryList.toArray(), mMap.toArray());
      }
      assertTrue(mMap.isEmpty());
    }
  }

  @Test
  public void testBalance() {
    for(String s: new String[] {"a","b","c","d","e","f","g"}) {
      mMap.put(s, s);
    }
    assertEquals(EXPECTEDBALANCERESULT,mMap.toTestString());
  }

  @Test
  public void testBalanceReverse() {
    for(String s: new String[] {"g","f","e","d","c","b","a"}) {
      mMap.put(s, s);
    }
    assertEquals(EXPECTEDBALANCERESULT,mMap.toTestString());
  }

  @Test
  public void testBalanceShuffle() {
    for(String s: new String[] {"a","f","d","b","g","e","c"}) {
      mMap.put(s, s);
    }
    assertEquals(EXPECTEDBALANCERESULT,mMap.toTestString());
  }

}
