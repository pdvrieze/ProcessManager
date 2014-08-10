package nl.adaptivity.process.clientProcessModel;

import static nl.adaptivity.process.clientProcessModel.ClientProcessModel.NS_PM;

import java.util.List;

import nl.adaptivity.process.processModel.IXmlResultType;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.StartNode;

public class ClientStartNode<T extends IClientProcessNode<T>> extends ClientProcessNode<T> implements StartNode<T> {

  public ClientStartNode() {
    super();
  }

  public ClientStartNode(final String pId) {
    super(pId);
  }

  protected ClientStartNode(final ClientStartNode<T> pOrig) {
    super(pOrig);
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
  public void serialize(SerializerAdapter pOut) {
    pOut.startTag(NS_PM, "start", true);
    serializeCommonAttrs(pOut);
    serializeCommonChildren(pOut);
    pOut.endTag(NS_PM, "start", true);
  }

  @Override
  public <R> R visit(ProcessNode.Visitor<R> pVisitor) {
    return pVisitor.visitStartNode(this);
  }

}
