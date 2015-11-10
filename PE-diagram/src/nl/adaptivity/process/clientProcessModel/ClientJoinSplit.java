package nl.adaptivity.process.clientProcessModel;


import nl.adaptivity.process.processModel.JoinSplit;
import nl.adaptivity.process.util.Identifiable;

import static nl.adaptivity.process.clientProcessModel.ClientProcessModel.NS_PM;


public abstract class ClientJoinSplit<T extends IClientProcessNode<T>> extends ClientProcessNode<T> implements JoinSplit<T>{

  private int mMin=-1;
  private int mMax=-1;

  public ClientJoinSplit() {
    super();
  }

  public ClientJoinSplit(String id) {
    super(id);
  }

  public ClientJoinSplit(ClientJoinSplit<T> orig) {
    super(orig);
    mMin = orig.mMin;
    mMax = orig.mMax;
  }

  @Override
  public void setMax(int max) {
    mMax = max;
  }

  @Override
  public int getMax() {
    return mMax;
  }

  @Override
  public void setMin(int min) {
    mMin = min;
  }

  @Override
  public int getMin() {
    return mMin;
  }

  @Override
  public void serializeCommonAttrs(SerializerAdapter out) {
    super.serializeCommonAttrs(out);
    if (mMin>=0) { out.addAttribute(null, "min", Integer.toString(mMin)); }
    if (mMax>=0) { out.addAttribute(null, "max", Integer.toString(mMax)); }
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