package nl.adaptivity.process.clientProcessModel;

import net.devrieze.util.StringUtil;
import nl.adaptivity.process.processModel.Activity;
import nl.adaptivity.process.processModel.ActivityBase;
import nl.adaptivity.process.processModel.Condition;
import nl.adaptivity.process.processModel.ProcessModelBase;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlWriter;


public class ClientActivityNode<T extends IClientProcessNode<T>> extends ActivityBase<T> implements IClientProcessNode<T> {

  private final boolean mCompat;
  private String mCondition;

  public ClientActivityNode(final ProcessModelBase<T> owner, final boolean compat) {
    super(owner);
    mCompat = compat;
  }


  public ClientActivityNode(final ProcessModelBase<T> owner, String id, final boolean compat) {
    super(owner);
    setId(id);
    mCompat = compat;
  }

  protected ClientActivityNode(Activity<?> orig, final boolean compat) {
    super(orig);
    mCompat = compat;
  }

  @Override
  public String getCondition() {
    return mCondition;
  }

  @Override
  public void setCondition(String condition) {
    mCondition = condition;
  }

  @Override
  protected void deserializeCondition(final XmlReader in) throws XmlException {
    mCondition = StringUtil.toString(XmlUtil.readSimpleElement(in));
  }

  @Override
  protected void serializeCondition(final XmlWriter out) throws XmlException {
    if (mCondition!=null && mCondition.length()>0) {
      XmlUtil.writeSimpleElement(out, Condition.ELEMENTNAME, mCondition);
    }
  }

  @Override
  public int getMaxSuccessorCount() {
    return isCompat() ? Integer.MAX_VALUE : 1;
  }

  @Override
  public boolean isCompat() {
    return mCompat;
  }
}
