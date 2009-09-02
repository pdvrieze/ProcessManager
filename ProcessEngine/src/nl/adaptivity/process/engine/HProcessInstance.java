package nl.adaptivity.process.engine;

import java.io.Serializable;

import javax.xml.bind.annotation.*;

@XmlRootElement(name="instanceHandle")
@XmlAccessorType(XmlAccessType.NONE)
public final class HProcessInstance implements Serializable {

  
  private static final long serialVersionUID = 8151525146116141232L;
  
  @XmlValue
  private long aHandle;
  
  public HProcessInstance() {
    setHandle(-1);
  }
  
  public HProcessInstance(long pHandle) {
    setHandle(pHandle);
  }

  @Override
  public boolean equals(Object pObj) {
    return (pObj == this) || ((pObj instanceof HProcessInstance) && getHandle() == ((HProcessInstance) pObj).getHandle());
  }

  @Override
  public int hashCode() {
    return (int) getHandle();
  }

  public long getHandle() {
    return aHandle;
  }

  public void setHandle(long handle) {
    aHandle = handle;
  }

}
