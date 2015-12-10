package nl.adaptivity.process;

import net.devrieze.util.MemHandleMap;
import net.devrieze.util.Transaction;

import java.sql.SQLException;


/**
 * Created by pdvrieze on 09/12/15.
 */
public class MemTransactionedHandleMap<T> extends MemHandleMap<T> implements net.devrieze.util.TransactionedHandleMap<T, Transaction> {

  @Override
  public long put(final Transaction transaction, final T value) throws SQLException {
    return put(value);
  }

  @Override
  public T get(final Transaction transaction, final long handle) throws SQLException {
    return get(handle);
  }

  @Override
  public T get(final Transaction transaction, final Handle<? extends T> handle) throws SQLException {
    return get(handle);
  }

  @Override
  public T castOrGet(final Transaction transaction, final Handle<? extends T> handle) throws SQLException {
    return get(handle);
  }

  @Override
  public T set(final Transaction transaction, final long handle, final T value) throws SQLException {
    return set(handle, value);
  }

  @Override
  public T set(final Transaction transaction, final Handle<? extends T> handle, final T value) throws SQLException {
    return set(handle, value);
  }

  @Override
  public Iterable<T> iterable(final Transaction transaction) {
    return this;
  }

  @Override
  public boolean contains(final Transaction transaction, final Object o) throws SQLException {
    return contains(o);
  }

  @Override
  public boolean contains(final Transaction transaction, final Handle<? extends T> handle) throws SQLException {
    return contains(handle);
  }

  @Override
  public boolean contains(final Transaction transaction, final long handle) throws SQLException {
    return contains(handle);
  }

  @Override
  public boolean remove(final Transaction transaction, final Handle<? extends T> object) throws SQLException {
    return remove(object);
  }

  @Override
  public boolean remove(final Transaction transaction, final long handle) throws SQLException {
    return remove(handle);
  }

  @Override
  public void invalidateCache(final Handle<? extends T> handle) { /* No-op */ }

  @Override
  public void clear(final Transaction transaction) throws SQLException {
    clear();
  }
}
