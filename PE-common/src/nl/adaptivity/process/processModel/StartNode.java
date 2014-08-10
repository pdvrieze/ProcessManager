package nl.adaptivity.process.processModel;

import java.util.List;


public interface StartNode<T extends ProcessNode<T>> extends ProcessNode<T>{

  public List<? extends IXmlResultType> getResults();

}