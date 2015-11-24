package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.Split;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlWriter;

import javax.xml.namespace.QName;


/**
 * A split node for clients. Note that ClientSplitNodes don't have a compat mode. They have multiple succesors
 * and compatibility concerns their absense.
 *
 * @param <T> The type of ProcessNode used.
 */
public class ClientSplitNode<T extends IClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends ClientJoinSplit<T, M> implements Split<T, M> {

  public ClientSplitNode() {
    super();
  }

  public ClientSplitNode(String id) {
    super(id);
  }

  protected ClientSplitNode(ClientJoinSplit<T, M> orig) {
    super(orig);
  }

  @Override
  public QName getElementName() {
    return ELEMENTNAME;
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

  @Override
  public boolean isCompat() {
    return false;
  }
}
