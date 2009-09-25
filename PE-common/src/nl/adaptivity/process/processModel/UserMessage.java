package nl.adaptivity.process.processModel;

import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.exec.Task;


@Deprecated
public class UserMessage extends Activity {

  private static final long serialVersionUID = -8184388617635747688L;
  private final String aMessage;
  private final Condition aCondition;

  public UserMessage(ProcessNode pPrevious, Condition pCondition, String pMessage) {
    super (pPrevious);
    aCondition = pCondition;
    aMessage = pMessage;
  }

  public String getUserMessage() {
    return aMessage;
  }

  @Override
  public boolean condition() {
    if (aCondition == null) { return true; }
    return aCondition.eval();
  }

  @Override
  public boolean startTask(IMessageService pMessageService, Task pInstance) {
    // TODO reevaluate this method
    throw new UnsupportedOperationException("not yet implemented");
//    final InternalMessage message = new InternalMessage(pProcessInstance, Payload.create(aMessage));
//    pThreads.add(new ProcessNodeInstance(this, message, Arrays.asList(pPredecessor)));
//    pProcessInstance.fireMessage(message);
  }

}
