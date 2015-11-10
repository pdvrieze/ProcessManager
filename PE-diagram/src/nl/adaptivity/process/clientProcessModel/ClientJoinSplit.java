package nl.adaptivity.process.clientProcessModel;


import nl.adaptivity.process.processModel.JoinSplit;
import nl.adaptivity.process.util.Identifiable;

import static nl.adaptivity.process.clientProcessModel.ClientProcessModel.NS_PM;


public abstract class ClientJoinSplit<T extends IClientProcessNode<T>> extends ClientProcessNode<T> implements JoinSplit<T>{

  private int aMin=-1;
  private int aMax=-1;

  public ClientJoinSplit() {
    super();
  }

  public ClientJoinSplit(String id) {
    super(id);
  }

  public ClientJoinSplit(ClientJoinSplit<T> orig) {
    super(orig);
    aMin = orig.aMin;
    aMax = orig.aMax;
  }

  @Override
  public void setMax(int max) {
    aMax = max;
  }

  @Override
  public int getMax() {
    return aMax;
  }

  @Override
  public void setMin(int min) {
    aMin = min;
  }

  @Override
  public int getMin() {
    return aMin;
  }

  @Override
  public void serializeCommonAttrs(SerializerAdapter out) {
    super.serializeCommonAttrs(out);
    if (aMin>=0) { out.addAttribute(null, "min", Integer.toString(aMin)); }
    if (aMax>=0) { out.addAttribute(null, "max", Integer.toString(aMax)); }
  }

  protected void serializeSplit(SerializerAdapter out) {
    out.startTag(NS_PM, "split", true);
    serializeCommonAttrs(out);
    serializeCommonChildren(out);
    out.endTag(NS_PM, "split", true);
  }

  protected void serializeJoin(SerializerAdapter out) {
    out.startTag(NS_PM, "join", true);
    serializeCommonAttrs(out);
    serializeCommonChildren(out);
    for(Identifiable predecessor: getPredecessors()) {
      out.startTag(NS_PM, "predecessor", false);
      out.text(predecessor.getId());
      out.endTag(NS_PM, "predecessor", true);
    }
    out.endTag(NS_PM, "join", true);
  }

}