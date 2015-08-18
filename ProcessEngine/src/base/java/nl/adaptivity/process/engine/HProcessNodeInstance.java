package nl.adaptivity.process.engine;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import net.devrieze.util.HandleMap.Handle;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;


@XmlRootElement(name = "instanceHandle")
@XmlAccessorType(XmlAccessType.NONE)
public final class HProcessNodeInstance implements Handle<ProcessNodeInstance>, Serializable {


  private static final long serialVersionUID = 8151525146116141232L;

  @XmlValue
  private long aHandle;

  public HProcessNodeInstance() {
    setHandle(-1);
  }

  public HProcessNodeInstance(final long pHandle) {
    setHandle(pHandle);
  }

  @Override
  public boolean equals(final Object pObj) {
    return (pObj == this) || ((pObj instanceof HProcessNodeInstance) && (getHandle() == ((HProcessNodeInstance) pObj).getHandle()));
  }

  @Override
  public int hashCode() {
    return (int) getHandle();
  }

  @Override
  public long getHandle() {
    return aHandle;
  }

  public void setHandle(final long handle) {
    aHandle = handle;
  }

}
