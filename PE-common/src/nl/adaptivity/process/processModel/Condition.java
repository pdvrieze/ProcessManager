package nl.adaptivity.process.processModel;

import java.io.Serializable;

import nl.adaptivity.process.exec.Task;


/**
 * Class encapsulating a condition.
 *
 * @author Paul de Vrieze
 */
public class Condition implements Serializable {

  private static final long serialVersionUID = -4361822049137881021L;

  private final String aCondition;

  public Condition(final String pCondition) {
    aCondition = pCondition;
  }

  /**
   * Evaluate the condition.
   * 
   * @param pInstance The instance to use to evaluate against.
   * @return <code>true</code>
   */
  public boolean eval(final Task<?> pInstance) {
    return true;
  }

}
