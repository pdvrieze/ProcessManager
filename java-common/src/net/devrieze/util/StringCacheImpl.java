package net.devrieze.util;

import java.util.WeakHashMap;

import org.jetbrains.annotations.Nullable;


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
    public synchronized String lookup(final @Nullable String pString) {
      return super.lookup(pString);
    }

  }

  WeakHashMap<String, String> mCache = new WeakHashMap<>();

  /*
   * (non-Javadoc)
   * @see net.devrieze.util.StringCache#lookup(java.lang.String)
   */
  @Override
  public String lookup(@Nullable final String pString) {
    if (pString==null) { return null; }
    String result = mCache.get(pString);
    if (result != null) {
      return result;
    }

    // Use a narrow stringbuilder such as not to carry extra bytes into the cache.
    final StringBuilder builder = new StringBuilder(pString.length());
    builder.append(pString);
    builder.trimToSize();
    result = builder.toString();
    mCache.put(result, result);
    return result;
  }

}
