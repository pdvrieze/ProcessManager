package nl.adaptivity.process.clientProcessModel;


import nl.adaptivity.process.processModel.JoinSplit;


public abstract class ClientJoinSplit<T extends IClientProcessNode<T>> extends ClientProcessNode<T> implements JoinSplit<T>{

  private int aMin=-1;
  private int aMax=-1;

  public ClientJoinSplit(ClientProcessModel<T> pOwner) {
    super(pOwner);
  }

  public ClientJoinSplit(String pId, ClientProcessModel<T> pOwner) {
    super(pId, pOwner);
  }

  public ClientJoinSplit(ClientJoinSplit<T> pOrig) {
    super(pOrig);
    aMin = pOrig.aMin;
    aMax = pOrig.aMax;
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