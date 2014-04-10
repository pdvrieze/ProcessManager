package nl.adaptivity.process.clientProcessModel;


import static nl.adaptivity.process.clientProcessModel.ClientProcessModel.NS_PM;
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

  @Override
  public void serializeCommonAttrs(SerializerAdapter pOut) {
    super.serializeCommonAttrs(pOut);
    if (aMin>=0) { pOut.addAttribute("min", Integer.toString(aMin)); }
    if (aMax>=0) { pOut.addAttribute("max", Integer.toString(aMax)); }
  }

  protected void serializeSplit(SerializerAdapter pOut) {
    pOut.startTag(NS_PM, "split", true);
    serializeCommonAttrs(pOut);
    serializeCommonChildren(pOut);
    pOut.endTag(NS_PM, "split", true);
  }

  protected void serializeJoin(SerializerAdapter pOut) {
    pOut.startTag(NS_PM, "join", true);
    serializeCommonAttrs(pOut);
    serializeCommonChildren(pOut);
    for(T predecessor: getPredecessors()) {
      pOut.ignorableWhitespace("\n    ");
      pOut.startTag(NS_PM, "predecessor", true);
      pOut.text(predecessor.getId());
      pOut.endTag(NS_PM, "predecessor", true);
    }
    pOut.ignorableWhitespace("\n  ");
    pOut.endTag(NS_PM, "join", true);
  }

}