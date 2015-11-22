package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.Split;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlWriter;


/**
 * A split node for clients. Note that ClientSplitNodes don't have a compat mode. They have multiple succesors
 * and compatibility concerns their absense.
 *
 * @param <T> The type of ProcessNode used.
 */
public class ClientSplitNode<T extends IClientProcessNode<T>> extends ClientJoinSplit<T> implements Split<T> {

  public ClientSplitNode() {
    super(false);
  }

  public ClientSplitNode(String id) {
    super(id, false);
  }

  protected ClientSplitNode(ClientJoinSplit<T> orig) {
    super(orig, false);
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
