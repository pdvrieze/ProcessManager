package nl.adaptivity.process.engine;

import java.io.Serializable;


public final class HProcessInstance implements Serializable {

  
  private static final long serialVersionUID = 8151525146116141232L;
  
  private final long aHandle;
  
  public HProcessInstance(long pHandle) {
    aHandle = pHandle;
  }

  @Override
  public boolean equals(Object pObj) {
    return (pObj == this) || ((pObj instanceof HProcessInstance) && aHandle == ((HProcessInstance) pObj).aHandle);
  }

  @Override
  public int hashCode() {
    return (int) aHandle;
  }

  public long getHandle() {
    return aHandle;
  }

}
