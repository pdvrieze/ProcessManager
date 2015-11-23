package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.ProcessNodeBase;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlWriter;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;


public abstract class ClientProcessNode<T extends IClientProcessNode<T>> extends ProcessNodeBase<T> implements IClientProcessNode<T>{

  private final boolean mCompat;


  protected ClientProcessNode(final boolean compat) {
    this((String) null, compat);
  }

  protected ClientProcessNode(final String id, final boolean compat) {
    super(null);
    setId(id);
    mCompat = compat;
  }

  protected ClientProcessNode(final ClientProcessNode<T> orig, final boolean compat) {
    this(orig.getId(), compat);
  }

  @Override
  public void disconnect() {
    final T me = this.asT();
    for (Iterator<? extends Identifiable> it = getPredecessors().iterator(); it.hasNext(); ) {
      Identifiable pred = it.next();
      it.remove(); // Remove first, otherwise we get strange iterator concurrent modification effects.
      getOwnerModel().getNode(pred).removeSuccessor(me);
    }
    for (Iterator<? extends Identifiable> it = getSuccessors().iterator(); it.hasNext(); ) {
      Identifiable sucId = it.next();
      it.remove();
      T suc = getOwnerModel().getNode(sucId);
      if (suc != null) { suc.removePredecessor(me); }
    }
  }

  @Override
  public int getMaxPredecessorCount() {
    return 1;
  }

  @Override
  public int getMaxSuccessorCount() {
    return mCompat ? Integer.MAX_VALUE : 1;
  }

  public void unsetPos() {
    setX(Double.NaN);
    setY(Double.NaN);
  }

  @Nullable
  public ClientProcessModel<T> getOwnerModel() {
    return (ClientProcessModel) super.getOwnerModel();
  }

  public boolean hasPos() {
    return !Double.isNaN(getX()) && !Double.isNaN(getY());
  }

  public boolean isCompat() {
    return mCompat;
  }

  @Override
  public void setX(double x) {
    super.setX(x);
    notifyChange();
  }

  @Override
  public void setY(double y) {
    super.setY(y);
    notifyChange();
  }

  public void offset(final int offsetX, final int offsetY) {
    setX(getX()+ offsetX);
    setY(getY()+ offsetY);
    notifyChange();
  }

  protected void notifyChange() {if (getOwnerModel() != null) getOwnerModel().nodeChanged(this.asT());}

  @Override
  public void setLabel(final String label) {
    super.setLabel(label);
    notifyChange();
  }

  @Override
  public void setId(final String id) {
    super.setId(id);
    notifyChange();
  }

  @Override
  public String toString() {
    String nm = getClass().getSimpleName();
    if (nm.startsWith("Client")) { nm = nm.substring(6); }
    if (nm.startsWith("Drawable")) { nm = nm.substring(8); }
    if (nm.endsWith("Node")) { nm = nm.substring(0, nm.length()-4); }

    return nm+"[id=" + getId() + '(' + getX() + ", " + getY() + ")";
  }

  @SuppressWarnings("unchecked")
  public T asT() {
    return (T) this;
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
