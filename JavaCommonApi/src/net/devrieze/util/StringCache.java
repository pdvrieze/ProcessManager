package net.devrieze.util;

import net.devrieze.annotations.NotNull;
import net.devrieze.annotations.Nullable;



public interface StringCache {

  public enum UniqueCaches implements StringCache {
    NOP;

    @Override
    public String lookup(String pString) {
      return pString;
    }

  }

  @NotNull
  public static final StringCache NOPCACHE=UniqueCaches.NOP;

  /**
   * Look up a string in the cache for string reuse.
   * @param pString <code>null</code> parameters will always return null
   * @return
   */
  public String lookup(@Nullable String pString);

}