package nl.adaptivity.process.processModel;

import org.jetbrains.annotations.NotNull;
import net.devrieze.util.ReadMap;
import nl.adaptivity.process.util.Identifiable;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.*;

// TODO rename to IdentifyableSet and move to util package
public abstract class ProcessNodeSet<T extends Identifiable> extends AbstractList<T> implements ReadMap<String, T>, RandomAccess, Cloneable {



  
  private class ReadonlyIterator implements Iterator<T>, ListIterator<T> {

    private final ListIterator<T> aIterator;
    
    private ReadonlyIterator(final ListIterator<T> listIterator) {
      aIterator = listIterator;
    }
    
    @Override
    public boolean hasNext() {
      return aIterator.hasNext();
    }

    @Override
    public T next() {
      return aIterator.next();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("This set is immutable");
    }

    @Override
    public boolean hasPrevious() {
      return aIterator.hasPrevious();
    }

    @Override
    public T previous() {
      return aIterator.previous();
    }

    @Override
    public int nextIndex() {
      return aIterator.nextIndex();
    }

    @Override
    public int previousIndex() {
      return aIterator.previousIndex();
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

  private class ReadOnlyProcessNodeSet extends ProcessNodeSet<T> {

    @NotNull
    @Override
    public Collection<T> values() {
      return this;
    }

    @NotNull
    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override
    public ProcessNodeSet<T> clone() {
      return this;
    }

    @Override
    public T get(final int index) {
      return ProcessNodeSet.this.get(index);
    }

    @Override
    public int size() {
      return ProcessNodeSet.this.size();
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
  
  }

  private static final class BaseProcessNodeSet<V extends Identifiable> extends ProcessNodeSet<V> {

    @NotNull private final List<V> aStore;

    public BaseProcessNodeSet() {
      aStore = new ArrayList<>();
    }

    public BaseProcessNodeSet(final int initialcapacity) {
      aStore = new ArrayList<>(initialcapacity);
    }

    public BaseProcessNodeSet(@NotNull final Collection<? extends V> c) {
      aStore = new ArrayList<>(c.size());
      addAll(c);
    }

    @NotNull
    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override
    public BaseProcessNodeSet<V> clone() {
      return new BaseProcessNodeSet<>(aStore);
    }

    @Override
    public boolean add(@NotNull final V object) {
      final String name = object.getId();

      if (containsKey(name)) {
        return false;
      }

      aStore.add(object);
      return true;
    }

    @Override
    public V remove(final int index) {
      return aStore.remove(index);
    }

    @Override
    public boolean remove(final Object o) {
      return aStore.remove(o);
    }

    @Override
    public void clear() {
      aStore.clear();
    }

    @Override
    public Collection<V> values() {
      return Collections.unmodifiableCollection(aStore);
    }

    @Override
    public V get(final int index) {
      return aStore.get(index);
    }

    @Override
    public int size() {
      return aStore.size();
    }

  }

  private static final class EmptyProcessNodeSet<V extends Identifiable> extends ProcessNodeSet<V> {

    @NotNull
    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override
    public EmptyProcessNodeSet<V> clone() {
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
    public int size() {
      return 0;
    }

  }

  private static final class SingletonProcessNodeSet<V extends Identifiable> extends ProcessNodeSet<V> {
    @Nullable private V aElement = null;

    public SingletonProcessNodeSet() {
    }

    public SingletonProcessNodeSet(@Nullable final V element) {
      if (element==null) { throw new NullPointerException(); }
      aElement = element;
    }

    @Nullable
    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override
    public SingletonProcessNodeSet<V> clone() {
      if (aElement==null) {
        return new SingletonProcessNodeSet<>();
      } else {
        return new SingletonProcessNodeSet<>(aElement);
      }
    }

    @Override
    public boolean add(@NotNull final V e) {
      if (e.equals(aElement)) {
        return false;
      } else if (aElement==null) {
        aElement = e;
        return true;
      } else {
        throw new IllegalStateException("Singleton node set can only contain one element");
      }
    }

    @Override
    public Collection<V> values() {
      return Collections.singleton(aElement);
    }

    @Nullable
    @Override
    public V get(final int index) {
      if (aElement==null || index!=0) {
        throw new IndexOutOfBoundsException();
      }
      return aElement;
    }

    @Override
    public int size() {
      return aElement==null ? 0 : 1;
    }

    @Nullable
    @Override
    public V remove(final int index) {
      if (aElement==null || index!=0) {
        throw new IndexOutOfBoundsException();
      }
      final V result = aElement;
      aElement = null;
      return result;
    }

    @Override
    public int indexOf(@NotNull final Object o) {
      if (o.equals(aElement)) { return 0; }
      return -1;
    }

    @Override
    public int lastIndexOf(@NotNull final Object o) {
      return indexOf(o);
    }

    @Override
    public void clear() {
      aElement = null;
    }

    @Override
    public boolean contains(@NotNull final Object o) {
      return o.equals(aElement);
    }

    @NotNull
    @Override
    public Object[] toArray() {
      if (aElement==null) {
        return new Object[0];
      } else {
        return new Object[] {aElement};
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
        if (size==1) { result[0] = cls.cast(aElement); }
        return result;
      }
      if (size==1) { a[0] = cls.cast(aElement); }
      if (a.length>size) {
        a[size+1] = null;
      }
      return a;
    }

    @Override
    public boolean remove(@NotNull final Object o) {
      if (o.equals(aElement)) {
        aElement = null;
        return true;
      }
      return false;
    }

    @Override
    public boolean containsAll(@NotNull final Collection<?> c) {
      for(final Object o:c) {
        if (! o.equals(aElement)) { return false; }
      }
      return true;
    }

    @Override
    public boolean retainAll(@NotNull final Collection<?> c) {
      if (aElement==null) { return false; }
      boolean change = true;
      for(final Object o:c) {
        if (o.equals(aElement)) {
          change = false;
        }
      }
      if (change) { aElement = null; }
      return change;
    }


  }

  private static final class MyKeyIterator implements Iterator<String> {

    private final Iterator<? extends Identifiable> aParent;

    public MyKeyIterator(final Iterator<? extends Identifiable> iterator) {
      aParent = iterator;
    }

    @Override
    public boolean hasNext() {
      return aParent.hasNext();
    }

    @Override
    public String next() {
      return aParent.next().getId();
    }

    @Override
    public void remove() {
      aParent.remove();
    }

  }

  private final class MyKeySet extends AbstractSet<String> {

    @NotNull
    @Override
    public Iterator<String> iterator() {
      return new MyKeyIterator(ProcessNodeSet.this.iterator());
    }

    @Override
    public int size() {
      return ProcessNodeSet.this.size();
    }

  }

  @NotNull
  public static <V extends Identifiable> ProcessNodeSet<V> processNodeSet() {
    return new BaseProcessNodeSet<>();
  }

  @NotNull
  public static <V extends Identifiable> ProcessNodeSet<V> processNodeSet(final int size) {
    return new BaseProcessNodeSet<>(size);
  }

  @NotNull
  public static <V extends Identifiable> ProcessNodeSet<V> processNodeSet(@NotNull final Collection<? extends V> collection) {
    return new BaseProcessNodeSet<>(collection);
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
  public abstract ProcessNodeSet<T> clone();

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
  public static <V extends Identifiable> ProcessNodeSet<V> empty() {
    return EMPTY;
  }

  @SuppressWarnings({ "rawtypes" })
  private static final ProcessNodeSet EMPTY = new EmptyProcessNodeSet<>();

  @NotNull
  public static <V extends Identifiable> ProcessNodeSet<V> singleton() {
    return new SingletonProcessNodeSet<>();
  }

  @NotNull
  public static <V extends Identifiable> ProcessNodeSet<V> singleton(final V element) {
    return new SingletonProcessNodeSet<>(element);
  }

  @NotNull
  public ProcessNodeSet<T> readOnly() {
    if (this instanceof ProcessNodeSet.ReadOnlyProcessNodeSet) {
      return this;
    }
    return new ReadOnlyProcessNodeSet();
  }

}
