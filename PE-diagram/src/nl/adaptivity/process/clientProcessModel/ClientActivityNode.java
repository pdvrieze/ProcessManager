package nl.adaptivity.process.clientProcessModel;

import static nl.adaptivity.process.clientProcessModel.ClientProcessModel.NS_PM;

import java.util.Collection;
import java.util.List;

import nl.adaptivity.process.processModel.Activity;
import nl.adaptivity.process.processModel.IXmlDefineType;
import nl.adaptivity.process.processModel.IXmlMessage;
import nl.adaptivity.process.processModel.IXmlResultType;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.ProcessNodeSet;


public class ClientActivityNode<T extends IClientProcessNode<T>> extends ClientProcessNode<T> implements Activity<T> {

  private String aName;

  private String aCondition;

  private ClientMessage aMessage;

  public ClientActivityNode() {
    super();
  }


  public ClientActivityNode(String pId) {
    super(pId);
  }

  protected ClientActivityNode(ClientActivityNode<T> pOrig) {
    super(pOrig);
    aName = pOrig.aName;
    aCondition = pOrig.aCondition;
    aMessage = pOrig.aMessage;
  }

  @Override
  public String getName() {
    return aName;
  }

  @Override
  public void setName(String pName) {
    aName = pName;
  }

  @Override
  public String getCondition() {
    return aCondition;
  }

  @Override
  public void setCondition(String pCondition) {
    aCondition = pCondition;
  }

  @Override
  public T getPredecessor() {
    ProcessNodeSet<T> list = getPredecessors();
    if (list.isEmpty()) {
      return null;
    } else {
      return list.get(0);
    }
  }

  @Override
  public void setPredecessor(T pPredecessor) {
    T previous = getPredecessor();
    if (previous==null) {
      removePredecessor(previous);
    }
    addPredecessor(pPredecessor);
  }


  @Override
  public IXmlMessage getMessage() {
    return aMessage;
  }

  @Override
  public void setMessage(IXmlMessage pMessage) {
    aMessage = ClientMessage.from(pMessage);
  }


  @Override
  public List<IXmlResultType> getResults() {
    return super.getResults();
  }

  @Override
  public void setDefines(Collection<? extends IXmlDefineType> pImports) {
    super.setDefines(pImports);
  }

  @Override
  public List<IXmlDefineType> getDefines() {
    return super.getDefines();
  }

  @Override
  public void setResults(Collection<? extends IXmlResultType> pExports) {
    super.setResults(pExports);
  }


  @Override
  public void serialize(SerializerAdapter pOut) {
    pOut.startTag(NS_PM, "activity", true);
    serializeCommonAttrs(pOut);
    if (aName!=null) { pOut.addAttribute(null, "name", aName); }
    if (aCondition!=null) { pOut.addAttribute(null, "condition", aCondition); }
    serializeCommonChildren(pOut);
    if (aMessage!=null) { aMessage.serialize(pOut); }
    pOut.endTag(NS_PM, "activity", true);
  }

  @Override
  public <R> R visit(ProcessNode.Visitor<R> pVisitor) {
    return pVisitor.visitActivity(this);
  }

}
