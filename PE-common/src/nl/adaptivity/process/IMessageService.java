package nl.adaptivity.process;

import nl.adaptivity.process.exec.Task;
import nl.adaptivity.process.processModel.XmlMessage;


public interface IMessageService<T> {

  T createMessage(XmlMessage pMessage);

  boolean sendMessage(T pMessage, Task pInstance);
}
