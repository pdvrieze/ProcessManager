package nl.adaptivity.util.activation;

import java.util.*;

import javax.activation.DataHandler;

import nl.adaptivity.process.engine.NormalizedMessage;


public class AttachmentMap extends AbstractMap<String, DataHandler> {


  private class Entry implements Map.Entry<String, DataHandler> {

    private final String aKey;

    public Entry(final String pKey) {
      aKey = pKey;
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
    public DataHandler setValue(final DataHandler pValue) {
      final DataHandler result = aMessage.getAttachment(aKey);
      aMessage.addAttachment(aKey, pValue);
      return result;
    }

  }

  private class EntryIterator implements Iterator<Map.Entry<String, DataHandler>> {

    private final Iterator<String> aBackingIterator;

    public EntryIterator() {
      aBackingIterator = aMessage.getAttachmentNames().iterator();
    }

    @Override
    public boolean hasNext() {
      return aBackingIterator.hasNext();
    }

    @Override
    public Entry next() {

      final String next = aBackingIterator.next();
      final Entry result = new Entry(next);
      return result;
    }

    @Override
    public void remove() {
      aBackingIterator.remove();
    }

  }

  private class EntrySet extends AbstractSet<Map.Entry<String, DataHandler>> {

    @Override
    public boolean remove(final Object pO) {
      if (pO instanceof Map.Entry) {
        final Map.Entry<?, ?> me = (Map.Entry<?, ?>) pO;
        return AttachmentMap.this.remove(me.getKey()) != null;
      }
      return false;
    }

    private int aSize = -1;

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
    public boolean removeAll(final Collection<?> pC) {
      boolean result = false;
      for (final Object o : pC) {
        result |= AttachmentMap.this.remove(o) != null;
      }
      return result;
    }

  }

  private final NormalizedMessage aMessage;

  public AttachmentMap(final NormalizedMessage pMessage) {
    aMessage = pMessage;
  }

  @Override
  public Set<java.util.Map.Entry<String, DataHandler>> entrySet() {
    return new EntrySet();
  }

  @Override
  public boolean containsKey(final Object pKey) {
    return keySet().contains(pKey);
  }

  @Override
  public DataHandler get(final Object pKey) {
    if (pKey instanceof String) {
      return aMessage.getAttachment((String) pKey);
    }
    return null;
  }

  @Override
  public Set<String> keySet() {
    return aMessage.getAttachmentNames();
  }

  @Override
  public DataHandler remove(final Object pKey) {
    if (pKey instanceof String) {
      final DataHandler old = aMessage.getAttachment((String) pKey);
      aMessage.removeAttachment((String) pKey);
      return old;
    }
    return null;
  }

}
