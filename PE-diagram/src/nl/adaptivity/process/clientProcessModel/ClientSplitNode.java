package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.Join;


public class ClientSplitNode<T extends IClientProcessNode<T>> extends ClientJoinSplit<T> implements Join<T> {

  public ClientSplitNode() {
    super();
  }

  public ClientSplitNode(String pId) {
    super(pId);
  }

  protected ClientSplitNode(ClientJoinSplit<T> pOrig) {
    super(pOrig);
  }

  @Override
  public int getMaxSuccessorCount() {
    return Integer.MAX_VALUE;
  }

  @Override
  public void serialize(SerializerAdapter pOut) {
    serializeSplit(pOut);
  }

}
