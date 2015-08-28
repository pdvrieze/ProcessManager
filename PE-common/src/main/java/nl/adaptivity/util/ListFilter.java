package nl.adaptivity.util;

import net.devrieze.parser.eval.types.Array;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class ListFilter<T> extends AbstractList<T> {

  private final List<T> mSource;
  private final Class<T> mClass;
  private final boolean mLax;

  public ListFilter(Class<T> pClass, boolean pLax) {
    mSource = new ArrayList<>();
    mClass = pClass;
    mLax = pLax;
  }

  @Override
  public T get(int pIndex) {
    return mSource.get(pIndex);
  }

  @Override
  public int size() {
    return mSource.size();
  }

  @Override
  public void add(int pIndex, Object pElement) {
    if (mClass.isInstance(pElement)) {
      mSource.add(pIndex, mClass.cast(pElement));
    } else if(! mLax) {
      mClass.cast(pElement);
    }
  }

  @Override
  public boolean add(final Object elem) {
    if (mClass.isInstance(elem)) {
      return super.add(mClass.cast(elem));
    } else {
      return false;
    }
  }

  public boolean addAllObjects(final Collection<?> c) {
    boolean result = false;

    if (mSource instanceof ArrayList) { ((ArrayList) mSource).ensureCapacity(mSource.size()+c.size()); }

    for(Object elem: c) { result=add(elem) || result; }
    return result;
  }

  @Override
  public T set(int pIndex, T pElement) {
    if (mClass.isInstance(pElement)) {
      return mSource.set(pIndex, pElement);
    } else if(! mLax) {
      mClass.cast(pElement);
    }
    return mSource.get(pIndex);
  }

}
