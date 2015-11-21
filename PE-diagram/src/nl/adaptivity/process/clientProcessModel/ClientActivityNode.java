package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlWriter;

import java.util.Collection;
import java.util.List;

import static nl.adaptivity.process.clientProcessModel.ClientProcessModel.NS_PM;


public class ClientActivityNode<T extends IClientProcessNode<T>> extends ClientProcessNode<T> implements Activity<T> {

  private String mName;

  private String mCondition;

  private ClientMessage mMessage;

  public ClientActivityNode() {
    super();
  }


  public ClientActivityNode(String id) {
    super(id);
  }

  protected ClientActivityNode(ClientActivityNode<T> orig) {
    super(orig);
    mName = orig.mName;
    mCondition = orig.mCondition;
    mMessage = orig.mMessage;
  }

  @Override
  public String getName() {
    return mName;
  }

  @Override
  public void setName(String name) {
    mName = name;
  }

  @Override
  public String getCondition() {
    return mCondition;
  }

  @Override
  public void setCondition(String condition) {
    mCondition = condition;
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
    return mMessage;
  }

  @Override
  public void setMessage(IXmlMessage message) {
    mMessage = ClientMessage.from(message);
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
  public void serialize(XmlWriter out) throws XmlException {
    out.startTag(NS_PM, "activity", null);
    serializeCommonAttrs(out);
    if (mName != null) { out.attribute(null, "name", null, mName); }
    if (mCondition != null) { out.attribute(null, "condition", null, mCondition); }
    serializeCommonChildren(out);
    if (mMessage != null) { mMessage.serialize(out); }
    out.endTag(NS_PM, "activity", null);
  }

  @Override
  public <R> R visit(ProcessNode.Visitor<R> visitor) {
    return visitor.visitActivity(this);
  }

}
