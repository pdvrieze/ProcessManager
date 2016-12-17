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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;


public final class Handles {

  private static final class SimpleHandle<T> implements ComparableHandle<T> {

    private final long mHandle;

    private SimpleHandle(long pHandle) {
      mHandle = pHandle;
    }

    @Override
    public boolean getValid() {
      return mHandle>=0L;
    }

    @Override
    public long getHandleValue() {
      return mHandle;
    }

    @Override
    public String toString() {
      return "H:"+mHandle;
    }

    @Override
    public int compareTo(@NotNull ComparableHandle<?> o) {
      return Long.compare(getHandleValue(), o.getHandleValue());
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

  private static final ComparableHandle<?> INVALID = new SimpleHandle<>(-1L);

  @SuppressWarnings("unchecked")
  @NotNull public static <T> ComparableHandle<T> getInvalid() {
    return (ComparableHandle<T>) INVALID;
  };

  private Handles() { /* Utility class */ }

  /**
   * Get a very simple Handle implementation.
   *
   * @param pHandle The handle
   * @return a Handle<T> object corresponding to the handle.
   */
  public static @NotNull <T> ComparableHandle<T> handle(final long pHandle) {
    return pHandle<0 ? Handles.<T>getInvalid() : new SimpleHandle<T>(pHandle);
  }

  public static <T> ComparableHandle<T> handle(final Handle<? extends T> pHandle) {
    if (pHandle instanceof ComparableHandle) { //noinspection unchecked
      return (ComparableHandle<T>) pHandle; }
    return new SimpleHandle<>(pHandle.getHandleValue());
  }

  /**
   * Convenience method that will parse the handle from a string
   * @param handle The string for the handle
   * @param <T>
   * @return
   */
  public static <T> ComparableHandle<T> handle(final String handle) {
    return handle(Long.parseLong(handle));
  }

  public static <T> ComparableHandle<T> handle(final URI handle) {
    String path = handle.getPath();
    int slashPos = path.lastIndexOf('/');
    if (slashPos>0) {
      return handle(path.substring(slashPos+1));
    } else {
      return handle(path);
    }
  }

}
