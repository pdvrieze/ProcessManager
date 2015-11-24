package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.ProcessModelBase;
import nl.adaptivity.process.processModel.ProcessNodeBase;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlWriter;
import org.jetbrains.annotations.Nullable;


public abstract class ClientProcessNode<T extends IClientProcessNode<T>> extends ProcessNodeBase<T> {


  protected ClientProcessNode() {
    this((String) null);
  }

  protected ClientProcessNode(final String id) {
    super((ProcessModelBase<T>) null);
    setId(id);
  }

  protected ClientProcessNode(final ClientProcessNode<T> orig) {
    this(orig.getId());
  }

  @Nullable
  public ClientProcessModel<T> getOwnerModel() {
    return (ClientProcessModel<T>) super.getOwnerModel();
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
