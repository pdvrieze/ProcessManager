package nl.adaptivity.process.engine.processModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="processModels")
@XmlAccessorType(XmlAccessType.NONE)
public class ProcessModelRefs implements Collection<ProcessModelRef> {
  
  private Collection<ProcessModelRef> aCollection;
  
  public ProcessModelRefs(Collection<ProcessModelRef> pCollection) {
    aCollection = new ArrayList<ProcessModelRef>(pCollection.size());
    aCollection.addAll(pCollection);
  }

  @XmlElement(name="processModel")
  public Collection<ProcessModelRef> getElements() {
    if (aCollection==null) {
      aCollection = new ArrayList<ProcessModelRef>();
    }
    return aCollection;
  }

  public boolean add(ProcessModelRef pE) {
    return aCollection.add(pE);
  }

  public boolean addAll(Collection<? extends ProcessModelRef> pC) {
    return aCollection.addAll(pC);
  }

  public void clear() {
    aCollection.clear();
  }

  public boolean contains(Object pO) {
    return aCollection.contains(pO);
  }

  public boolean containsAll(Collection<?> pC) {
    return aCollection.containsAll(pC);
  }

  @Override
  public int hashCode() {
    return aCollection.hashCode();
  }

  public boolean isEmpty() {
    return aCollection.isEmpty();
  }

  public Iterator<ProcessModelRef> iterator() {
    return aCollection.iterator();
  }

  public boolean remove(Object pO) {
    return aCollection.remove(pO);
  }

  public boolean removeAll(Collection<?> pC) {
    return aCollection.removeAll(pC);
  }

  public boolean retainAll(Collection<?> pC) {
    return aCollection.retainAll(pC);
  }

  public int size() {
    return aCollection.size();
  }

  public Object[] toArray() {
    return aCollection.toArray();
  }

  public <T> T[] toArray(T[] pA) {
    return aCollection.toArray(pA);
  }

}
