package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.IXmlResultType;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.StartNode;

import java.util.List;

import static nl.adaptivity.process.clientProcessModel.ClientProcessModel.NS_PM;

public class ClientStartNode<T extends IClientProcessNode<T>> extends ClientProcessNode<T> implements StartNode<T> {

  public ClientStartNode() {
    super();
  }

  public ClientStartNode(final String id) {
    super(id);
  }

  protected ClientStartNode(final ClientStartNode<T> orig) {
    super(orig);
  }

  @Override
  public List<IXmlResultType> getResults() {
    return super.getResults();
  }


  @Override
  public int getMaxPredecessorCount() {
    return 0;
  }

  @Override
  public void serialize(SerializerAdapter out) {
    out.startTag(NS_PM, "start", true);
    serializeCommonAttrs(out);
    serializeCommonChildren(out);
    out.endTag(NS_PM, "start", true);
  }

  @Override
  public <R> R visit(ProcessNode.Visitor<R> visitor) {
    return visitor.visitStartNode(this);
  }

}
