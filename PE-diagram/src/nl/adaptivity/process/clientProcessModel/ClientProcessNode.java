package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.ProcessNodeBase;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlWriter;


public abstract class ClientProcessNode<T extends IClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends ProcessNodeBase<T, M> {


  protected ClientProcessNode() {
    this((String) null);
  }

  protected ClientProcessNode(final String id) {
    super((M) null);
    setId(id);
  }

  protected ClientProcessNode(final ClientProcessNode<T, M> orig) {
    this(orig.getId());
  }

  @Override
  public String toString() {
    String nm = getClass().getSimpleName();
    if (nm.startsWith("Client")) { nm = nm.substring(6); }
    if (nm.startsWith("Drawable")) { nm = nm.substring(8); }
    if (nm.endsWith("Node")) { nm = nm.substring(0, nm.length()-4); }

    return nm+"[id=" + getId() + '(' + getX() + ", " + getY() + ")";
  }

  public void serializeAttributes(XmlWriter out) throws XmlException {
    super.serializeAttributes(out);
    if (getMaxPredecessorCount()==1 && getPredecessors().size()==1) {
      out.attribute(null, "predecessor", null, getPredecessors().get(0).getId());
    }
  }

  public void serializeCommonChildren(XmlWriter out) {
    // TODO handle imports and exports.
  }

}
