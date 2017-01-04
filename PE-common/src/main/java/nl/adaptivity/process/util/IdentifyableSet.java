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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.util;

import kotlin.sequences.Sequence;
import net.devrieze.util.ReadMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.*;


public abstract class IdentifyableSet<T extends Identifiable> extends AbstractList<T> implements ReadMap<String, T>, RandomAccess, Cloneable {

  private class ReadonlyIterator implements Iterator<T>, ListIterator<T> {

    private final ListIterator<T> mIterator;
    
    private ReadonlyIterator(final ListIterator<T> listIterator) {
      mIterator = listIterator;
    }
    
    @Override
    public boolean hasNext() {
      return mIterator.hasNext();
    }

    @Override
    public T next() {
      return mIterator.next();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("This set is immutable");
    }

    @Override
    public boolean hasPrevious() {
      return mIterator.hasPrevious();
    }

    @Override
    public T previous() {
      return mIterator.previous();
    }

    @Override
    public int nextIndex() {
      return mIterator.nextIndex();
    }

    @Override
    public int previousIndex() {
      return mIterator.previousIndex();
    }

    @Override
    public void set(final T e) {
      throw new UnsupportedOperationException("This set is immutable");
    }

    @Override
    public void add(final T e) {
      throw new UnsupportedOperationException("This set is immutable");
    }

  }

  private class ReadOnlyIdentifyableSet extends IdentifyableSet<T> {

    @NotNull
    @Override
    public Collection<T> values() {
      return this;
    }

    @NotNull
    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override
    public IdentifyableSet<T> clone() {
      return this;
    }

    @Override
    public T get(final int index) {
      return IdentifyableSet.this.get(index);
    }

    @Override
    public int size() {
      return IdentifyableSet.this.size();
    }

    @Override
    public boolean add(final T e) {
      throw new UnsupportedOperationException("This set is immutable");
    }

    @NotNull
    @Override
    public T set(final int index, final T element) {
      throw new UnsupportedOperationException("This set is immutable");
    }

    @Override
    public void add(final int index, final T element) {
      throw new UnsupportedOperationException("This set is immutable");
    }

    @NotNull
    @Override
    public T remove(final int index) {
      throw new UnsupportedOperationException("This set is immutable");
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
      return new ReadonlyIterator(super.listIterator());
    }

    @NotNull
    @Override
    public ListIterator<T> listIterator() {
      return new ReadonlyIterator(super.listIterator());
    }

    @NotNull
    @Override
    public ListIterator<T> listIterator(final int index) {
      return new ReadonlyIterator(super.listIterator(index));
    }

    @Override
    public boolean remove(final Object o) {
      throw new UnsupportedOperationException("This set is immutable");
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
      throw new UnsupportedOperationException("This set is immutable");
    }

    @Override
    public boolean equals(final Object o) {
      return IdentifyableSet.this.equals(o);
    }

    @Override
    public int hashCode() {
      return IdentifyableSet.this.hashCode();
    }
  }

  private static final class BaseIdentifyableSet<V extends Identifiable> extends IdentifyableSet<V> {

    @NotNull private final List<V> mStore;

    public BaseIdentifyableSet() {
      mStore = new ArrayList<>();
    }

    public BaseIdentifyableSet(final int initialcapacity) {
      mStore = new ArrayList<>(initialcapacity);
    }

    public BaseIdentifyableSet(@NotNull final Sequence<? extends V> c) {
      mStore = new ArrayList<V>();
      addAll(c);
    }

    public BaseIdentifyableSet(@NotNull final Iterable<? extends V> c) {
      mStore = (c instanceof Collection) ? new ArrayList<V>(((Collection<? extends V>)c).size()) : new ArrayList<V>();
      addAll(c);
    }

    @NotNull
    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override
    public BaseIdentifyableSet<V> clone() {
      return new BaseIdentifyableSet<>(mStore);
    }

    @Override
    public boolean add(@NotNull final V object) {
      final String name = object.getId();

      if (name==null) {
        if (mStore.contains(object)) {
          return false;
        }
      } else if (containsKey(name)) {
        return false;
      }

      mStore.add(object);
      return true;
    }

    @Override
    public V remove(final int index) {
      return mStore.remove(index);
    }

    @Override
    public boolean remove(final Object o) {
      return mStore.remove(o);
    }

    @Override
    public boolean contains(final Object o) {
      if (o == null) { return false; }
      int len = mStore.size();
      if (o instanceof Identifiable) {
        String id = ((Identifiable)o).getId();
        if (id!=null) {
          for (int i = 0; i < len; ++i) {
            if (id.equals(mStore.get(i).getId())) {
              return true;
            }
          }
          return false;
        }
      }
      for (int i = 0; i < len; ++i) {
        if (o.equals(mStore.get(i))) { return true; }
      }
      return false;
    }

    @Override
    public void clear() {
      mStore.clear();
    }

    @Override
    public Collection<V> values() {
      return Collections.unmodifiableCollection(mStore);
    }

    @Override
    public V set(final int index, final V element) {
      return mStore.set(index, element);
    }

    @Override
    public V get(final int index) {
      return mStore.get(index);
    }

    @Override
    public int size() {
      return mStore.size();
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) { return true; }
      if (o == null || getClass() != o.getClass()) { return false; }

      BaseIdentifyableSet<?> that = (BaseIdentifyableSet<?>) o;

      return mStore.equals(that.mStore);

    }

    @Override
    public int hashCode() {
      int result = 1;
      result = 31 * result + mStore.hashCode();
      return result;
    }
  }

  private static final class EmptyIdentifyableSet<V extends Identifiable> extends IdentifyableSet<V> {

    @NotNull
    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override
    public EmptyIdentifyableSet<V> clone() {
      return this;
    }

    @Override
    public Collection<V> values() {
      return Collections.emptyList();
    }

    @NotNull
    @Override
    public V get(final int index) {
      throw new IndexOutOfBoundsException();
    }

    @Override
    public V set(final int index, final V element) {
      throw new IndexOutOfBoundsException();
    }

    @Override
    public void add(final int index, final V element) {
      throw new IndexOutOfBoundsException();
    }

    @Override
    public int size() {
      return 0;
    }

    @Override
    public int hashCode() {
      return 1;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) { return true; }
      if (o == null || getClass() != o.getClass()) { return false; }
      return true;
    }
  }

  private static final class SingletonIdentifyableSet<V extends Identifiable> extends IdentifyableSet<V> {
    @Nullable private V mElement = null;

    public SingletonIdentifyableSet() {
    }

    public SingletonIdentifyableSet(@Nullable final V element) {
      if (element==null) { throw new NullPointerException(); }
      mElement = element;
    }

    @Nullable
    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override
    public SingletonIdentifyableSet<V> clone() {
      if (mElement==null) {
        return new SingletonIdentifyableSet<>();
      } else {
        return new SingletonIdentifyableSet<>(mElement);
      }
    }

    @Override
    public boolean add(@NotNull final V e) {
      if (e.equals(mElement)) {
        return false;
      } else if (mElement==null) {
        mElement = e;
        return true;
      } else {
        throw new IllegalStateException("Singleton node set can only contain one element");
      }
    }

    @Override
    public Collection<V> values() {
      return Collections.singleton(mElement);
    }

    @Nullable
    @Override
    public V get(final int index) {
      if (mElement==null || index!=0) {
        throw new IndexOutOfBoundsException();
      }
      return mElement;
    }

    @Override
    public V set(final int index, final V element) {
      if (index!=0) {
        throw new IndexOutOfBoundsException();
      }
      V result = mElement;
      mElement = element;
      return result;
    }

    @Override
    public int size() {
      return mElement==null ? 0 : 1;
    }

    @Nullable
    @Override
    public V remove(final int index) {
      if (mElement==null || index!=0) {
        throw new IndexOutOfBoundsException();
      }
      final V result = mElement;
      mElement = null;
      return result;
    }

    @Override
    public int indexOf(@NotNull final Object o) {
      if (o.equals(mElement)) { return 0; }
      return -1;
    }

    @Override
    public int lastIndexOf(@NotNull final Object o) {
      return indexOf(o);
    }

    @Override
    public void clear() {
      mElement = null;
    }

    @Override
    public boolean contains(@NotNull final Object o) {
      if (mElement == null) { return o==null; }
      if (o instanceof Identifiable && mElement.getId()!=null) {
        return mElement.getId().equals(((Identifiable)o).getId());
      }
      return mElement.equals(o);
    }

    @NotNull
    @Override
    public Object[] toArray() {
      if (mElement==null) {
        return new Object[0];
      } else {
        return new Object[] {mElement};
      }
    }

    @NotNull
    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(@NotNull final T[] a) {
      final Class<T> cls = (Class) a.getClass();
      final int size = size();
      if (a.length<size()) {
        final T[] result = (T[]) Array.newInstance(cls, size);
        if (size==1) { result[0] = cls.cast(mElement); }
        return result;
      }
      if (size==1) { a[0] = cls.cast(mElement); }
      if (a.length>size) {
        a[size+1] = null;
      }
      return a;
    }

    @Override
    public boolean remove(@NotNull final Object o) {
      if (o.equals(mElement)) {
        mElement = null;
        return true;
      }
      return false;
    }

    @Override
    public boolean containsAll(@NotNull final Collection<?> c) {
      for(final Object o:c) {
        if (! contains(o)) { return false; }
      }
      return true;
    }

    @Override
    public boolean retainAll(@NotNull final Collection<?> c) {
      if (mElement==null) { return false; }
      boolean change = true;
      for(final Object o:c) {
        if (o.equals(mElement)) {
          change = false;
        }
      }
      if (change) { mElement = null; }
      return change;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) { return true; }
      if (o == null || getClass() != o.getClass()) { return false; }

      SingletonIdentifyableSet<?> that = (SingletonIdentifyableSet<?>) o;

      return mElement != null ? mElement.equals(that.mElement) : that.mElement == null;

    }

    @Override
    public int hashCode() {
      int result = 1;
      result = 31 * result + (mElement != null ? mElement.hashCode() : 0);
      return result;
    }
  }

  private static final class MyKeyIterator implements Iterator<String> {

    private final Iterator<? extends Identifiable> mParent;

    public MyKeyIterator(final Iterator<? extends Identifiable> iterator) {
      mParent = iterator;
    }

    @Override
    public boolean hasNext() {
      return mParent.hasNext();
    }

    @Override
    public String next() {
      return mParent.next().getId();
    }

    @Override
    public void remove() {
      mParent.remove();
    }

  }

  private final class MyKeySet extends AbstractSet<String> {

    @NotNull
    @Override
    public Iterator<String> iterator() {
      return new MyKeyIterator(IdentifyableSet.this.iterator());
    }

    @Override
    public int size() {
      return IdentifyableSet.this.size();
    }

  }

  @NotNull
  public static <V extends Identifiable> IdentifyableSet<V> processNodeSet() {
    return new BaseIdentifyableSet<>();
  }

  @NotNull
  public static <V extends Identifiable> IdentifyableSet<V> processNodeSet(final int size) {
    return new BaseIdentifyableSet<>(size);
  }

  @NotNull
  public static <V extends Identifiable> IdentifyableSet<V> processNodeSet(@NotNull final Sequence<? extends V> collection) {
    return new BaseIdentifyableSet<>(collection);
  }

  @NotNull
  public static <V extends Identifiable> IdentifyableSet<V> processNodeSet(@NotNull final Iterable<? extends V> collection) {
    return new BaseIdentifyableSet<>(collection);
  }

  public static <V extends Identifiable> IdentifyableSet<V> processNodeSet(final int maxSize, @NotNull final Collection<? extends V>elements) {
    switch (maxSize) {
      case 0:
        if (elements.size()>0) { throw new IllegalArgumentException("More elements than allowed"); }
        return IdentifyableSet.empty();
      case 1: {
        if (elements.size() > 1) { throw new IllegalArgumentException("More elements than allowed"); }
        final Iterator<? extends V> iterator = elements.iterator();
        if (iterator.hasNext())
          return singleton(iterator.next());
        else
          return singleton();
      }
      default:
        return processNodeSet(elements);
    }
  }

  public static <V extends Identifiable> IdentifyableSet<V> processNodeSet(final int maxSize, @NotNull final Sequence<? extends V> elements) {
    Iterator<? extends V> it = elements.iterator();
    switch (maxSize) {
      case 0:
        if (it.hasNext()) { throw new IllegalArgumentException("More elements than allowed"); }
        return IdentifyableSet.empty();
      case 1: {
        if (it.hasNext()) {
          try {
            return singleton(it.next());
          } finally {
            if (it.hasNext()) { throw new IllegalArgumentException("More elements than allowed"); }
          }
        } else { return singleton(); }
      }
      default:
        return processNodeSet(elements);
    }
  }

  @Override
  public boolean containsKey(final String key) {
    return get(key) !=null;
  }

  @Override
  public boolean containsValue(final T value) {
    return contains(value);
  }

  @Nullable
  @Override
  public abstract IdentifyableSet<T> clone();

  public boolean addAll(final Iterable<? extends T> c) {
    boolean changed = false;
    for (T e:c) {
      if (add(e)) {
        changed = true;
      }
    }
    return changed;
  }

  public boolean addAll(final Sequence<? extends T> sequence) {
    boolean changed = false;
    for (Iterator<? extends T> it = sequence.iterator(); it.hasNext();) {
      if (add(it.next())) {
        changed = true;
      }
    }
    return changed;
  }

  @Nullable
  public T get(@NotNull final Identifiable key) {
    return get(key.getId());
  }

  @Nullable
  @Override
  public T get(@Nullable final String key) {
    if (key==null) {
      for(final T elem: this) {
        if (elem.getId()==null) {
          return elem;
        }
      }
    } else {
      for(final T elem: this) {
        if (key.equals(elem.getId())) {
          return elem;
        }
      }
    }
    return null;
  }

  @NotNull
  @Override
  public Set<String> keySet() {
    return new MyKeySet();
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public static <V extends Identifiable> IdentifyableSet<V> empty() {
    return EMPTY;
  }

  @SuppressWarnings({ "rawtypes" })
  private static final IdentifyableSet EMPTY = new EmptyIdentifyableSet<>();

  @NotNull
  public static <V extends Identifiable> IdentifyableSet<V> singleton() {
    return new SingletonIdentifyableSet<>();
  }

  @NotNull
  public static <V extends Identifiable> IdentifyableSet<V> singleton(final V element) {
    return new SingletonIdentifyableSet<>(element);
  }

  @NotNull
  public IdentifyableSet<T> readOnly() {
    if (this instanceof IdentifyableSet.ReadOnlyIdentifyableSet) {
      return this;
    }
    return new ReadOnlyIdentifyableSet();
  }

}
