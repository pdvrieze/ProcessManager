package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.Split;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlWriter;


public class ClientSplitNode<T extends IClientProcessNode<T>> extends ClientJoinSplit<T> implements Split<T> {

  public ClientSplitNode() {
    super();
  }

  public ClientSplitNode(String id) {
    super(id);
  }

  protected ClientSplitNode(ClientJoinSplit<T> orig) {
    super(orig);
  }

  @Override
  public int getMaxSuccessorCount() {
    return Integer.MAX_VALUE;
  }

  @Override
  public void serialize(XmlWriter out) throws XmlException {
    serializeSplit(out);
  }

  @Override
  public <R> R visit(ProcessNode.Visitor<R> visitor) {
    return visitor.visitSplit(this);
  }

}
