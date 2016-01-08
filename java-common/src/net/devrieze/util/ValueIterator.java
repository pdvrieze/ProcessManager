/**
 *
 */
package net.devrieze.util;

import java.util.Iterator;
import java.util.Map;


public class ValueIterator<T> implements Iterator<T> {

  private final Iterator<? extends Map.Entry<?, T>> mBackingIterator;

  public ValueIterator(final Iterator<? extends Map.Entry<?, T>> pBackingIterator) {
    mBackingIterator = pBackingIterator;
  }

  @Override
  public boolean hasNext() {
    return mBackingIterator.hasNext();
  }

  @Override
  public T next() {
    return mBackingIterator.next().getValue();
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("Read only collection");
  }

}