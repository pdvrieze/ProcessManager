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

package nl.adaptivity.process.engine;

import net.devrieze.util.Handle;
import nl.adaptivity.util.xml.SimpleXmlDeserializable;
import nl.adaptivity.xml.*;

import javax.xml.bind.annotation.XmlValue;


/**
 * Created by pdvrieze on 10/12/15.
 */
public abstract class XmlHandle<T> implements Handle<T>, XmlSerializable, SimpleXmlDeserializable {

  @XmlValue
  private long mHandle;

  public XmlHandle(long handle) {
    mHandle = handle;
  }

  public XmlHandle(Handle<? extends T> handle) {
    mHandle = handle.getHandleValue();
  }

  @Override
  public boolean deserializeChild(final XmlReader in) throws XmlException {
    return false;
  }

  @Override
  public boolean deserializeChildText(final CharSequence elementText) {
    mHandle = Long.parseLong(elementText.toString());
    return true;
  }

  @Override
  public boolean deserializeAttribute(final CharSequence attributeNamespace, final CharSequence attributeLocalName, final CharSequence attributeValue) {
    return false;
  }

  @Override
  public void onBeforeDeserializeChildren(final XmlReader in) throws XmlException {
    // ignore
  }

  @Override
  public void serialize(final XmlWriter out) throws XmlException {
    XmlWriterUtil.writeSimpleElement(out, getElementName(), Long.toString(mHandle));
  }

  @Override
  public final long getHandleValue() {
    return mHandle;
  }

  public final void setHandle(final long handle) {
    mHandle = handle;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('{');
    sb.append(mHandle);
    sb.append('}');
    return sb.toString();
  }
}
