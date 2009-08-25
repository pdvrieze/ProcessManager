package nl.adaptivity.process.engine;

import java.io.Serializable;


public interface IMessage extends Serializable {

  boolean isValidReply(IMessage pMessage);

  IProcessCursor getCursor();

  long getHandle();

  long getReplyTo();

  long getProcessInstanceHandle();

}
