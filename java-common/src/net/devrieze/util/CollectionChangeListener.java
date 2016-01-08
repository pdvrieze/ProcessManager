package net.devrieze.util;


public interface CollectionChangeListener<V> {

  public void elementAdded(V elem);

  public void elementRemoved(V elem);

  public void collectionCleared();

}
