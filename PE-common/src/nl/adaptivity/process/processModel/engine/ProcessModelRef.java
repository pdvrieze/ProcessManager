package nl.adaptivity.process.processModel.engine;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;


@XmlAccessorType(XmlAccessType.NONE)
public class ProcessModelRef implements IProcessModelRef<ProcessNodeImpl> {

  private String aName;

  private long aHandle;

  public ProcessModelRef() {}

  public ProcessModelRef(final String pName, final long pHandle) {
    aName = pName;
    aHandle = pHandle;
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
