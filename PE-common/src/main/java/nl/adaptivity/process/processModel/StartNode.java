package nl.adaptivity.process.processModel;


import nl.adaptivity.process.ProcessConsts.Engine;

import javax.xml.namespace.QName;


public interface StartNode<T extends ProcessNode<T, M>, M extends ProcessModel<T, M>> extends ProcessNode<T, M>{

  String ELEMENTLOCALNAME = "start";
  QName ELEMENTNAME = new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);
// No special aspects.
}