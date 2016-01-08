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

package net.devrieze.util;

public interface StringCache {

  public enum UniqueCaches implements StringCache {
    NOP;

    @Override
    public String lookup(String string) {
      return string;
    }

  }

  public static final StringCache NOPCACHE=UniqueCaches.NOP;

  /**
   * Look up a string in the cache for string reuse.
   * @param string <code>null</code> parameters will always return null
   * @return
   */
  public String lookup(String string);

}