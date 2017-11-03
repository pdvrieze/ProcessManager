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

package net.devrieze.util;

import org.jetbrains.annotations.Nullable;

import java.util.WeakHashMap;


/**
 * A cache that will help with replacing strings with a canonical version.
 * 
 * @author Paul de Vrieze
 */
public class StringCacheImpl implements StringCache {

  /**
   *
   * @author Paul de Vrieze
   * @deprecated The purpose of this is not clear
   */
  @Deprecated
  public static class SafeStringCache extends StringCacheImpl {

    @Override
    public synchronized String lookup(@Nullable final String pString) {
      return super.lookup(pString);
    }

  }

  final WeakHashMap<String, String> mCache = new WeakHashMap<>();

  /*
   * (non-Javadoc)
   * @see net.devrieze.util.StringCache#lookup(java.lang.String)
   */
  @Override
  public String lookup(@Nullable final String pString) {
    if (pString==null) { return null; }
    {
      final String result = mCache.get(pString);
      if (result != null) {
        return result;
      }
    }

    // Use a narrow stringbuilder such as not to carry extra bytes into the cache.
    final StringBuilder builder = new StringBuilder(pString.length());
    builder.append(pString);
    builder.trimToSize();

    final String result = builder.toString();
    mCache.put(result, result);
    return result;
  }

}
