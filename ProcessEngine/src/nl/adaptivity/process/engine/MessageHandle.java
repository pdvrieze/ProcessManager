package nl.adaptivity.process.engine;

import java.io.Serializable;


public final class MessageHandle implements Serializable {
  
  private static final long serialVersionUID = 4495439180153379936L;
  private final long aHandle;
  
  public MessageHandle(long pHandle) {
    aHandle = pHandle;
  }

  @Override
  public boolean equals(Object pObj) {
    return (pObj == this) || ((pObj instanceof MessageHandle) && aHandle == ((MessageHandle) pObj).aHandle);
  }

  @Override
  public int hashCode() {
    return (int) aHandle;
  }

  public long getHandle() {
    return aHandle;
  }

}
