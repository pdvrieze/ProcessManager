/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package net.devrieze.util;

import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;


/**
 * Created by pdvrieze on 19/05/16.
 */
public abstract class AbstractTransactionedHandleMap<V,T extends Transaction> implements TransactionedHandleMap<V,T> {

  @Deprecated
  @Override
  public V get(final T transaction, final long handle) throws SQLException {
    return get(transaction, Handles.<V>handle(handle));
  }

  @Override
  public V castOrGet(final T transaction, final Handle<? extends V> handle) throws SQLException {
    return get(transaction, handle);
  }

  @Deprecated
  @Override
  public V get(final long handle) {
    return get(Handles.<V>handle(handle));
  }

  @Deprecated
  @Override
  public V get(final Handle<? extends V> handle) {
    try(T t=newTransaction()) {
      return get(t, handle);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Deprecated
  @NotNull
  @Override
  public Object[] toArray() {
    return toArray(new Object[size()]);
  }

  @Deprecated
  @Override
  public Collection<V> toCollection() {
    return this;
  }

  @Deprecated
  @Override
  public Iterator<V> iterator() {
    try(T t = newTransaction()) {
      return iterator(t, false);
    }
  }

  @Deprecated
  @Override
  public boolean contains(final T transaction, final long handle) throws SQLException {
    return contains(transaction, Handles.<V>handle(handle));
  }

  @Deprecated
  @Override
  public boolean contains(final long handle) {
    return contains(Handles.<V>handle(handle));
  }

  @Deprecated
  @Override
  public boolean contains(final Object object) {
    try (T t=newTransaction()) {
      return contains(t, object);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Deprecated
  @Override
  public boolean containsAll(final Collection<?> c) {
    try (T t = newTransaction()) {
      return containsAll(t, c);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Deprecated
  @Override
  public boolean containsAll(final T transaction, final Collection<?> c) throws SQLException {
    for(Object o:c) {
      if(! contains(o)) { return false; }
    }
    return true;
  }

  @Deprecated
  @Override
  public boolean containsHandle(final Handle<? extends V> handle) {
    try (T t=newTransaction()) {
      return contains(t, handle);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Deprecated
  @Override
  public V set(final T transaction, final long handle, final V value) throws SQLException {
    return set(transaction, Handles.<V>handle(handle), value);
  }

  @Override
  @Deprecated
  public V set(final long handle, final V value) {
    return set(Handles.<V>handle(handle), value);
  }

  @Deprecated
  @Override
  public V set(final Handle<? extends V> handle, final V value) {
    try (T t = newTransaction()) {
      return set(t, handle, value);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Deprecated
  @Override
  public <W extends V> Handle<W> put(final W value) {
    try(T t = newTransaction()) {
      return put(t, value);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Deprecated
  @Override
  public boolean add(final V v) {
    return put(v)!=null;
  }

  @Deprecated
  @Override
  public boolean addAll(final Collection<? extends V> c) {
    boolean changed =false;
    for(V elem:c) {
      changed = add(elem)||changed;
    }
    return changed;
  }

  @Deprecated
  @Override
  public boolean remove(final T transaction, final long handle) throws SQLException {
    return remove(transaction, Handles.<V>handle(handle));
  }

  @Deprecated
  @Override
  public boolean remove(final Handle<? extends V> handle) {
    try(T t = newTransaction()) {
      return remove(t, handle);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Deprecated
  @Override
  public boolean remove(final long handle) {
    return remove(Handles.<V>handle(handle));
  }

  @Deprecated
  @Override
  public boolean removeAll(final Collection<?> c) {
    boolean changed = false;
    for (Object obj : c) {
      changed = remove(obj) || changed;
    }
    return changed;
  }

  @Deprecated
  public void clear() {
    try (T t = newTransaction()) {
      clear(t);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

}
