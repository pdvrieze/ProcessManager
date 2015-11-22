package nl.adaptivity.process.clientProcessModel;


import nl.adaptivity.process.processModel.JoinSplit;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlWriter;

import static nl.adaptivity.process.clientProcessModel.ClientProcessModel.NS_PM;


public abstract class ClientJoinSplit<T extends IClientProcessNode<T>> extends ClientProcessNode<T> implements JoinSplit<T>{

  private int mMin=-1;
  private int mMax=-1;

  public ClientJoinSplit(final boolean compat) {
    super(compat);
  }

  public ClientJoinSplit(String id, final boolean compat) {
    super(id, compat);
  }

  public ClientJoinSplit(ClientJoinSplit<T> orig, final boolean compat) {
    super(orig, compat);
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
  public void serializeCommonAttrs(XmlWriter out) throws XmlException {
    super.serializeCommonAttrs(out);
    if (mMin>=0) { out.attribute(null, "min", null, Integer.toString(mMin)); }
    if (mMax>=0) {
      out.attribute(null, "max", null, Integer.toString(mMax));
    }
  }

  protected void serializeSplit(XmlWriter out) throws XmlException {
    out.startTag(NS_PM, "split", null);
    serializeCommonAttrs(out);
    serializeCommonChildren(out);
    out.endTag(NS_PM, "split", null);
  }

  protected void serializeJoin(XmlWriter out) throws XmlException {
    out.startTag(NS_PM, "join", null);
    serializeCommonAttrs(out);
    serializeCommonChildren(out);
    for(Identifiable predecessor: getPredecessors()) {
      out.startTag(NS_PM, "predecessor", null);
      out.text(predecessor.getId());
      out.endTag(NS_PM, "predecessor", null);
    }
    out.endTag(NS_PM, "join", null);
  }

}