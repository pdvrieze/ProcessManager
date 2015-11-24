package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.Join;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlWriter;

import javax.xml.namespace.QName;


public class ClientJoinNode<T extends IClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends ClientJoinSplit<T, M> implements Join<T, M> {

  private final boolean mCompat;

  public ClientJoinNode(final boolean compat) {
    super();
    mCompat = compat;
  }

  public ClientJoinNode(String id, final boolean compat) {
    super(id);
    mCompat = compat;
  }

  protected ClientJoinNode(ClientJoinSplit<T, M> orig, final boolean compat) {
    super(orig);
    mCompat = compat;
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

  @Override
  public boolean isCompat() {
    return mCompat;
  }
}
