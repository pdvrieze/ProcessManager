package nl.adaptivity.process.engine;

import java.io.Serializable;


@Deprecated
public interface IExtMessage extends Serializable {

  long getHandle();

  long getReplyTo();

  long getProcessInstanceHandle();

  Payload getPayload();

}
