package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.Join;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlWriter;


public class ClientJoinNode<T extends IClientProcessNode<T>> extends ClientJoinSplit<T> implements Join<T> {

  public ClientJoinNode() {
    super();
  }

  public ClientJoinNode(String id) {
    super(id);
  }

  protected ClientJoinNode(ClientJoinSplit<T> orig) {
    super(orig);
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
