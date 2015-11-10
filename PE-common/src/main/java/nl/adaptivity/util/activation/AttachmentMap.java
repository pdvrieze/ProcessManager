package nl.adaptivity.util.activation;

import nl.adaptivity.process.engine.NormalizedMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.activation.DataHandler;

import java.util.*;


public class AttachmentMap extends AbstractMap<String, DataHandler> {


  private class Entry implements Map.Entry<String, DataHandler> {

    private final String aKey;

    public Entry(final String key) {
      aKey = key;
    }

    @Override
    public String getKey() {
      return aKey;
    }

    @Override
    public DataHandler getValue() {
      return aMessage.getAttachment(aKey);
    }

    @Override
    public DataHandler setValue(final DataHandler value) {
      final DataHandler result = aMessage.getAttachment(aKey);
      aMessage.addAttachment(aKey, value);
      return result;
    }

  }

  private class EntryIterator implements Iterator<Map.Entry<String, DataHandler>> {

    @NotNull private final Iterator<String> aBackingIterator;

    public EntryIterator() {
      aBackingIterator = aMessage.getAttachmentNames().iterator();
    }

    @Override
    public boolean hasNext() {
      return aBackingIterator.hasNext();
    }

    @NotNull
    @Override
    public Entry next() {

      final String next = aBackingIterator.next();
      return new Entry(next);
    }

    @Override
    public void remove() {
      aBackingIterator.remove();
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

    private int aSize = -1;

    @NotNull
    @Override
    public Iterator<Map.Entry<String, DataHandler>> iterator() {
      return new EntryIterator();
    }

    @Override
    public int size() {
      if (aSize < 0) {
        aSize = aMessage.getAttachmentNames().size();
      }
      return aSize;
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

  private final NormalizedMessage aMessage;

  public AttachmentMap(final NormalizedMessage message) {
    aMessage = message;
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
      return aMessage.getAttachment((String) key);
    }
    return null;
  }

  @NotNull
  @Override
  public Set<String> keySet() {
    return aMessage.getAttachmentNames();
  }

  @Nullable
  @Override
  public DataHandler remove(final Object key) {
    if (key instanceof String) {
      final DataHandler old = aMessage.getAttachment((String) key);
      aMessage.removeAttachment((String) key);
      return old;
    }
    return null;
  }

}
