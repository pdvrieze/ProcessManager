package nl.adaptivity.process.processModel.engine;

import java.util.UUID;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import net.devrieze.util.HandleMap.Handle;

import nl.adaptivity.process.processModel.ProcessModel;


@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "processModel")
public class ProcessModelRef implements IProcessModelRef<ProcessNodeImpl> {

  private String aName;

  private long aHandle;

  private UUID aUuid;

  public ProcessModelRef() {}

  public ProcessModelRef(final String pName, final long pHandle, UUID pUuid) {
    aName = pName;
    aHandle = pHandle;
    aUuid = pUuid;
  }

  public ProcessModelRef(IProcessModelRef<?> pSource) {
    aHandle = pSource.getHandle();
    aName = pSource.getName();
    aUuid = pSource.getUuid();
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

  @Override
  public int compareTo(Handle<ProcessModel<ProcessNodeImpl>> pO) {
    return Long.compare(aHandle, pO.getHandle());
  }

  @Override
  public UUID getUuid() {
    return aUuid;
  }

  @XmlAttribute(name="uuid")
  String getXmlUuid() {
    return aUuid==null ? null : aUuid.toString();
  }

  void setXmlUuid(String pUuid) {
    aUuid = pUuid==null ? null : UUID.fromString(pUuid);
  }

}
