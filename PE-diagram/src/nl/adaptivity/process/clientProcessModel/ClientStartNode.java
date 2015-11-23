package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.StartNode;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlWriter;

import javax.xml.namespace.QName;

import static nl.adaptivity.process.clientProcessModel.ClientProcessModel.NS_PM;

public class ClientStartNode<T extends IClientProcessNode<T>> extends ClientProcessNode<T> implements StartNode<T> {

  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  public ClientStartNode(final boolean compat) {
    super(compat);
  }

  public ClientStartNode(final String id, final boolean compat) {
    super(id, compat);
  }

  protected ClientStartNode(final ClientStartNode<T> orig, final boolean compat) {
    super(orig, compat);
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

}
