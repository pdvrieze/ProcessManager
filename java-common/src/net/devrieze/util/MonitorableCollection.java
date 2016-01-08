package net.devrieze.util;

import java.util.Collection;


public interface MonitorableCollection<V> extends Collection<V> {

  public void addCollectionChangeListener(CollectionChangeListener<? super V> listener);

  public void removeCollectionChangeListener(CollectionChangeListener<? super V> listener);

}
