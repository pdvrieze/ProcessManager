package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.Join;


public class ClientSplitNode<T extends IClientProcessNode<T>> extends ClientJoinSplit<T> implements Join<T> {

  public ClientSplitNode(ClientProcessModel<T> pOwner) {
    super(pOwner);
  }

  public ClientSplitNode(String pId, ClientProcessModel<T> pOwner) {
    super(pId, pOwner);
  }

  protected ClientSplitNode(ClientJoinNode<T> pOrig) {
    super(pOrig);
  }

  @Override
  public int getMaxSuccessorCount() {
    return Integer.MAX_VALUE;
  }

}
