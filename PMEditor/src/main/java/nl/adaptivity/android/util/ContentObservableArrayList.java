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

package nl.adaptivity.android.util;

import android.databinding.ListChangeRegistry;
import android.databinding.Observable;
import android.databinding.Observable.OnPropertyChangedCallback;
import android.databinding.ObservableList;

import java.util.ArrayList;
import java.util.Collection;


/**
 * An extended {@link android.databinding.ObservableArrayList} that also observes changes in the children.
 * Created by pdvrieze on 04/01/16.
 */
public class ContentObservableArrayList<T extends Observable> extends ArrayList<T> implements ObservableList<T> {

  private transient ListChangeRegistry mListeners = new ListChangeRegistry();

  private final OnPropertyChangedCallback mChangeCallback = new OnPropertyChangedCallback() {
    @Override
    public void onPropertyChanged(final Observable sender, final int propertyId) {
      ContentObservableArrayList.this.onPropertyChanged(sender, propertyId);
    }
  };

  private void onPropertyChanged(final Observable sender, final int propertyId) {
    final int i = indexOf(sender);
    if (i>=0) {
      mListeners.notifyChanged(this, i, 1);
    }
  }

  @Override
  public void addOnListChangedCallback(final OnListChangedCallback listener) {
    if (mListeners == null) {
      mListeners = new ListChangeRegistry();
    }
    mListeners.add(listener);
  }

  @Override
  public void removeOnListChangedCallback(final OnListChangedCallback listener) {
    if (mListeners != null) {
      mListeners.remove(listener);
    }
  }

  @Override
  public boolean add(final T object) {
    if (super.add(object)) {
      notifyAdd(size() - 1, 1);
      if (object!=null) { object.addOnPropertyChangedCallback(mChangeCallback); }
      return true;
    }
    return false;
  }

  @Override
  public void add(final int index, final T object) {
    super.add(index, object);
    notifyAdd(index, 1);
    if (object!=null) { object.addOnPropertyChangedCallback(mChangeCallback); }
  }

  @Override
  public boolean addAll(final Collection<? extends T> collection) {
    final int     oldSize = size();
    final boolean added   = super.addAll(collection);
    if (added) {
      notifyAdd(oldSize, size() - oldSize);
      for(final Observable o:collection) {
        if (o!=null) { o.addOnPropertyChangedCallback(mChangeCallback); }
      }
    }
    return added;
  }

  @Override
  public boolean addAll(final int index, final Collection<? extends T> collection) {
    final boolean added = super.addAll(index, collection);
    if (added) {
      notifyAdd(index, collection.size());
      for(final Observable o:collection) {
        if (o!=null) { o.addOnPropertyChangedCallback(mChangeCallback); }
      }
    }
    return added;
  }

  @Override
  public void clear() {
    final int oldSize = size();
    for (int i = oldSize-1; i >=0 ; i--) {
      get(i).removeOnPropertyChangedCallback(mChangeCallback);
    }
    super.clear();
    if (oldSize != 0) {
      notifyRemove(0, oldSize);
    }
  }

  @Override
  public T remove(final int index) {
    final T val = super.remove(index);
    if (val!=null) { val.removeOnPropertyChangedCallback(mChangeCallback); }
    notifyRemove(index, 1);
    return val;
  }

  @Override
  public boolean remove(final Object object) {
    final int index = indexOf(object);
    if (index >= 0) {
      remove(index);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public T set(final int index, final T object) {
    final T val = super.set(index, object);
    if (val!=null) {
      val.removeOnPropertyChangedCallback(mChangeCallback);
    }
    if (object!=null) { object.addOnPropertyChangedCallback(mChangeCallback); }
    if (mListeners != null) {
      mListeners.notifyChanged(this, index, 1);
    }
    return val;
  }

  @Override
  protected void removeRange(final int fromIndex, final int toIndex) {
    for (int i = fromIndex; i < toIndex; i++) {
      final Observable object = get(i);
      if (object!=null) { object.removeOnPropertyChangedCallback(mChangeCallback); }
    }
    super.removeRange(fromIndex, toIndex);
    notifyRemove(fromIndex, toIndex - fromIndex);
  }

  private void notifyAdd(final int start, final int count) {
    if (mListeners != null) {
      mListeners.notifyInserted(this, start, count);
    }
  }

  private void notifyRemove(final int start, final int count) {
    if (mListeners != null) {
      mListeners.notifyRemoved(this, start, count);
    }
  }
}
