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

  private String mName;

  private long mHandle;

  @Nullable private UUID mUuid;

  public ProcessModelRef() {}

  public ProcessModelRef(final String name, final long handle, final UUID uuid) {
    mName = name;
    mHandle = handle;
    mUuid = uuid;
  }

  public ProcessModelRef(@NotNull final IProcessModelRef<?> source) {
    mHandle = source.getHandle();
    mName = source.getName();
    mUuid = source.getUuid();
  }

  @NotNull
  public static ProcessModelRef get(final IProcessModelRef<?> src) {
    if (src instanceof ProcessModelRef) { return (ProcessModelRef) src; }
    return new ProcessModelRef(src);
  }

  void setName(final String name) {
    mName = name;
  }

  @Override
  @XmlAttribute(required = true)
  public String getName() {
    return mName;
  }

  void setHandle(final long handle) {
    mHandle = handle;
  }

  @Override
  @XmlAttribute(required = true)
  public long getHandle() {
    return mHandle;
  }

  @Nullable
  @Override
  public UUID getUuid() {
    return mUuid;
  }

  @Nullable
  @XmlAttribute(name="uuid")
  String getXmlUuid() {
    return mUuid==null ? null : mUuid.toString();
  }

  void setXmlUuid(@Nullable final String uuid) {
    mUuid = uuid==null ? null : UUID.fromString(uuid);
  }

}
