package nl.adaptivity.util;


import java.util.Iterator;
import java.util.List;


public final class Util {

  public interface NameChecker {

    boolean isAvailable(String string);

  }

  private Util() { /* xx */ }

  public static boolean listEquals(final List<?> list1, final List<?> list2) {
    if (list1==null) { return list2==null; }
    if (list2==null) { return false; }
    if (list1.size()!=list2.size()) { return false; }
    Iterator<?> it1 = list1.iterator();
    Iterator<?> it2 = list2.iterator();

    while (it1.hasNext()) {
      final Object val1 = it1.next();
      final Object val2 = it2.next();
      if ((val1 == null) ? (val2 != null) : !val1.equals(val2)) { return false; }
    }
    return true;
  }

  /** Replacement for {@link java.util.Objects#equals} that works with lower jdk levels. */
  public static boolean equals(final Object val1, final Object val2) {
    return (val1 == null) ? (val2 == null) : val1.equals(val2);
  }

  public static String suggestNewName(CharSequence previousName) {
    return suggestNewName(previousName, null);
  }

  public static String suggestNewName(CharSequence previousName, NameChecker nameChecker) {
    int i=previousName.length()-1;
    while (Character.isDigit(previousName.charAt(i))) {
      --i;
    }
    String suggestedNewName;
    int suffix;
    CharSequence base;
    if ((i+1)<previousName.length()) {
      int prevNo = Integer.parseInt(previousName.subSequence(i+1, previousName.length()).toString());
      base = previousName.subSequence(0, i+1);
      suffix = prevNo+1;
    } else {
      base = previousName+" ";
      suffix = 2;
    }
    if (nameChecker!=null) {
      while (! nameChecker.isAvailable(suggestedNewName=base+Integer.toString(suffix))) {
        suffix++;
      }
    } else {
      suggestedNewName = base+Integer.toString(suffix);
    }
    return suggestedNewName;
  }

}
