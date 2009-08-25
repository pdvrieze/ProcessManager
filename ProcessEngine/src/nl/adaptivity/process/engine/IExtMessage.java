package nl.adaptivity.process.engine;

import java.io.Serializable;


public interface IExtMessage extends Serializable {

  boolean isValidReply(IExtMessage pMessage);

  long getHandle();

  long getReplyTo();

  long getProcessInstanceHandle();

  Payload getPayload();

}
