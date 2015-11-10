package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.util.Identifiable;

import java.util.Collection;
import java.util.List;

import static nl.adaptivity.process.clientProcessModel.ClientProcessModel.NS_PM;


public class ClientActivityNode<T extends IClientProcessNode<T>> extends ClientProcessNode<T> implements Activity<T> {

  private String aName;

  private String aCondition;

  private ClientMessage aMessage;

  public ClientActivityNode() {
    super();
  }


  public ClientActivityNode(String id) {
    super(id);
  }

  protected ClientActivityNode(ClientActivityNode<T> orig) {
    super(orig);
    aName = orig.aName;
    aCondition = orig.aCondition;
    aMessage = orig.aMessage;
  }

  @Override
  public String getName() {
    return aName;
  }

  @Override
  public void setName(String name) {
    aName = name;
  }

  @Override
  public String getCondition() {
    return aCondition;
  }

  @Override
  public void setCondition(String condition) {
    aCondition = condition;
  }

  @Override
  public Identifiable getPredecessor() {
    ProcessNodeSet<? extends Identifiable> list = getPredecessors();
    if (list.isEmpty()) {
      return null;
    } else {
      return list.get(0);
    }
  }

  @Override
  public void setPredecessor(Identifiable predecessor) {
    Identifiable previous = getPredecessor();
    if (previous==null) {
      removePredecessor(previous);
    }
    addPredecessor(predecessor);
  }


  @Override
  public IXmlMessage getMessage() {
    return aMessage;
  }

  @Override
  public void setMessage(IXmlMessage message) {
    aMessage = ClientMessage.from(message);
  }


  @Override
  public List<IXmlResultType> getResults() {
    return super.getResults();
  }

  @Override
  public void setDefines(Collection<? extends IXmlDefineType> imports) {
    super.setDefines(imports);
  }

  @Override
  public List<IXmlDefineType> getDefines() {
    return super.getDefines();
  }

  @Override
  public void setResults(Collection<? extends IXmlResultType> exports) {
    super.setResults(exports);
  }


  @Override
  public void serialize(SerializerAdapter out) {
    out.startTag(NS_PM, "activity", true);
    serializeCommonAttrs(out);
    if (aName!=null) { out.addAttribute(null, "name", aName); }
    if (aCondition!=null) { out.addAttribute(null, "condition", aCondition); }
    serializeCommonChildren(out);
    if (aMessage!=null) { aMessage.serialize(out); }
    out.endTag(NS_PM, "activity", true);
  }

  @Override
  public <R> R visit(ProcessNode.Visitor<R> visitor) {
    return visitor.visitActivity(this);
  }

}
