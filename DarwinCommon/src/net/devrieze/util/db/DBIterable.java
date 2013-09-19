package net.devrieze.util.db;

import java.io.Closeable;
import java.util.Iterator;

import net.devrieze.annotations.NotNull;


public interface DBIterable<T> extends Closeable {

  @NotNull
  Iterable<T> all();

  @NotNull
  Iterator<T> iterator();
}
