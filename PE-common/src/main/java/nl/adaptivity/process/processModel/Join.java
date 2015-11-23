package nl.adaptivity.process.processModel;


import nl.adaptivity.process.ProcessConsts.Engine;

import javax.xml.namespace.QName;


public interface Join<T extends ProcessNode<T>> extends ProcessNode<T>, JoinSplit<T> {

  String ELEMENTLOCALNAME = "join";
  QName ELEMENTNAME = new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);
  QName PREDELEMNAME = new QName(Engine.NAMESPACE, "predecessor", Engine.NSPREFIX);
  // No methods beyond JoinSplit
}