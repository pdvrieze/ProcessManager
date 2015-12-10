package nl.adaptivity.process.processModel.engine;

import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.process.processModel.ProcessModel;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.util.xml.*;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;

import java.util.UUID;


@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = ProcessModelRef.ELEMENTLOCALNAME)
@XmlDeserializer(ProcessModelRef.Factory.class)
public class ProcessModelRef<T extends ProcessNode<T, M>, M extends ProcessModel<T, M>> implements IProcessModelRef<T, M>, XmlSerializable, SimpleXmlDeserializable {

  public static class Factory<T extends ProcessNode<T, M>, M extends ProcessModel<T, M>> implements XmlDeserializerFactory<ProcessModelRef<T,M>> {

    @Override
    public ProcessModelRef<T, M> deserialize(final XmlReader in) throws XmlException {
      return ProcessModelRef.deserialize(in);
    }
  }

  public static final String ELEMENTLOCALNAME = "processModel";
  public static final QName ELEMENTNAME = new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);

  private String mName;

  private long mHandle;

  @Nullable private UUID mUuid;

  public ProcessModelRef() {}

  public ProcessModelRef(final String name, final long handle, final UUID uuid) {
    mName = name;
    mHandle = handle;
    mUuid = uuid;
  }

  public ProcessModelRef(@NotNull final IProcessModelRef<?, M> source) {
    mHandle = source.getHandle();
    mName = source.getName();
    mUuid = source.getUuid();
  }

  @NotNull
  public static ProcessModelRef get(final IProcessModelRef<?, ?> src) {
    if (src instanceof ProcessModelRef) { return (ProcessModelRef) src; }
    return new ProcessModelRef(src);
  }

  public static <T extends ProcessNode<T, M>, M extends ProcessModel<T, M>> ProcessModelRef<T,M> deserialize(final XmlReader in) throws XmlException {
    return XmlUtil.deserializeHelper(new ProcessModelRef<T, M>(), in);
  }

  @Override
  public boolean deserializeChild(final XmlReader in) throws XmlException {
    return false;
  }

  @Override
  public boolean deserializeChildText(final CharSequence elementText) {
    return false;
  }

  @Override
  public boolean deserializeAttribute(final CharSequence attributeNamespace, final CharSequence attributeLocalName, final CharSequence attributeValue) {
    switch (attributeLocalName.toString()) {
      case "name": mName = attributeValue.toString(); return true;
      case "handle": mHandle = Long.parseLong(attributeValue.toString()); return true;
      case "uuid": mUuid = UUID.fromString(attributeValue.toString()); return true;
    }
    return false;
  }

  @Override
  public void onBeforeDeserializeChildren(final XmlReader in) throws XmlException {
    // ignore
  }

  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  @Override
  public void serialize(final XmlWriter out) throws XmlException {
    XmlUtil.writeStartElement(out, getElementName());
    XmlUtil.writeAttribute(out, "name", mName);
    XmlUtil.writeAttribute(out, "handle", mHandle);
    XmlUtil.writeAttribute(out, "uuid", mUuid==null ? null : mUuid.toString());
    XmlUtil.writeEndElement(out, getElementName());
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

  @Override
  public boolean equals(final Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }

    ProcessModelRef<?, ?> that = (ProcessModelRef<?, ?>) o;

    if (mHandle != that.mHandle) { return false; }
    if (mName != null ? !mName.equals(that.mName) : that.mName != null) { return false; }
    return !(mUuid != null ? !mUuid.equals(that.mUuid) : that.mUuid != null);

  }

  @Override
  public int hashCode() {
    int result = mName != null ? mName.hashCode() : 0;
    result = 31 * result + (int) (mHandle ^ (mHandle >>> 32));
    result = 31 * result + (mUuid != null ? mUuid.hashCode() : 0);
    return result;
  }
}
