package nl.adaptivity.process.processModel;


import nl.adaptivity.process.ProcessConsts.Engine;

import javax.xml.namespace.QName;


public interface Condition {

  String ELEMENTLOCALNAME = "condition";
  QName ELEMENTNAME = new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);

  String getCondition();

}