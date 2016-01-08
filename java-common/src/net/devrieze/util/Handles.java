package net.devrieze.util;

import net.devrieze.util.HandleMap.ComparableHandle;


public final class Handles {

  private static final class SimpleHandle<T> implements HandleMap.ComparableHandle<T> {

    private final long mHandle;

    private SimpleHandle(long pHandle) {
      mHandle = pHandle;
    }

    @Override
    public long getHandle() {
      return mHandle;
    }

    @Override
    public String toString() {
      return "H:"+mHandle;
    }

    @Override
    public int compareTo(ComparableHandle<T> pO) {
      return Long.compare(getHandle(), pO.getHandle());
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (mHandle ^ (mHandle >>> 32));
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      SimpleHandle<?> other = (SimpleHandle<?>) obj;
      if (mHandle != other.mHandle)
        return false;
      return true;
    }

  }

  private Handles() { /* Utility class */ }

  /**
   * Get a very simple Handle implementation.
   *
   * @param pHandle The handle
   * @return a Handle<T> object corresponding to the handle.
   */
  public static <T> HandleMap.ComparableHandle<T> handle(final long pHandle) {
    return new SimpleHandle<>(pHandle);
  }

  public static <T> HandleMap.ComparableHandle<T> handle(final HandleMap.Handle<T> pHandle) {
    if (pHandle instanceof ComparableHandle) { return (ComparableHandle<T>) pHandle; }
    return new SimpleHandle<>(pHandle.getHandle());
  }

}
