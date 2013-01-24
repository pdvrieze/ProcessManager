package net.devrieze.util.db;

import java.io.Closeable;
import java.util.Iterator;


public interface DBIterable<T> extends Closeable {

  Iterable<T> all();
  
  Iterator<T> iterator();
}
