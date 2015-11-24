package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.StartNode;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlWriter;

import javax.xml.namespace.QName;

import static nl.adaptivity.process.clientProcessModel.ClientProcessModel.NS_PM;

public class ClientStartNode<T extends IClientProcessNode<T>> extends ClientProcessNode<T> implements StartNode<T>, IClientProcessNode<T> {

  private final boolean mCompat;

  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  public ClientStartNode(final boolean compat) {
    super();
    mCompat = compat;
  }

  public ClientStartNode(final String id, final boolean compat) {
    super(id);
    mCompat = compat;
  }

  protected ClientStartNode(final ClientStartNode<T> orig, final boolean compat) {
    super(orig);
    mCompat = compat;
  }

  @Override
  public int getMaxPredecessorCount() {
    return 0;
  }

  @Override
  public void serialize(XmlWriter out) throws XmlException {
    out.startTag(NS_PM, "start", null);
    serializeAttributes(out);
    serializeCommonChildren(out);
    out.endTag(NS_PM, "start", null);
  }

  @Override
  public <R> R visit(ProcessNode.Visitor<R> visitor) {
    return visitor.visitStartNode(this);
  }

  @Override
  public int getMaxSuccessorCount() {
    return isCompat() ? Integer.MAX_VALUE : 1;
  }

  @Override
  public boolean isCompat() {
    return mCompat;
  }
}
