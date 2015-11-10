package nl.adaptivity.process.processModel.engine;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;


@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "processModel")
public class ProcessModelRef implements IProcessModelRef<ProcessNodeImpl> {

  private String aName;

  private long aHandle;

  @Nullable private UUID aUuid;

  public ProcessModelRef() {}

  public ProcessModelRef(final String name, final long handle, final UUID uuid) {
    aName = name;
    aHandle = handle;
    aUuid = uuid;
  }

  public ProcessModelRef(@NotNull final IProcessModelRef<?> source) {
    aHandle = source.getHandle();
    aName = source.getName();
    aUuid = source.getUuid();
  }

  @NotNull
  public static ProcessModelRef get(final IProcessModelRef<?> src) {
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

  @Nullable
  @Override
  public UUID getUuid() {
    return aUuid;
  }

  @Nullable
  @XmlAttribute(name="uuid")
  String getXmlUuid() {
    return aUuid==null ? null : aUuid.toString();
  }

  void setXmlUuid(@Nullable final String uuid) {
    aUuid = uuid==null ? null : UUID.fromString(uuid);
  }

}
