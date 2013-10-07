package net.devrieze.util.db;

import net.devrieze.annotations.NotNull;
import net.devrieze.util.AutoCloseableIterator;


public interface DBIterable<T> extends AutoCloseable {

  @NotNull
  Iterable<T> all();

  @NotNull
  AutoCloseableIterator<T> iterator();
}
