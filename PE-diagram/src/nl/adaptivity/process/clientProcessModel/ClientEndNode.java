package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.EndNode;
import nl.adaptivity.process.processModel.IXmlDefineType;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.ProcessNodeSet;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identifier;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlWriter;

import java.util.Collection;
import java.util.List;

import static nl.adaptivity.process.clientProcessModel.ClientProcessModel.NS_PM;


public class ClientEndNode<T extends IClientProcessNode<T>> extends ClientProcessNode<T> implements EndNode<T> {

  public ClientEndNode() {
    super(false);
  }

  public ClientEndNode(String id) {
    super(id, false);
  }

  protected ClientEndNode(ClientEndNode<T> orig) {
super(orig, false);
  }

  @Override
  public int getMaxSuccessorCount() {
    return 0;
  }

  @Override
  public List<IXmlDefineType> getDefines() {
    return super.getDefines();
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
  public void setPredecessor(Identifier predecessor) {
    Identifiable previous = getPredecessor();
    if (previous==null) {
      removePredecessor(previous);
    }
    addPredecessor(predecessor);
  }

  @Override
  public void serialize(XmlWriter out) throws XmlException {
    out.startTag(NS_PM, "end", null);
    serializeCommonAttrs(out);
    serializeCommonChildren(out);
    out.endTag(NS_PM, "end", null);
  }

  @Override
  public <R> R visit(ProcessNode.Visitor<R> visitor) {
    return visitor.visitEndNode(this);
  }

}
