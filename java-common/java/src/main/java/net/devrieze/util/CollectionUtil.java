/*
 * Copyright (c) 2018.
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

/*
 * Created on Dec 1, 2003
 *
 */

package net.devrieze.util;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.*;


/**
 * This class provides functions that the Collections class does not.
 *
 * @author Paul de Vrieze
 * @version 0.1 $Revision$
 */
public final class CollectionUtil {


  private static class CombiningIterable<T, U extends Iterable<? extends T>> implements Iterable<T> {

    U mFirst;
    U[] mOthers;

    public CombiningIterable(final U first, final U[] others) {
      mFirst = first;
      mOthers = others;
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
      return new CombiningIterator<>(this);
    }

  }

  private static class ConcatenatedList<T> extends CombiningIterable<T, List<T>> implements List<T> {

    public ConcatenatedList(final List<T> first, final List<T>[] others) {
      super(first, others);
    }

    private List<? extends T> first() {
      return mFirst;
    }

    private List<? extends T>[] others() {
      return mOthers;
    }

    @Override
    public int size() {
      int size=first().size();
      for(final List<? extends T> other:others()) {
        size+=other.size();
      }
      return size;
    }

    @Override
    public boolean isEmpty() {
      if (!first().isEmpty()) { return false; }
      for(final List<? extends T> other:others()) {
        if (!other.isEmpty()) { return false; }
      }
      return true;
    }

    @Override
    public boolean contains(final Object o) {
      if (!first().contains(o)) { return true; }
      for(final List<? extends T> other:others()) {
        if (other.contains(o)) { return true; }
      }
      return false;
    }

    @NotNull
    @Override
    public Object[] toArray() {
      final Object[] result = new Object[size()];
      return toArrayHelper(result);
    }

    @SuppressWarnings("unchecked")
    private <V> V[] toArrayHelper(final V[] result) {
      int i=0;
      for(final T elem:this) {
        result[i] = (V) elem;
        ++i;
      }
      return result;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    @Override
    public <V> V[] toArray(@NotNull final V[] init) {
      final int size   = size();
      V[]       result = init;
      if (init.length<size) {
        result = (V[]) Array.newInstance(init.getClass().getComponentType(), size);
      } else if (init.length>size) {
        result[size]=null;
      }
      return toArrayHelper(result);
    }

    @Override
    public boolean add(final T elem) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(final Object object) {
      if (first().remove(object)) { return true; }
      for(final List<? extends T> other:others()) {
        if (other.remove(object)) { return true; }
      }
      return false;
    }

    @Override
    public boolean containsAll(@NotNull final Collection<?> c) {
      final HashSet<Object> all = new HashSet<>(c);
      all.removeAll(first());
      if (all.isEmpty()) { return true; }
      for(final List<? extends T> other:others()) {
        all.removeAll(other);
        if (all.isEmpty()) { return true; }
      }
      return all.isEmpty();
    }

    @Override
    public boolean addAll(@NotNull final Collection<? extends T> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(final int index, @NotNull final Collection<? extends T> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(@NotNull final Collection<?> c) {
      boolean result = first().removeAll(c);
      for(final List<? extends T> other:others()) {
        result = other.removeAll(c)||result;
      }
      return result;
    }

    @Override
    public boolean retainAll(@NotNull final Collection<?> c) {
      boolean result = first().retainAll(c);
      for(final List<? extends T> other:others()) {
        result = other.retainAll(c)||result;
      }
      return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void clear() {
      mFirst = Collections.emptyList();
      mOthers = (List<T>[]) new List[0];
    }

    @Override
    public T get(final int index) {
      int offset = first().size();
      if (index<offset) {
        return first().get(index);
      }
      for(final List<? extends T> other:others()) {
        final int oldOffset = offset;
        offset+=other.size();
        if (index<offset) {
          return other.get(index - oldOffset);
        }
      }
      throw new IndexOutOfBoundsException();
    }

    @Override
    public T set(final int index, final T element) {
      throw new UnsupportedOperationException();
//      int offset = first().size();
//      if (index<offset) {
//        return first().set(index, element);
//      }
//      for(List<? extends T> other:others()) {
//        int oldOffset = offset;
//        offset+=other.size();
//        if (index<offset) {
//          return other.set(index - oldOffset, element);
//        }
//      }
//      throw new IndexOutOfBoundsException();
    }

    @Override
    public void add(final int index, final T element) {
      throw new UnsupportedOperationException();
    }

    @Override
    public T remove(final int index) {
      int offset = first().size();
      if (index<offset) {
        return first().remove(index);
      }
      for(final List<? extends T> other:others()) {
        final int oldOffset = offset;
        offset+=other.size();
        if (index<offset) {
          return other.remove(index - oldOffset);
        }
      }
      throw new IndexOutOfBoundsException();
    }

    @Override
    public int indexOf(final Object o) {
      {
        final int idx = first().indexOf(o);
        if (idx>=0) { return idx; }
      }
      int offset = first().size();
      for(final List<? extends T> other:others()) {
        final int idx = other.indexOf(o);
        if (idx>=0) {
          return offset+idx;
        }
        offset+=other.size();
      }
      throw new IndexOutOfBoundsException();
    }

    @Override
    public int lastIndexOf(final Object o) {
      int offset = size()-others()[mOthers.length-1].size();
      for(int i=mOthers.length-1; i>=0; --i) {
        final List<? extends T> other = others()[i];
        offset -= other.size();
        final int idx = other.lastIndexOf(o);
        if (idx>=0) {
          return offset+idx;
        }
      }
      return first().lastIndexOf(o);
    }

    @NotNull
    @Override
    public ListIterator<T> listIterator() {
      return new CombiningListIterator<>(this);
    }

    @NotNull
    @Override
    public ListIterator<T> listIterator(final int index) {
      return new CombiningListIterator<>(this, index);
    }

    @NotNull
    @Override
    public List<T> subList(final int fromIndex, final int toIndex) {
      throw new UnsupportedOperationException();
    }

  }

  private static final class CombiningIterator<T> implements Iterator<T> {

    private int mIteratorIdx=0;
    private final List<Iterator<? extends T>> mIterators;

    public CombiningIterator(final CombiningIterable<T, ? extends Iterable<? extends T>> iterable) {
      mIterators = toIterators(iterable.mFirst, iterable.mOthers);
    }

    private static <T> List<Iterator<? extends T>> toIterators(final Iterable<? extends T> first, final Iterable<? extends T>[] others) {
      final List<Iterator<? extends T>> result = new ArrayList<>(others.length + 1);
      result.add(first.iterator());
      for(final Iterable<? extends T> other:others) {
        result.add(other.iterator());
      }
      return result;
    }

    @Override
    public boolean hasNext() {
      while (mIteratorIdx<mIterators.size()){
        if (mIterators.get(mIteratorIdx).hasNext()) {
          return true;
        }
        ++mIteratorIdx;
      }
      return false;
    }

    @Override
    public T next() {
      while (mIteratorIdx<mIterators.size()) {
        if (mIterators.get(mIteratorIdx).hasNext()) {
          return mIterators.get(mIteratorIdx).next();
        }
        ++mIteratorIdx;
      }
      throw new NoSuchElementException();
    }

    @Override
    public void remove() {
      mIterators.get(mIteratorIdx).remove();
    }

  }

  private static final class CombiningListIterator<T> implements ListIterator<T> {

    private int mIteratorIdx=0;
    private int mItemIdx=0;
    private final List<ListIterator<? extends T>> mIterators;

    public CombiningListIterator(final ConcatenatedList<T> list) {
      mIterators = toIterators(list.first(), list.others());
    }

    public CombiningListIterator(final ConcatenatedList<T> list, final int index) {
      final List<? extends T> first = list.first();
      final List<? extends T>[] others = list.others();
      mIteratorIdx = -1;

      mIterators = new ArrayList<>(others.length+1);
      if (index<first.size()) {
        mIteratorIdx=0;
        mIterators.add(first.listIterator(index));
      } else {
        mIterators.add(first.listIterator());
      }

      final int offset = first.size();
      for(final List<? extends T> other:others) {
        if (mIteratorIdx<0 && index-offset<other.size()) {
          mIterators.add(other.listIterator(index-offset));
          mIteratorIdx = mIterators.size()-1;
        } else {
          mIterators.add(other.listIterator());
        }
      }
      if (mIteratorIdx<0) { throw new IndexOutOfBoundsException(); }
    }

    private static <T> List<ListIterator<? extends T>> toIterators(final List<? extends T> first, final List<? extends T>[] others) {
      final List<ListIterator<? extends T>> result = new ArrayList<>(others.length + 1);
      result.add(first.listIterator());
      for(final List<? extends T> other:others) {
        result.add(other.listIterator());
      }
      return result;
    }

    @Override
    public boolean hasNext() {
      while (mIteratorIdx<mIterators.size()){
        if (mIterators.get(mIteratorIdx).hasNext()) {
          return true;
        }
        ++mIteratorIdx;
      }
      return false;
    }

    @Override
    public T next() {
      while (mIteratorIdx<mIterators.size()) {
        if (mIterators.get(mIteratorIdx).hasNext()) {
          mItemIdx++;
          return mIterators.get(mIteratorIdx).next();
        }
        ++mIteratorIdx;
      }
      throw new NoSuchElementException();
    }

    @Override
    public void remove() {
      mItemIdx--;
      mIterators.get(mIteratorIdx).remove();
    }

    @Override
    public boolean hasPrevious() {
      while (mIteratorIdx>=0){
        if (mIterators.get(mIteratorIdx).hasPrevious()) {
          return true;
        }
        if (mIteratorIdx==0) { break; }
        --mIteratorIdx;
      }
      return false;
    }

    @Override
    public T previous() {
      while (mIteratorIdx>=0) {
        if (mIterators.get(mIteratorIdx).hasPrevious()) {
          mItemIdx--;
          return mIterators.get(mIteratorIdx).previous();
        }
        if (mIteratorIdx==0) { break; }
        --mIteratorIdx;
      }
      throw new NoSuchElementException();
    }

    @Override
    public int nextIndex() {
      return mItemIdx+1;
    }

    @Override
    public int previousIndex() {
      return mItemIdx-1;
    }

    @Override
    public void set(final T e) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void add(final T e) {
      throw new UnsupportedOperationException();
    }

  }

  private static class MonitoringIterator<T> implements Iterator<T> {

    private final Collection<CollectionChangeListener<? super T>> mListeners;

    private final Iterator<T> mOriginal;

    private T last;

    @Override
    public boolean hasNext() {
      return mOriginal.hasNext();
    }

    @Override
    public T next() {
      last = mOriginal.next();
      return last;
    }

    @Override
    public void remove() {
      mOriginal.remove();
      fireElementRemoved();
      last = null;
    }

    private void fireElementRemoved() {
      if (mListeners != null) {
        RuntimeException error = null;
        for (final CollectionChangeListener<? super T> listener : mListeners) {
          try {
            listener.elementRemoved(last);
          } catch (final RuntimeException e) {
            if (error == null) {
              error = e;
            }
          }
        }
        if (error != null) {
          throw error;
        }
      }
    }

    public MonitoringIterator(final Collection<CollectionChangeListener<? super T>> listeners, final Iterator<T> original) {
      mListeners = listeners;
      mOriginal = original;
    }

  }

  private static class MonitoringCollectionAdapter<V> implements MonitorableCollection<V> {

    private final Collection<V> mCollection;

    private Set<CollectionChangeListener<? super V>> mListeners;


    public MonitoringCollectionAdapter(final Collection<V> collection) {
      mCollection = collection;
    }

    @Override
    public void addCollectionChangeListener(final CollectionChangeListener<? super V> listener) {
      getListeners().add(listener);
    }

    @Override
    public void removeCollectionChangeListener(final CollectionChangeListener<? super V> listener) {
      if (mListeners != null) {
        getListeners().remove(listener);
      }
    }

    private Set<CollectionChangeListener<? super V>> getListeners() {
      if (mListeners == null) {
        mListeners = new HashSet<>();
      }
      return mListeners;
    }

    private void fireAddEvent(final V element) {
      if (mListeners != null) {
        MultiException error = null;
        for (final CollectionChangeListener<? super V> listener : mListeners) {
          try {
            listener.elementAdded(element);
          } catch (final RuntimeException e) {
            error = MultiException.add(error, e);
          }
        }
        MultiException.throwIfError(error);
      }
    }

    private void fireClearEvent() {
      if (mListeners != null) {
        MultiException error = null;
        for (final CollectionChangeListener<? super V> listener : mListeners) {
          try {
            listener.collectionCleared();
          } catch (final RuntimeException e) {
            error = MultiException.add(error, e);
          }
        }
        MultiException.throwIfError(error);
      }
    }

    private void fireRemoveEvent(final V element) {
      if (mListeners != null) {
        MultiException error = null;
        for (final CollectionChangeListener<? super V> listener : mListeners) {
          try {
            listener.elementRemoved(element);
          } catch (final RuntimeException e) {
            error = MultiException.add(error, e);
          }
        }
        MultiException.throwIfError(error);
      }
    }

    @Override
    public boolean add(final V element) {
      final boolean result = mCollection.add(element);
      if (result) {
        fireAddEvent(element);
      }
      return result;
    }

    @Override
    public boolean addAll(@NotNull final Collection<? extends V> c) {
      boolean result = false;
      for (final V elem : c) {
        result |= add(elem);
      }
      return result;
    }

    @Override
    public void clear() {
      mCollection.clear();
      fireClearEvent();
    }

    @Override
    public boolean contains(final Object o) {
      return mCollection.contains(o);
    }

    @Override
    public boolean containsAll(@NotNull final Collection<?> c) {
      return mCollection.containsAll(c);
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(final Object o) {
      return mCollection.equals(o);
    }

    @Override
    public int hashCode() {
      return mCollection.hashCode();
    }

    @Override
    public boolean isEmpty() {
      return mCollection.isEmpty();
    }

    @NotNull
    @Override
    public Iterator<V> iterator() {
      return monitoringIterator(mListeners, mCollection.iterator());
    }

    @Override
    public boolean remove(final Object o) {
      final boolean result = mCollection.remove(o);
      if (result) {
        @SuppressWarnings("unchecked")
        final V element = (V) o;
        fireRemoveEvent(element);
      }
      return result;
    }

    @Override
    public boolean removeAll(@NotNull final Collection<?> c) {
      boolean result = false;
      for (final Object o : c) {
        result |= remove(o);
      }
      return result;
    }

    @Override
    public boolean retainAll(@NotNull final Collection<?> c) {
      boolean modified = false;
      final Iterator<V> e = iterator();
      while (e.hasNext()) {
        if (!c.contains(e.next())) {
          e.remove();
          modified = true;
        }
      }
      return modified;
    }

    @Override
    public int size() {
      return mCollection.size();
    }

    @NotNull
    @Override
    public Object[] toArray() {
      return mCollection.toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull final T[] a) {
      //noinspection SuspiciousToArrayCall
      return mCollection.toArray(a);
    }

  }

  @SuppressWarnings("rawtypes")
  public static final SortedSet EMPTYSORTEDSET = new EmptySortedSet<>();

  private static class EmptySortedSet<T> extends AbstractSet<T> implements SortedSet<T> {

    @Override
    public Comparator<? super T> comparator() {
      return null;
    }

    @NotNull
    @Override
    public SortedSet<T> subSet(final T fromElement, final T toElement) {
      throw new IllegalArgumentException();
    }

    @NotNull
    @Override
    public SortedSet<T> headSet(final T toElement) {
      throw new IllegalArgumentException();
    }

    @NotNull
    @Override
    public SortedSet<T> tailSet(final T fromElement) {
      throw new IllegalArgumentException();
    }

    @NotNull
    @Override
    public T first() {
      throw new IndexOutOfBoundsException();
    }

    @NotNull
    @Override
    public T last() {
      throw new IndexOutOfBoundsException();
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
      // Use this roundabout way as Android does not have Collections.emptyIterator
      // It does have emptySet().iterator() though which has the same effect.
      return Collections.<T>emptySet().iterator();
    }

    @Override
    public int size() {
      return 0;
    }

  }

  public static boolean hasNull(final Collection<?> objects) {
    for(final Object o:objects) {
      if (o==null) return true;
    }
    return false;
  }

  public static boolean isNullOrEmpty(final byte[] content) {
    return content==null || content.length==0;
  }

  public static boolean isNullOrEmpty(final short[] content) {
    return content==null || content.length==0;
  }

  public static boolean isNullOrEmpty(final boolean[] content) {
    return content==null || content.length==0;
  }

  public static boolean isNullOrEmpty(final char[] content) {
    return content==null || content.length==0;
  }

  public static boolean isNullOrEmpty(final int[] content) {
    return content==null || content.length==0;
  }

  public static boolean isNullOrEmpty(final long[] content) {
    return content==null || content.length==0;
  }

  public static boolean isNullOrEmpty(final float[] content) {
    return content==null || content.length==0;
  }

  public static boolean isNullOrEmpty(final double[] content) {
    return content==null || content.length==0;
  }

  public static boolean isNullOrEmpty(final Object[] content) {
    return content==null || content.length==0;
  }

  @SuppressWarnings("ChainOfInstanceofChecks")
  public static <T> ArrayList<T> toArrayList(final Iterable<T> values) {
    if (values instanceof ArrayList) {
      return (ArrayList<T>) values;
    }
    if (values instanceof Collection) {
      return new ArrayList<>((Collection<T>) values);
    }
    final ArrayList<T> result = new ArrayList<>();
    for (final T value : values) {
      result.add(value);
    }
    return result;
  }

  /**
   * @deprecated Use {@link Collections#emptySortedSet()} when on Java 1.8
   */
  @Deprecated
  @SuppressWarnings({"unchecked", "AssignmentOrReturnOfFieldWithMutableType"})
  public static <T> SortedSet<T> emptySortedSet() {
    return EMPTYSORTEDSET;
  }

  @SuppressWarnings({"unchecked", "AssignmentOrReturnOfFieldWithMutableType"})
  public static <T> LinkedHashSet<T> emptyLinkedHashSet() {
    return EMPTYLINKEDHASHSET;
  }

  @SuppressWarnings("rawtypes")
  private static final LinkedHashSet EMPTYLINKEDHASHSET = new EmptyLinkedHashSet<>();

  private static final class EmptyLinkedHashSet<T> extends LinkedHashSet<T> {

    private static final long serialVersionUID = 979867754186273651L;

    @Override
    public Iterator<T> iterator() {
      // Use this roundabout way as Android does not have Collections.emptyIterator
      // It does have emptySet().iterator() though which has the same effect.
      return Collections.<T>emptySet().iterator();
    }

    @Override
    public int size() { return 0; }

    @Override
    public boolean isEmpty() { return true; }

    @Override
    public boolean contains(final Object o) { return false; }

    @Override
    public boolean add(final T e) { throw new UnsupportedOperationException("Not mutable"); }

    @Override
    public boolean remove(final Object o) { return false; }

    @Override
    public void clear() { /* noop */ }

    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override
    public EmptyLinkedHashSet<T> clone() { return this; }

  }

  /**
   * This is only a static class, so the constructor only makes sense for
   * subclasses.
   */
  private CollectionUtil() {
    /* This class should not be instanciated */
  }

  /**
   * Check whether the collection contains only elements assignable to specified
   * class. This should allways hold when generics are used by the compiler
   *
   * @param <T> The type of the elements that returned collection should
   *          contain.
   * @param <X> The type of the elements of the checked collection.
   * @param clazz The class of which the elements need to be in the collection.
   * @param collection The collection to be checked
   * @return the checked collection. This allows faster assignment.
   * @throws ClassCastException When the element is not of the right class.
   */
  public static <T, X extends T> Collection<X> checkClass(final Class<T> clazz, final Collection<X> collection) throws ClassCastException {
    for (final Object x : collection) {
      if (!clazz.isInstance(x)) {
        throw new ClassCastException("An element of the collection is not of the required type: " + clazz.getName());
      }
    }

    return collection;
  }

  /**
   * Create an iterable that combines the given iterables.
   * @param first The first iterable.
   * @param others The other iterables.
   * @return An iterable that combines both when iterating.
   */
  @SafeVarargs
  public static <T> Iterable<T> combine(final Iterable<? extends T> first, final Iterable<? extends T>... others) {
    return new CombiningIterable<>(first, others);
  }

  /**
   * Create an iterable that combines the given iterables.
   * @param first The first iterable.
   * @param other The other iterable.
   * @return An iterable that combines both when iterating.
   */
  public static <T> Iterable<T> combine(final Iterable<? extends T> first, final Iterable<? extends T> other) {
    @SuppressWarnings("unchecked") final Iterable<? extends T>[] others = new Iterable[] { other };
    return new CombiningIterable<>(first, others);
  }
  public static <T> List<T> concatenate(final List<T> first, final List<T> second) {
    @SuppressWarnings("unchecked") final List<T>[] others = (List<T>[]) new List<?>[] { second };
    return new ConcatenatedList<>(first, others);
  }

  @SafeVarargs
  public static <T> List<T> concatenate(final List<T> first, final List<T>... others) {
    return new ConcatenatedList<>(first, others);
  }

  public static <T extends Comparable<? super T>> List<T> sortedList(final Collection<T> collection) {
    final List<T> result = new ArrayList<>(collection);
    Collections.sort(result);
    return result;
  }

  public static <T, U> HashMap<T, U> createMap(final T key, final U value) {
    final HashMap<T, U> map = new HashMap<>();
    map.put(key, value);
    return map;
  }

  /**
   * Create a hashmap from a set of key-value pairs.
   *
   * @param <T> The type of the keys to the hashmap
   * @param <U> The type of the values.
   * @param tupples The elements to put into the map.
   * @return The resulting hashmap.
   */
  @SafeVarargs
  public static <T, U> HashMap<T, U> hashMap(@SuppressWarnings("unchecked") final Tupple<? extends T, ? extends U>... tupples) {
    // Make the new hashmap have a capacity 125% of the amount of tuples.
    final HashMap<T, U> result = new HashMap<>(tupples.length + (tupples.length >> 2));
    for (final Tupple<? extends T, ? extends U> t : tupples) {
      result.put(t.getElem1(), t.getElem2());
    }
    return result;
  }

  @SafeVarargs
  public static <T extends Enum<T>, U> EnumMap<T, U> enumMap(@SuppressWarnings("unchecked") final Tupple<? extends T, ? extends U>... tupples) {
    if (tupples.length < 1) {
      throw new IllegalArgumentException("For an enumeration map simple creator, at least one element must be present");
    }
    @SuppressWarnings("unchecked")
    final Class<T> type = (Class<T>) Enum.class.asSubclass(tupples[0].getElem1().getClass());
    final EnumMap<T, U> result = new EnumMap<>(type);
    for (final Tupple<? extends T, ? extends U> t : tupples) {
      result.put(t.getElem1(), t.getElem2());
    }
    return result;
  }

  /**
   * @deprecated Use {@link Collections#singletonList(Object)}
   */
  @Deprecated
  public static <T> List<T> singletonList(final T elem) {
    return Collections.singletonList(elem);
  }

  public static <T> Iterator<T> monitoringIterator(final Collection<CollectionChangeListener<? super T>> listeners, final Iterator<T> original) {
    return new MonitoringIterator<>(listeners, original);
  }

  public static <T> void mergeLists(final List<T> base, final List<? extends T> other) {
    for (int i = 0; i < other.size(); i++) {
      if (i>=base.size()) {
        for (int j = i; j < other.size(); j++) {
          base.add(other.get(i));
        }
        break;
      }
      final T current     = base.get(i);
      final T replacement = other.get(i);
      if (current == null ? replacement!=null : ! current.equals(replacement)) {
        // not equal
        Object next;
        if (i+1<base.size() && (((next = base.get(i+1)) == replacement) || (next != null && next.equals(replacement)))) {
          base.remove(i); // Remove the current item so there is a match, the item was removed in the other
        } else if (i+1<other.size() && ((current == (next=other.get(i+1)))|| (current != null && current.equals(next)))) {
          base.add(i, replacement); // Insert the item here. The item was added in the other list
        } else {
          base.set(i, replacement);// In other cases, just set the value
        }
      }
    }
    while (base.size()>other.size()) {
      base.remove(base.size()-1); // Remove the last item
    }

  }

  /**
   * Create a collection wrapper that allows for monitoring of changes to the collection. For this to work the changes
   * need to be made through the monitor.
   * @param collection The collection to monitor
   * @param <T> The type contained in the collection
   * @return The collection that can be monitored.
   */
  public static <T> MonitorableCollection<T> monitorableCollection(final Collection<T> collection) {
    return new MonitoringCollectionAdapter<>(collection);
  }

  public static boolean containsInstances(final Iterable<?> collection, final Class<?>... classes) {
    for (final Object element : collection) {
      for (final Class<?> c : classes) {
        if (c.isInstance(element)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Add instances of any of the target classes to the given collection.
   * @param target The receiving collection
   * @param source The source iterable
   * @param verifiers The classes to check. Only one needs to match.
   * @param <T> The type contained in the collection
   * @param <V> The resulting collection type.
   * @return This returns {@code target}
   */
  @SafeVarargs
  public static <T, V extends Collection<T>> V addInstancesOf(final V target, final Iterable<?> source, @SuppressWarnings("unchecked") final Class<? extends T>... verifiers) {
    for (final Object element : source) {
      for (final Class<? extends T> c : verifiers) {
        if (c.isInstance(element)) {
          final T e = c.cast(element);
          target.add(e);
          break;
        }
      }
    }
    return target;
  }


  public static <T, V extends Collection<T>> V addNonInstancesOf(final V target, final Iterable<? extends T> source, final Class<?>... verifiers) {
    for (final T element : source) {
      for (final Class<?> c : verifiers) {
        if (!c.isInstance(element)) {
          target.add(element);
          break;
        }
      }
    }
    return target;
  }

  public static <T> List<T> copy(final Collection<? extends T> orig) {
    if (orig==null) { return null; }
    if (orig.size()==1) {
      return Collections.singletonList(orig.iterator().next());
    }
    final ArrayList<T> result = new ArrayList<>(orig.size());
    result.addAll(orig);
    return result;
  }

}
