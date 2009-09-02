package nl.adaptivity.process.engine.processModel;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.NONE)
public class ProcessModelRef {

  private String aName;
  private long aHandle;
  
  public void setName(String name) {
    aName = name;
  }
  
  @XmlAttribute(required=true)
  public String getName() {
    return aName;
  }
  
  public void setHandle(long handle) {
    aHandle = handle;
  }
  
  @XmlAttribute(required=true)
  public long getHandle() {
    return aHandle;
  }
}
