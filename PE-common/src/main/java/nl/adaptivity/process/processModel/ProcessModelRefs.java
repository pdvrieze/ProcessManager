package nl.adaptivity.process.processModel;

import nl.adaptivity.process.processModel.engine.ExecutableProcessNode;
import nl.adaptivity.process.processModel.engine.IProcessModelRef;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl;
import nl.adaptivity.process.processModel.engine.ProcessModelRef;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;


@XmlRootElement(name = "processModels")
public class ProcessModelRefs implements Collection<IProcessModelRef<? extends ExecutableProcessNode, ? extends ProcessModelImpl>> {

  private Collection<IProcessModelRef<? extends ExecutableProcessNode, ? extends ProcessModelImpl>> mCollection;

  public ProcessModelRefs() {
    mCollection = new ArrayList<>();
  }

  /**
   * Create a new collection with the given source of references to initialise it
   * @param collection The initialiser.
   */
  public ProcessModelRefs(@NotNull final Collection<? extends IProcessModelRef<ExecutableProcessNode, ? extends ProcessModelImpl>> collection) {
    mCollection = new ArrayList<>(collection.size());
    addAll(collection);
  }

  @NotNull
  @XmlElement(name = "processModel")
  public Collection<ProcessModelRef> getElements() {
    if (mCollection == null) {
      mCollection = new ArrayList<>();
    }
    //noinspection unchecked
    return (Collection) mCollection;
  }

  public boolean add(final IProcessModelRef<? extends ExecutableProcessNode, ? extends ProcessModelImpl> modelRef) {
    if (modelRef instanceof ProcessModelRef) {
      return mCollection.add(modelRef);
    } else {
      return mCollection.add(new ProcessModelRef(modelRef));
    }
  }

  public boolean add(final ProcessModelRef e) {
    return mCollection.add(e);
  }

  public boolean addAll(@NotNull final Collection<? extends IProcessModelRef<? extends ExecutableProcessNode, ? extends ProcessModelImpl>> c) {
    boolean changed = false;
    for(final IProcessModelRef<? extends ExecutableProcessNode, ? extends ProcessModelImpl> elem:c) {
      changed |= add(elem);
    }
    return changed;
  }

//  @Override
//  public boolean addAll(final Collection<? extends ProcessModelRef> pC) {
//    return mCollection.addAll(pC);
//  }

  @Override
  public void clear() {
    mCollection.clear();
  }

  @Override
  public boolean contains(final Object o) {
    return mCollection.contains(o);
  }

  @Override
  public boolean containsAll(@NotNull final Collection<?> c) {
    return mCollection.containsAll(c);
  }

  @Override
  public int hashCode() {
    return mCollection.hashCode();
  }

  @Override
  public boolean equals(@NotNull final Object obj) {
    if (getClass()!=obj.getClass()) { return false; }
    return mCollection.equals(((ProcessModelRefs)obj).mCollection);
  }

  @Override
  public boolean isEmpty() {
    return mCollection.isEmpty();
  }

  @NotNull
  @Override
  public Iterator<IProcessModelRef<? extends ExecutableProcessNode, ? extends ProcessModelImpl>> iterator() {
    return mCollection.iterator();
  }

  @Override
  public boolean remove(final Object o) {
    return mCollection.remove(o);
  }

  @Override
  public boolean removeAll(@NotNull final Collection<?> c) {
    return mCollection.removeAll(c);
  }

  @Override
  public boolean retainAll(@NotNull final Collection<?> c) {
    return mCollection.retainAll(c);
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
  public <U> U[] toArray(@NotNull final U[] a) {
    //noinspection SuspiciousToArrayCall
    return mCollection.toArray(a);
  }

}
