/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.Handle;
import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.process.processModel.ProcessModel;
import nl.adaptivity.process.processModel.MutableProcessNode;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.xml.*;
import nl.adaptivity.util.xml.*;
import nl.adaptivity.xml.XmlUtil;
import nl.adaptivity.xml.schema.annotations.XmlName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

import java.util.UUID;

@XmlDeserializer(ProcessModelRef.Factory.class)
public class ProcessModelRef<T extends ProcessNode<T, M>, M extends ProcessModel<T, M>> implements IProcessModelRef<T, M>, XmlSerializable, SimpleXmlDeserializable {

  public static class Factory implements XmlDeserializerFactory<ProcessModelRef<?,?>> {

    @Override
    public ProcessModelRef<?, ?> deserialize(final XmlReader reader) throws XmlException {
      return ProcessModelRef.deserialize(reader);
    }
  }

  public static final String ELEMENTLOCALNAME = "processModel";
  public static final QName ELEMENTNAME = new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);

  private String mName;

  private long mHandle;

  @Nullable private UUID mUuid;

  public ProcessModelRef() {}

  public ProcessModelRef(final String name, final Handle<? extends ProcessModel<?,?>> handle, final UUID uuid) {
    mName = name;
    mHandle = handle.getHandleValue();
    mUuid = uuid;
  }

  public ProcessModelRef(@NotNull final IProcessModelRef<?, M> source) {
    mHandle = source.getHandleValue();
    mName = source.getName();
    mUuid = source.getUuid();
  }

  @NotNull
  public static <T extends ProcessNode<T, M>, M extends ProcessModel<T, M>> ProcessModelRef<T,M> get(final IProcessModelRef<T, M> src) {
    if (src instanceof ProcessModelRef) { return (ProcessModelRef<T,M>) src; }
    return new ProcessModelRef<>(src);
  }

  public static <T extends ProcessNode<T, M>, M extends ProcessModel<T, M>> ProcessModelRef<T,M> deserialize(final XmlReader in) throws XmlException {
    return XmlUtil.<ProcessModelRef<T,M>>deserializeHelper(new ProcessModelRef<T, M>(), in);
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
  public void onBeforeDeserializeChildren(@NotNull final XmlReader reader) {
    // ignore
  }

  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  @Override
  public void serialize(final XmlWriter out) throws XmlException {
    XmlWriterUtil.smartStartTag(out, getElementName());
    XmlWriterUtil.writeAttribute(out, "name", mName);
    XmlWriterUtil.writeAttribute(out, "handle", mHandle);
    final String value = mUuid==null ? null : mUuid.toString();
    XmlWriterUtil.writeAttribute(out, "uuid", value);
    XmlWriterUtil.endTag(out, getElementName());
  }

  void setName(final String name) {
    mName = name;
  }

  @Override
  public String getName() {
    return mName;
  }

  void setHandle(final long handle) {
    mHandle = handle;
  }

  @Override
  public long getHandleValue() {
    return mHandle;
  }

  @Override
  public boolean getValid() {
    return mHandle>=0L;
  }

  @Nullable
  @Override
  public UUID getUuid() {
    return mUuid;
  }

  @Nullable
  @XmlName("uuid")
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
