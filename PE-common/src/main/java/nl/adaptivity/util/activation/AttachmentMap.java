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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.util.activation;

import nl.adaptivity.process.engine.NormalizedMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.activation.DataHandler;

import java.util.*;


public class AttachmentMap extends AbstractMap<String, DataHandler> {


  private class Entry implements Map.Entry<String, DataHandler> {

    private final String mKey;

    public Entry(final String key) {
      mKey = key;
    }

    @Override
    public String getKey() {
      return mKey;
    }

    @Override
    public DataHandler getValue() {
      return mMessage.getAttachment(mKey);
    }

    @Override
    public DataHandler setValue(final DataHandler value) {
      final DataHandler result = mMessage.getAttachment(mKey);
      mMessage.addAttachment(mKey, value);
      return result;
    }

  }

  private class EntryIterator implements Iterator<Map.Entry<String, DataHandler>> {

    @NotNull private final Iterator<String> mBackingIterator;

    public EntryIterator() {
      mBackingIterator = mMessage.getAttachmentNames().iterator();
    }

    @Override
    public boolean hasNext() {
      return mBackingIterator.hasNext();
    }

    @NotNull
    @Override
    public Entry next() {

      final String next = mBackingIterator.next();
      return new Entry(next);
    }

    @Override
    public void remove() {
      mBackingIterator.remove();
    }

  }

  private class EntrySet extends AbstractSet<Map.Entry<String, DataHandler>> {

    @Override
    public boolean remove(final Object o) {
      if (o instanceof Map.Entry) {
        final Map.Entry<?, ?> me = (Map.Entry<?, ?>) o;
        return AttachmentMap.this.remove(me.getKey()) != null;
      }
      return false;
    }

    private int mSize = -1;

    @NotNull
    @Override
    public Iterator<Map.Entry<String, DataHandler>> iterator() {
      return new EntryIterator();
    }

    @Override
    public int size() {
      if (mSize < 0) {
        mSize = mMessage.getAttachmentNames().size();
      }
      return mSize;
    }

    @Override
    public boolean removeAll(@NotNull final Collection<?> c) {
      boolean result = false;
      for (final Object o : c) {
        result |= AttachmentMap.this.remove(o) != null;
      }
      return result;
    }

  }

  private final NormalizedMessage mMessage;

  public AttachmentMap(final NormalizedMessage message) {
    mMessage = message;
  }

  @NotNull
  @Override
  public Set<java.util.Map.Entry<String, DataHandler>> entrySet() {
    return new EntrySet();
  }

  @Override
  public boolean containsKey(final Object key) {
    return keySet().contains(key);
  }

  @Nullable
  @Override
  public DataHandler get(final Object key) {
    if (key instanceof String) {
      return mMessage.getAttachment((String) key);
    }
    return null;
  }

  @NotNull
  @Override
  public Set<String> keySet() {
    return mMessage.getAttachmentNames();
  }

  @Nullable
  @Override
  public DataHandler remove(final Object key) {
    if (key instanceof String) {
      final DataHandler old = mMessage.getAttachment((String) key);
      mMessage.removeAttachment((String) key);
      return old;
    }
    return null;
  }

}
