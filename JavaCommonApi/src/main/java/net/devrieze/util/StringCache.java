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