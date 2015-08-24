package nl.adaptivity.process.processModel;

import java.lang.reflect.Array;
import java.util.*;

import net.devrieze.annotations.NotNull;
import net.devrieze.util.ReadMap;


public abstract class ProcessNodeSet<T extends ProcessNode<T>> extends AbstractList<T> implements ReadMap<String, T>, RandomAccess, Cloneable {



  
  private class ReadonlyIterator implements Iterator<T>, ListIterator<T> {

    private final ListIterator<T> aIterator;
    
    private ReadonlyIterator(ListIterator<T> pListIterator) {
      aIterator = pListIterator;
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
    public void set(T pE) {
      throw new UnsupportedOperationException("This set is immutable");
    }

    @Override
    public void add(T pE) {
      throw new UnsupportedOperationException("This set is immutable");
    }

  }

  private class ReadOnlyProcessNodeSet extends ProcessNodeSet<T> {

    @Override
    public Collection<T> values() {
      return this;
    }

    @Override
    public ProcessNodeSet<T> clone() {
      return this;
    }

    @Override
    public T get(int pIndex) {
      return ProcessNodeSet.this.get(pIndex);
    }

    @Override
    public int size() {
      return ProcessNodeSet.this.size();
    }

    @Override
    public boolean add(T pE) {
      throw new UnsupportedOperationException("This set is immutable");
    }

    @Override
    public T set(int pIndex, T pElement) {
      throw new UnsupportedOperationException("This set is immutable");
    }

    @Override
    public void add(int pIndex, T pElement) {
      throw new UnsupportedOperationException("This set is immutable");
    }

    @Override
    public T remove(int pIndex) {
      throw new UnsupportedOperationException("This set is immutable");
    }

    @Override
    public Iterator<T> iterator() {
      return new ReadonlyIterator(super.listIterator());
    }

    @Override
    public ListIterator<T> listIterator() {
      return new ReadonlyIterator(super.listIterator());
    }

    @Override
    public ListIterator<T> listIterator(int pIndex) {
      return new ReadonlyIterator(super.listIterator(pIndex));
    }

    @Override
    public boolean remove(Object pO) {
      throw new UnsupportedOperationException("This set is immutable");
    }

    @Override
    public boolean retainAll(Collection<?> pC) {
      throw new UnsupportedOperationException("This set is immutable");
    }
  
  }

  private static final class BaseProcessNodeSet<V extends ProcessNode<V>> extends ProcessNodeSet<V> {

    private List<V> aStore;

    public BaseProcessNodeSet() {
      aStore = new ArrayList<>();
    }

    public BaseProcessNodeSet(int initialcapacity) {
      aStore = new ArrayList<>(initialcapacity);
    }

    public BaseProcessNodeSet(Collection<? extends V> c) {
      aStore = new ArrayList<>(c.size());
      addAll(c);
    }

    @Override
    public BaseProcessNodeSet<V> clone() {
      return new BaseProcessNodeSet<>(aStore);
    }

    @Override
    public boolean add(V pObject) {
      final String name = pObject.getId();

      if (containsKey(name)) {
        return false;
      }

      aStore.add(pObject);
      return true;
    }

    @Override
    public V remove(int pIndex) {
      return aStore.remove(pIndex);
    }

    @Override
    public boolean remove(Object pO) {
      return aStore.remove(pO);
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
    public V get(int pIndex) {
      return aStore.get(pIndex);
    }

    @Override
    public int size() {
      return aStore.size();
    }

  }

  private static final class EmptyProcessNodeSet<V extends ProcessNode<V>> extends ProcessNodeSet<V> {

    @Override
    public EmptyProcessNodeSet<V> clone() {
      return this;
    }

    @Override
    public Collection<V> values() {
      return Collections.emptyList();
    }

    @Override
    public V get(int pIndex) {
      throw new IndexOutOfBoundsException();
    }

    @Override
    public int size() {
      return 0;
    }

  }

  private static final class SingletonProcessNodeSet<V extends ProcessNode<V>> extends ProcessNodeSet<V> {
    private V aElement = null;

    public SingletonProcessNodeSet() {
    }

    public SingletonProcessNodeSet(V pElement) {
      if (pElement==null) { throw new NullPointerException(); }
      aElement = pElement;
    }

    @Override
    public SingletonProcessNodeSet<V> clone() {
      if (aElement==null) {
        return new SingletonProcessNodeSet<>();
      } else {
        return new SingletonProcessNodeSet<>(aElement);
      }
    }

    @Override
    public boolean add(@NotNull V pE) {
      if (pE.equals(aElement)) {
        return false;
      } else if (aElement==null) {
        aElement = pE;
        return true;
      } else {
        throw new IllegalStateException("Singleton node set can only contain one element");
      }
    }

    @Override
    public Collection<V> values() {
      return Collections.singleton(aElement);
    }

    @Override
    public V get(int pIndex) {
      if (aElement==null || pIndex!=0) {
        throw new IndexOutOfBoundsException();
      }
      return aElement;
    }

    @Override
    public int size() {
      return aElement==null ? 0 : 1;
    }

    @Override
    public V remove(int pIndex) {
      if (aElement==null || pIndex!=0) {
        throw new IndexOutOfBoundsException();
      }
      V result = aElement;
      aElement = null;
      return result;
    }

    @Override
    public int indexOf(Object pO) {
      if (pO.equals(aElement)) { return 0; }
      return -1;
    }

    @Override
    public int lastIndexOf(Object pO) {
      return indexOf(pO);
    }

    @Override
    public void clear() {
      aElement = null;
    }

    @Override
    public boolean contains(Object pO) {
      return pO.equals(aElement);
    }

    @Override
    public Object[] toArray() {
      if (aElement==null) {
        return new Object[0];
      } else {
        return new Object[] {aElement};
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(T[] pA) {
      Class<T> cls = (Class<T>) pA.getClass();
      int size = size();
      if (pA.length<size()) {
        T[] result = (T[]) Array.newInstance(cls, size);
        if (size==1) { result[0] = cls.cast(aElement); }
        return result;
      }
      if (size==1) { pA[0] = cls.cast(aElement); }
      if (pA.length>size) {
        pA[size+1] = null;
      }
      return pA;
    }

    @Override
    public boolean remove(Object pO) {
      if (pO.equals(aElement)) {
        aElement = null;
        return true;
      }
      return false;
    }

    @Override
    public boolean containsAll(Collection<?> pC) {
      for(Object o:pC) {
        if (! o.equals(aElement)) { return false; }
      }
      return true;
    }

    @Override
    public boolean retainAll(Collection<?> pC) {
      if (aElement==null) { return false; }
      boolean change = true;
      for(Object o:pC) {
        if (o.equals(aElement)) {
          change = false;
        }
      }
      if (change) { aElement = null; }
      return change;
    }


  }

  private static final class MyKeyIterator implements Iterator<String> {

    private Iterator<? extends ProcessNode<?>> aParent;

    public MyKeyIterator(Iterator<? extends ProcessNode<?>> pIterator) {
      aParent = pIterator;
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

    @Override
    public Iterator<String> iterator() {
      return new MyKeyIterator(ProcessNodeSet.this.iterator());
    }

    @Override
    public int size() {
      return ProcessNodeSet.this.size();
    }

  }
  public static <V extends ProcessNode<V>> ProcessNodeSet<V> processNodeSet() {
    return new BaseProcessNodeSet<>();
  }

  public static <V extends ProcessNode<V>> ProcessNodeSet<V> processNodeSet(int pSize) {
    return new BaseProcessNodeSet<>(pSize);
  }

  public static <V extends ProcessNode<V>> ProcessNodeSet<V> processNodeSet(Collection<? extends V> pCollection) {
    return new BaseProcessNodeSet<>(pCollection);
  }

  @Override
  public boolean containsKey(String pKey) {
    return get(pKey) !=null;
  }

  @Override
  public boolean containsValue(T pValue) {
    return contains(pValue);
  }

  @Override
  public abstract ProcessNodeSet<T> clone();

  @Override
  public T get(String pKey) {
    if (pKey==null) {
      for(T elem: this) {
        if (elem.getId()==null) {
          return elem;
        }
      }
    } else {
      for(T elem: this) {
        if (pKey.equals(elem.getId())) {
          return elem;
        }
      }
    }
    return null;
  }

  @Override
  public Set<String> keySet() {
    return new MyKeySet();
  }

  @SuppressWarnings("unchecked")
  public static <V extends ProcessNode<V>> ProcessNodeSet<V> empty() {
    return EMPTY;
  }

  @SuppressWarnings({ "rawtypes" })
  private static ProcessNodeSet EMPTY = new EmptyProcessNodeSet<>();

  public static <V extends ProcessNode<V>> ProcessNodeSet<V> singleton() {
    return new SingletonProcessNodeSet<>();
  }

  public static <V extends ProcessNode<V>> ProcessNodeSet<V> singleton(V pElement) {
    return new SingletonProcessNodeSet<>(pElement);
  }

  public ProcessNodeSet<T> readOnly() {
    if (this instanceof ProcessNodeSet.ReadOnlyProcessNodeSet) {
      return this;
    }
    return new ReadOnlyProcessNodeSet();
  }

}
