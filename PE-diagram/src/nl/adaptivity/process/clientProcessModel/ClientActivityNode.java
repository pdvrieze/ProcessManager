package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlWriter;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

import java.util.*;

import static nl.adaptivity.process.clientProcessModel.ClientProcessModel.NS_PM;


public class ClientActivityNode<T extends IClientProcessNode<T>> extends ClientProcessNode<T> implements Activity<T> {

  private String mName;

  private String mCondition;

  private ClientMessage mMessage;
  private List<XmlDefineType> mDefines;
  private List<XmlResultType> mResults;

  public ClientActivityNode(final boolean compat) {
    super(compat);
  }


  public ClientActivityNode(String id, final boolean compat) {
    super(id, compat);
  }

  protected ClientActivityNode(ClientActivityNode<T> orig, final boolean compat) {
    super(orig, compat);
    mName = orig.mName;
    mCondition = orig.mCondition;
    mMessage = orig.mMessage;
  }

  @Override
  public QName getElementName() {
    return ELEMENTNAME;
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
  public Identifiable getPredecessor() { // XXX pull up
    Set<? extends Identifiable> list = getPredecessors();
    if (list.isEmpty()) {
      return null;
    } else {
      return list.iterator().next();
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
  public void setDefines(@Nullable final Collection<? extends IXmlDefineType> exports) {
    super.setDefines(exports);
  }

  @Override
  public void setResults(@Nullable final Collection<? extends IXmlResultType> imports) {
    super.setResults(imports);
  }

  @Override
  public void serialize(XmlWriter out) throws XmlException {
    out.startTag(NS_PM, "activity", null);
    serializeAttributes(out);
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
