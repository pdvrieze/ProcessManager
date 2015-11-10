package nl.adaptivity.process.engine;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import net.devrieze.util.HandleMap.Handle;


@XmlRootElement(name = "instanceHandle")
@XmlAccessorType(XmlAccessType.NONE)
public final class HProcessInstance implements Handle<ProcessInstance>, Serializable {


  private static final long serialVersionUID = 8151525146116141232L;

  @XmlValue
  private long mHandle;

  public HProcessInstance() {
    setHandle(-1);
  }

  public HProcessInstance(final long handle) {
    setHandle(handle);
  }

  @Override
  public boolean equals(final Object obj) {
    return (obj == this) || ((obj instanceof HProcessInstance) && (getHandle() == ((HProcessInstance) obj).getHandle()));
  }

  @Override
  public int hashCode() {
    return (int) getHandle();
  }

  @Override
  public long getHandle() {
    return mHandle;
  }

  public void setHandle(final long handle) {
    mHandle = handle;
  }

}
