package nl.adaptivity.util;


public final class Util {
  private Util() { /* xx */ }

  public static String suggestNewName(CharSequence previousName) {
    int i=previousName.length()-1;
    while (Character.isDigit(previousName.charAt(i))) {
      --i;
    }
    String suggestedNewName;
    if ((i+1)<previousName.length()) {
      int prevNo = Integer.parseInt(previousName.subSequence(i+1, previousName.length()).toString());
      suggestedNewName = previousName.subSequence(0, i+1)+Integer.toString(prevNo+1);
    } else {
      suggestedNewName = previousName + " 2";
    }
    return suggestedNewName;
  }

}
