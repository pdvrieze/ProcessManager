package nl.adaptivity.process.processModel.engine;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;


@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "processModel")
public class ProcessModelRef implements IProcessModelRef<ProcessNodeImpl> {

  private String aName;

  private long aHandle;

  public ProcessModelRef() {}

  public ProcessModelRef(final String pName, final long pHandle) {
    aName = pName;
    aHandle = pHandle;
  }

  public ProcessModelRef(IProcessModelRef<?> pSource) {
    aHandle = pSource.getHandle();
    aName = pSource.getName();
  }

  public static ProcessModelRef get(IProcessModelRef<?> src) {
    if (src instanceof ProcessModelRef) { return (ProcessModelRef) src; }
    return new ProcessModelRef(src);
  }

  void setName(final String name) {
    aName = name;
  }

  @Override
  @XmlAttribute(required = true)
  public String getName() {
    return aName;
  }

  void setHandle(final long handle) {
    aHandle = handle;
  }

  @Override
  @XmlAttribute(required = true)
  public long getHandle() {
    return aHandle;
  }
}
