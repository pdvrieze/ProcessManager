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

import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.xml.XmlDeserializer;
import nl.adaptivity.xml.XmlDeserializerFactory;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;


@XmlDeserializer(XmlJoin.Factory.class)
public class XmlJoin extends JoinBase<XmlProcessNode,ProcessModelImpl> {

  public static class Factory implements XmlDeserializerFactory<XmlJoin> {

    @NotNull
    @Override
    public XmlJoin deserialize(@NotNull final XmlReader reader) throws XmlException {
      return XmlJoin.deserialize(null, reader);
    }
  }

  public XmlJoin(final Join<?, ?> orig) {
    super(orig);
  }

  @NotNull
  public static XmlJoin deserialize(final ProcessModelImpl ownerModel, @NotNull final XmlReader in) throws
          XmlException {
    return nl.adaptivity.xml.XmlUtil.deserializeHelper(new XmlJoin(ownerModel), in);
  }

  public XmlJoin(final ProcessModelImpl ownerModel, final Collection<? extends Identifiable> predecessors, final int min, final int max) {
    super(ownerModel, predecessors, max, min);
    if ((getMin() < 1) || (max < min)) {
      throw new IllegalProcessModelException("Join range (" + min + ", " + max + ") must be sane");
    }
  }

  public XmlJoin(final ProcessModelImpl ownerModel) {
    super(ownerModel);
  }

  @NotNull
  public static XmlJoin andJoin(final ProcessModelImpl ownerModel, final XmlProcessNode... predecessors) {
    return new XmlJoin(ownerModel, Arrays.asList(predecessors), Integer.MAX_VALUE, Integer.MAX_VALUE);
  }

  @Deprecated
  @Nullable
  Set<? extends Identifiable> getXmlPrececessors() {
    return getPredecessors();
  }

  @Deprecated
  void setXmlPrececessors(final List<? extends XmlProcessNode> pred) {
    swapPredecessors(pred);
  }

}
