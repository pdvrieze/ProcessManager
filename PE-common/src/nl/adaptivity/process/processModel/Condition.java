package nl.adaptivity.process.processModel;

import java.io.Serializable;

import nl.adaptivity.process.exec.Task;

/**
 * Class encapsulating a condition.
 * @author Paul de Vrieze
 *
 */
public class Condition implements Serializable{

  private static final long serialVersionUID = -4361822049137881021L;
  private String aCondition;

  public Condition(String pCondition) {
    aCondition = pCondition;
  }

  /**
   * Evaluate the condition.
   * @param pInstance The instance to use to evaluate against.
   * @return <code>true</code>
   */
  public boolean eval(Task<?> pInstance) {
    return true;
  }

}
