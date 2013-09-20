package net.devrieze.util;

import net.devrieze.annotations.Nullable;



public interface StringCache {

  /**
   * Look up a string in the cache for string reuse.
   * @param pString <code>null</code> parameters will always return null
   * @return
   */
  public String lookup(@Nullable String pString);

}