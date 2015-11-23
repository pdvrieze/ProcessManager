package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.StringUtil;
import net.devrieze.util.Transaction;
import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.Condition;
import nl.adaptivity.util.xml.XmlSerializable;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlWriter;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;


/**
 * Class encapsulating a condition.
 *
 * @author Paul de Vrieze
 */
public class ConditionImpl implements XmlSerializable, Condition {

  private final String mCondition;

  public ConditionImpl(final String condition) {
    mCondition = condition;
  }

  @Override
  public void serialize(@NotNull final XmlWriter out) throws XmlException {
    XmlUtil.writeSimpleElement(out, new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX), getCondition());
  }

  @NotNull
  public static ConditionImpl deserialize(@NotNull final XmlReader in) throws XmlException {
    final CharSequence condition = XmlUtil.readSimpleElement(in);
    return new ConditionImpl(StringUtil.toString(condition));
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.engine.Condition#getCondition()
   */
  @Override
  public String getCondition() {
    return mCondition;
  }

  /**
   * Evaluate the condition.
   *
   * @param transaction The transaction to use for reading state
   * @param instance The instance to use to evaluate against.
   * @return <code>true</code> if the condition holds, <code>false</code> if not
   */
  public boolean eval(final Transaction transaction, final IProcessNodeInstance<?> instance) {
    // TODO process the condition as xpath, expose the node's defines as variables
    return true;
  }

}
