package net.devrieze.util.db;

import net.devrieze.util.AutoCloseableIterator;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;


public interface DBIterable<T> extends AutoCloseable {

  @NotNull
  Iterable<T> all();

  @NotNull
  AutoCloseableIterator<T> iterator();
  
  @Override
  void close() throws SQLException;
}
