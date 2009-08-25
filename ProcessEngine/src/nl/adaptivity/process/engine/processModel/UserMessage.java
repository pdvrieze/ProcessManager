package nl.adaptivity.process.engine.processModel;

import java.util.Collection;

import nl.adaptivity.process.engine.Message;
import nl.adaptivity.process.engine.ProcessInstance;


public class UserMessage extends Activity {

  private static final long serialVersionUID = -8184388617635747688L;
  private final String aMessage;
  private final Condition aCondition;

  public UserMessage(ProcessNode pPrevious, Condition pCondition, String pMessage) {
    super (pPrevious);
    aCondition = pCondition;
    aMessage = pMessage;
  }

  public String getMessage() {
    return aMessage;
  }

  @Override
  public boolean condition() {
    if (aCondition == null) { return true; }
    return aCondition.eval();
  }

  @Override
  public void start(Collection<ProcessNodeInstance> pThreads, ProcessInstance pProcessInstance) {
    final Message message = new Message(pProcessInstance, aMessage);
    long msgHandle = pProcessInstance.getEngine().ensureMessageHandle(message);
    pThreads.add(new ProcessNodeInstance(this, msgHandle));
    pProcessInstance.fireMessage(message);
  }

}
