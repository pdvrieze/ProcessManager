package nl.adaptivity.process.processModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import nl.adaptivity.process.processModel.engine.IProcessModelRef;
import nl.adaptivity.process.processModel.engine.ProcessModelRef;
import nl.adaptivity.process.processModel.engine.ProcessNodeImpl;


@XmlRootElement(name = "processModels")
@XmlAccessorType(XmlAccessType.NONE)
public class ProcessModelRefs<T extends ProcessNodeImpl> implements Collection<ProcessModelRef> {

  private Collection<ProcessModelRef> aCollection;

  public ProcessModelRefs() {
    aCollection = new ArrayList<>();
  }

  public ProcessModelRefs(final Collection<? extends IProcessModelRef<? extends T>> pCollection) {
    aCollection = new ArrayList<>(pCollection.size());
    addAllI(pCollection);
  }

  @XmlElement(name = "processModel")
  public Collection<ProcessModelRef> getElements() {
    if (aCollection == null) {
      aCollection = new ArrayList<>();
    }
    return aCollection;
  }

  public boolean add(final IProcessModelRef<?> pE) {
    if (pE instanceof ProcessModelRef) {
      return aCollection.add((ProcessModelRef)pE);
    } else {
      return aCollection.add(new ProcessModelRef(pE));
    }
  }

  @Override
  public boolean add(final ProcessModelRef pE) {
    return aCollection.add(pE);
  }

  public boolean addAllI(final Collection<? extends IProcessModelRef<? extends T>> pC) {
    boolean changed = false;
    for(IProcessModelRef<? extends T> elem:pC) {
      if (elem instanceof ProcessModelRef) {
        changed = aCollection.add((ProcessModelRef)elem) || changed;
      } else {
        changed = aCollection.add(new ProcessModelRef(elem)) || changed;
      }
    }
    return changed;
  }

  @Override
  public boolean addAll(final Collection<? extends ProcessModelRef> pC) {
    return aCollection.addAll(pC);
  }

  @Override
  public void clear() {
    aCollection.clear();
  }

  @Override
  public boolean contains(final Object pO) {
    return aCollection.contains(pO);
  }

  @Override
  public boolean containsAll(final Collection<?> pC) {
    return aCollection.containsAll(pC);
  }

  @Override
  public int hashCode() {
    return aCollection.hashCode();
  }

  @Override
  public boolean isEmpty() {
    return aCollection.isEmpty();
  }

  @Override
  public Iterator<ProcessModelRef> iterator() {
    return aCollection.iterator();
  }

  @Override
  public boolean remove(final Object pO) {
    return aCollection.remove(pO);
  }

  @Override
  public boolean removeAll(final Collection<?> pC) {
    return aCollection.removeAll(pC);
  }

  @Override
  public boolean retainAll(final Collection<?> pC) {
    return aCollection.retainAll(pC);
  }

  @Override
  public int size() {
    return aCollection.size();
  }

  @Override
  public Object[] toArray() {
    return aCollection.toArray();
  }

  @Override
  public <U> U[] toArray(final U[] pA) {
    return aCollection.toArray(pA);
  }

}
