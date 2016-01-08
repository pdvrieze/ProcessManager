package net.devrieze.util;

import java.util.Iterator;


public interface AutoCloseableIterator<T> extends AutoCloseable, Iterator<T> {
  // Interface for joint functionality
}
