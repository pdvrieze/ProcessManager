package nl.adaptivity.process.processModel.engine;

import java.io.Serializable;

import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.Condition;


/**
 * Class encapsulating a condition.
 *
 * @author Paul de Vrieze
 */
public class ConditionImpl implements Serializable, Condition {

  private static final long serialVersionUID = -4361822049137881021L;

  private final String aCondition;

  public ConditionImpl(final String pCondition) {
    aCondition = pCondition;
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
