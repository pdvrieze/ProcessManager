package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.Transaction;
import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.Condition;
import nl.adaptivity.util.xml.XmlSerializable;
import nl.adaptivity.util.xml.XmlUtil;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
 * Class encapsulating a condition.
 *
 * @author Paul de Vrieze
 */
public class ConditionImpl implements XmlSerializable, Condition {

  public static final String ELEMENTLOCALNAME = "condition";

  private final String aCondition;

  public ConditionImpl(final String condition) {
    aCondition = condition;
  }

  @Override
  public void serialize(@NotNull final XMLStreamWriter out) throws XMLStreamException {
    XmlUtil.writeSimpleElement(out, new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX), getCondition());
  }

  @NotNull
  public static ConditionImpl deserialize(@NotNull final XMLStreamReader in) throws XMLStreamException {
    final String condition = XmlUtil.readSimpleElement(in);
    return new ConditionImpl(condition);
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.engine.Condition#getCondition()
   */
  @Override
  public String getCondition() {
    return aCondition;
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
