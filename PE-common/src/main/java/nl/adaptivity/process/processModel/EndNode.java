package nl.adaptivity.process.processModel;

import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.process.util.Identifiable;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

import java.util.Collection;


public interface EndNode<T extends ProcessNode<T>> extends ProcessNode<T>{

  String ELEMENTLOCALNAME = "end";
  QName ELEMENTNAME = new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);

  void setDefines(Collection<? extends IXmlDefineType> exports);

  @Nullable
  Identifiable getPredecessor();

  void setPredecessor(Identifiable predecessor);

}