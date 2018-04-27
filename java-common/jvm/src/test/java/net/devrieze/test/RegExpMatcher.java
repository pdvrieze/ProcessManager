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
