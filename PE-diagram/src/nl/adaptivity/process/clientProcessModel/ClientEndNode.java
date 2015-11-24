package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.EndNode;
import nl.adaptivity.process.processModel.IXmlDefineType;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.ProcessNodeSet;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlWriter;

import javax.xml.namespace.QName;

import java.util.Collection;

import static nl.adaptivity.process.clientProcessModel.ClientProcessModel.NS_PM;


public class ClientEndNode<T extends IClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends ClientProcessNode<T, M> implements EndNode<T, M>, IClientProcessNode<T, M> {

  public ClientEndNode() {
    super();
  }

  public ClientEndNode(String id) {
    super(id);
  }

  protected ClientEndNode(ClientEndNode<T, M> orig) {
super(orig);
  }

  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  @Override
  public int getMaxSuccessorCount() {
    return 0;
  }

  @Override
  public void setDefines(Collection<? extends IXmlDefineType> defines) {
    super.setDefines(defines);
  }

  @Override
  public Identifiable getPredecessor() {
    ProcessNodeSet<? extends Identifiable> list = getPredecessors();
    return list.isEmpty() ? null : list.get(0);
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
  public void serialize(XmlWriter out) throws XmlException {
    out.startTag(NS_PM, "end", null);
    serializeAttributes(out);
    serializeCommonChildren(out);
    out.endTag(NS_PM, "end", null);
  }

  @Override
  public <R> R visit(ProcessNode.Visitor<R> visitor) {
    return visitor.visitEndNode(this);
  }

  @Override
  public boolean isCompat() {
    return false;
  }
}
