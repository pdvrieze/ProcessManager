package nl.adaptivity.process.clientProcessModel;

import java.util.Collection;
import java.util.List;

import nl.adaptivity.process.processModel.Activity;
import nl.adaptivity.process.processModel.IXmlExportType;
import nl.adaptivity.process.processModel.IXmlImportType;
import nl.adaptivity.process.processModel.IXmlMessage;
import nl.adaptivity.process.processModel.ProcessNodeSet;

import static nl.adaptivity.process.clientProcessModel.ClientProcessModel.*;


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
  public List<IXmlImportType> getImports() {
    return super.getImports();
  }

  @Override
  public void setImports(Collection<? extends IXmlImportType> pImports) {
    super.setImports(pImports);
  }

  @Override
  public List<IXmlExportType> getExports() {
    return super.getExports();
  }

  @Override
  public void setExports(Collection<? extends IXmlExportType> pExports) {
    super.setExports(pExports);
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

}
