package nl.adaptivity.util;


public final class Util {

  public interface NameChecker {

    boolean isAvailable(String string);

  }

  private Util() { /* xx */ }

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
