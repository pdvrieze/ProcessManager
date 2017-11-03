/*
 * Copyright (c) 2017.
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

package nl.adaptivity.util;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Iterator;
import java.util.List;


public final class Util {

  public interface NameChecker {

    boolean isAvailable(String string);

  }

  private Util() { /* xx */ }

  public static boolean listEquals(@Nullable final List<?> list1, @Nullable final List<?> list2) {
    if (list1==null) { return list2==null; }
    if (list2==null) { return false; }
    if (list1.size()!=list2.size()) { return false; }
    final Iterator<?> it1 = list1.iterator();
    final Iterator<?> it2 = list2.iterator();

    while (it1.hasNext()) {
      final Object val1 = it1.next();
      final Object val2 = it2.next();
      if ((val1 == null) ? (val2 != null) : !val1.equals(val2)) { return false; }
    }
    return true;
  }

  /** Replacement for {@link java.util.Objects#equals} that works with lower jdk levels. */
  public static boolean equals(@Nullable final Object val1, @Nullable final Object val2) {
    return (val1 == null) ? (val2 == null) : val1.equals(val2);
  }

  public static String suggestNewName(@NonNull final CharSequence previousName) {
    return suggestNewName(previousName, null);
  }

  public static String suggestNewName(final CharSequence previousName, @Nullable final NameChecker nameChecker) {
    int i=previousName.length()-1;
    while (Character.isDigit(previousName.charAt(i))) {
      --i;
    }
    String             suggestedNewName;
    int                suffix;
    final CharSequence base;
    if ((i+1)<previousName.length()) {
      final int prevNo = Integer.parseInt(previousName.subSequence(i + 1, previousName.length()).toString());
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
