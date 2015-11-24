package nl.adaptivity.process.processModel;


import nl.adaptivity.process.ProcessConsts.Engine;

import javax.xml.namespace.QName;


public interface Split<T extends ProcessNode<T, M>, M extends ProcessModel<T, M>> extends ProcessNode<T, M>, JoinSplit<T, M> {

  String ELEMENTLOCALNAME = "split";
  QName ELEMENTNAME = new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);
  // No methods beyond JoinSplit
}