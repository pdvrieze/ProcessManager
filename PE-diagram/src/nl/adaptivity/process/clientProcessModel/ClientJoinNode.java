package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.Join;


public class ClientJoinNode<T extends IClientProcessNode<T>> extends ClientJoinSplit<T> implements Join<T> {

  public ClientJoinNode(ClientProcessModel<T> pOwner) {
    super(pOwner);
  }

  public ClientJoinNode(String pId, ClientProcessModel<T> pOwner) {
    super(pId, pOwner);
  }

  protected ClientJoinNode(ClientJoinNode<T> pOrig) {
    super(pOrig);
  }

  @Override
  public int getMaxPredecessorCount() {
    return Integer.MAX_VALUE;
  }

}
