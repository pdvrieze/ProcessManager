package net.devrieze.test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RegExpMatcher {

  final Pattern mPattern;

  public RegExpMatcher(final String pattern) {
    mPattern = Pattern.compile(pattern);
  }

  public boolean matches(final Object item) {
    if ((item == null) || (!(item instanceof String))) {
      return false;
    }
    final String other = (String) item;
    final Matcher m = mPattern.matcher(other);
    return m.matches();
  }

}
