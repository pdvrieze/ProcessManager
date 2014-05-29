package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.Join;


public class ClientJoinNode<T extends IClientProcessNode<T>> extends ClientJoinSplit<T> implements Join<T> {

  public ClientJoinNode() {
    super();
  }

  public ClientJoinNode(String pId) {
    super(pId);
  }

  protected ClientJoinNode(ClientJoinSplit<T> pOrig) {
    super(pOrig);
  }

  @Override
  public int getMaxPredecessorCount() {
    return Integer.MAX_VALUE;
  }

  @Override
  public void serialize(SerializerAdapter pOut) {
    serializeJoin(pOut);
  }

}
