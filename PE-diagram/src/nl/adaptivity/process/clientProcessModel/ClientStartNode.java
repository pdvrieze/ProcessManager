package nl.adaptivity.process.clientProcessModel;

import java.util.List;

import nl.adaptivity.process.processModel.IXmlImportType;
import nl.adaptivity.process.processModel.StartNode;

public class ClientStartNode<T extends IClientProcessNode<T>> extends ClientProcessNode<T> implements StartNode<T> {

  public ClientStartNode(ClientProcessModel<T> pOwner) {
    super(pOwner);
  }

  public ClientStartNode(final String pId, ClientProcessModel<T> pOwner) {
    super(pId, pOwner);
  }

  protected ClientStartNode(final ClientStartNode<T> pOrig) {
    super(pOrig);
  }

  @Override
  public List<IXmlImportType> getImports() {
    return super.getImports();
  }


  @Override
  public int getMaxPredecessorCount() {
    return 0;
  }

}
