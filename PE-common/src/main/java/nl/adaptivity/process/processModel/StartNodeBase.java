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

package nl.adaptivity.process.processModel;

import nl.adaptivity.process.ProcessConsts;
import nl.adaptivity.process.processModel.ProcessNodeBase.Builder;
import nl.adaptivity.util.xml.SimpleXmlDeserializable;
import nl.adaptivity.xml.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;


/**
 * Created by pdvrieze on 26/11/15.
 */
public abstract class StartNodeBase<T extends ProcessNode<T, M>, M extends ProcessModelBase<T,M>> extends ProcessNodeBase<T,M> implements StartNode<T,M>, SimpleXmlDeserializable {

  public static abstract class Builder<T extends ProcessNode<T, M>, M extends ProcessModelBase<T, M>> extends ProcessNodeBase.Builder<T,M>  implements StartNode.Builder<T,M> {

    @NotNull
    @Override
    public abstract StartNodeBase<T,M> build(@NotNull final M newOwner);
  }

  public StartNodeBase(@Nullable final M ownerModel) {
    super(ownerModel);
  }

  public StartNodeBase(@NotNull final ProcessNode<?, ?> orig, @Nullable final M newOwnerModel) {
    super(orig, newOwnerModel);
  }

  public StartNodeBase(final ProcessNode<?,?> orig) {
    this(orig, null);
  }

  public StartNodeBase(@NotNull final Builder<T,M> builder, @NotNull final M newOwnerModel) {
    super(builder, newOwnerModel);
  }

  @Override
  public boolean deserializeChild(@NotNull final XmlReader in) throws XmlException {
    if (ProcessConsts.Engine.NAMESPACE.equals(in.getNamespaceUri())) {
      switch (in.getLocalName().toString()) {
        case "import":
          getResults().add(XmlResultType.deserialize(in)); return true;
      }
    }
    return false;
  }

  @Override
  public boolean deserializeChildText(final CharSequence elementText) {
    return false;
  }

  @Override
  public void serialize(@NotNull final XmlWriter out) throws XmlException {
    XmlWriterUtil.smartStartTag(out, ELEMENTNAME);
    serializeAttributes(out);
    serializeChildren(out);
    XmlWriterUtil.endTag(out, ELEMENTNAME);
  }

  @Override
  public final <R> R visit(@NotNull final Visitor<R> visitor) {
    return visitor.visitStartNode(this);
  }

  @NotNull
  @Override
  public final QName getElementName() {
    return ELEMENTNAME;
  }

  @Override
  public final int getMaxPredecessorCount() {
    return 0;
  }
}
