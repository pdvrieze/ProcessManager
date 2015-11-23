package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.Join;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlWriter;

import javax.xml.namespace.QName;


public class ClientJoinNode<T extends IClientProcessNode<T>> extends ClientJoinSplit<T> implements Join<T> {

  public ClientJoinNode(final boolean compat) {
    super(compat);
  }

  public ClientJoinNode(String id, final boolean compat) {
    super(id, compat);
  }

  protected ClientJoinNode(ClientJoinSplit<T> orig, final boolean compat) {
    super(orig, compat);
  }

  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  @Override
  public int getMaxPredecessorCount() {
    return Integer.MAX_VALUE;
  }

  @Override
  public void serialize(XmlWriter out) throws XmlException {
    serializeJoin(out);
  }

  @Override
  public <R> R visit(ProcessNode.Visitor<R> visitor) {
    return visitor.visitJoin(this);
  }

}
