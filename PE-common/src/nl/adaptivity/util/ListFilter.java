package nl.adaptivity.util;

import java.util.AbstractList;
import java.util.List;


public class ListFilter<T> extends AbstractList<T> {

  private final List<T> mSource;
  private final Class<T> mClass;
  private final boolean mLax;

  public ListFilter(List<T> source, Class<T> pClass, boolean pLax) {
    mSource = source;
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
  public void add(int pIndex, T pElement) {
    if (mClass.isInstance(pElement)) {
      super.add(pIndex, pElement);
    } else if(! mLax) {
      mClass.cast(pElement);
    }
  }

  @Override
  public T set(int pIndex, T pElement) {
    if (mClass.isInstance(pElement)) {
      return super.set(pIndex, pElement);
    } else if(! mLax) {
      mClass.cast(pElement);
    }
    return mSource.get(pIndex);
  }

}
