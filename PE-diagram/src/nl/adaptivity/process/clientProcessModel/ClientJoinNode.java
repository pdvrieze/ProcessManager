package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.Join;


public class ClientJoinNode<T extends IClientProcessNode<T>> extends ClientProcessNode<T> implements Join<T> {

  private int aMin;

  private int aMax;

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

  @Override
  public void setMax(int pMax) {
    aMax = pMax;
  }

  @Override
  public int getMax() {
    return aMax;
  }

  @Override
  public void setMin(int pMin) {
    aMin = pMin;
  }

  @Override
  public int getMin() {
    return aMin;
  }

}
