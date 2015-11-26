package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.EndNode;
import nl.adaptivity.process.processModel.EndNodeBase;


public class ClientEndNode<T extends ClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends EndNodeBase<T, M> implements EndNode<T, M>, ClientProcessNode<T, M> {

  public ClientEndNode(final M ownerModel) {
    super(ownerModel);
  }

  public ClientEndNode(final M ownerModel, String id) {
    super(ownerModel);
    setId(id);
  }

  protected ClientEndNode(EndNode<?, ?> orig) {
    super(orig);
  }

  @Override
  public boolean isCompat() {
    return false;
  }
}
