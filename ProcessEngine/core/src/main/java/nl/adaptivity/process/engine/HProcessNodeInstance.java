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

import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;
import nl.adaptivity.util.xml.XmlDeserializer;
import nl.adaptivity.util.xml.XmlDeserializerFactory;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;

import javax.xml.namespace.QName;

@XmlDeserializer(HProcessNodeInstance.Factory.class)
public final class HProcessNodeInstance extends XmlHandle<ProcessNodeInstance> {

  public static class Factory implements XmlDeserializerFactory<HProcessNodeInstance> {

    @Override
    public HProcessNodeInstance deserialize(final XmlReader in) throws XmlException {
      return HProcessNodeInstance.deserialize(in);
    }
  }

  public static final java.lang.String ELEMENTLOCALNAME = "nodeInstanceHandle";
  public static final QName ELEMENTNAME = new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);

  public HProcessNodeInstance() {
    super(-1);
  }

  public HProcessNodeInstance(final long handle) {
    super(handle);
  }

  private static HProcessNodeInstance deserialize(final XmlReader in) throws XmlException {
    return XmlUtil.deserializeHelper(new HProcessNodeInstance(), in);
  }

  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  @Override
  public boolean equals(final Object obj) {
    return (obj == this) || ((obj instanceof HProcessNodeInstance) && (getHandle() == ((HProcessNodeInstance) obj).getHandle()));
  }

  @Override
  public int hashCode() {
    return (int) getHandle();
  }

}
