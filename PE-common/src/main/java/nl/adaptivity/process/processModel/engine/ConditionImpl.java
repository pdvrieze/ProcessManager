package nl.adaptivity.process.processModel.engine;

import java.io.Serializable;

import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.Condition;
import nl.adaptivity.util.xml.XmlSerializable;
import nl.adaptivity.util.xml.XmlUtil;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
 * Class encapsulating a condition.
 *
 * @author Paul de Vrieze
 */
public class ConditionImpl implements XmlSerializable, Serializable, Condition {

  private static final long serialVersionUID = -4361822049137881021L;
  public static final String ELEMENTLOCALNAME = "condition";

  private final String aCondition;

  public ConditionImpl(final String pCondition) {
    aCondition = pCondition;
  }

  @Override
  public void serialize(final XMLStreamWriter out) throws XMLStreamException {
    XmlUtil.writeSimpleElement(out, new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX), getCondition());
  }

  public static ConditionImpl deserialize(final XMLStreamReader pIn) throws XMLStreamException {
    String condition = XmlUtil.readSimpleElement(pIn);
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
   * @param pInstance The instance to use to evaluate against.
   * @return <code>true</code>
   */
  public boolean eval(final IProcessNodeInstance<?> pInstance) {
    return true;
  }

}
