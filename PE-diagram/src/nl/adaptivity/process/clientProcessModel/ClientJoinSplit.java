package nl.adaptivity.process.clientProcessModel;


import nl.adaptivity.process.processModel.JoinSplit;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlWriter;

import static nl.adaptivity.process.clientProcessModel.ClientProcessModel.NS_PM;


public abstract class ClientJoinSplit<T extends IClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends ClientProcessNode<T, M> implements JoinSplit<T, M>, IClientProcessNode<T, M> {

  private int mMin=-1;
  private int mMax=-1;

  public ClientJoinSplit() {
    super();
  }

  public ClientJoinSplit(String id) {
    super(id);
  }

  public ClientJoinSplit(ClientJoinSplit<T, M> orig) {
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
  public void serializeAttributes(XmlWriter out) throws XmlException {
    super.serializeAttributes(out);
    if (mMin>=0) { out.attribute(null, "min", null, Integer.toString(mMin)); }
    if (mMax>=0) {
      out.attribute(null, "max", null, Integer.toString(mMax));
    }
  }

  protected void serializeSplit(XmlWriter out) throws XmlException {
    out.startTag(NS_PM, "split", null);
    serializeAttributes(out);
    serializeCommonChildren(out);
    out.endTag(NS_PM, "split", null);
  }

  protected void serializeJoin(XmlWriter out) throws XmlException {
    out.startTag(NS_PM, "join", null);
    serializeAttributes(out);
    serializeCommonChildren(out);
    for(Identifiable predecessor: getPredecessors()) {
      out.startTag(NS_PM, "predecessor", null);
      out.text(predecessor.getId());
      out.endTag(NS_PM, "predecessor", null);
    }
    out.endTag(NS_PM, "join", null);
  }

  @Override
  public int getMaxSuccessorCount() {
    return isCompat() ? Integer.MAX_VALUE : 1;
  }

}