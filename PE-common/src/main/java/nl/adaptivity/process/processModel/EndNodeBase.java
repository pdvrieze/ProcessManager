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
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identifier;
import nl.adaptivity.process.util.IdentifyableSet;
import nl.adaptivity.util.xml.SimpleXmlDeserializable;
import nl.adaptivity.xml.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;


/**
 * Created by pdvrieze on 24/11/15.
 */
public abstract class EndNodeBase<T extends ProcessNode<T, M>, M extends ProcessModelBase<T, M>> extends ProcessNodeBase<T, M> implements EndNode<T, M>, SimpleXmlDeserializable {

  public EndNodeBase(@Nullable final M ownerModel) {
    super(ownerModel);
  }

  public EndNodeBase(final EndNode<?, ?> orig) {
    super(orig, null);
  }

  @Override
  public boolean deserializeChild(@NotNull final XmlReader in) throws XmlException {
    if (ProcessConsts.Engine.NAMESPACE.equals(in.getNamespaceUri())) {
      switch (in.getLocalName().toString()) {
        case "export":
        case XmlDefineType.ELEMENTLOCALNAME:
          ((List) getDefines()).add(XmlDefineType.deserialize(in)); return true;
      }
    }
    return false;
  }

  @Override
  public boolean deserializeAttribute(final CharSequence attributeNamespace, final CharSequence attributeLocalName, final CharSequence attributeValue) {
    if (ATTR_PREDECESSOR.equals(attributeLocalName)) {
      setPredecessor(new Identifier(attributeValue.toString()));
      return true;
    }
    return super.deserializeAttribute(attributeNamespace, attributeLocalName, attributeValue);
  }

  @Override
  public boolean deserializeChildText(final CharSequence elementText) {
    return false;
  }

  @Override
  public void serialize(@NotNull final XmlWriter out) throws XmlException {
    XmlWriterUtil.smartStartTag(out, EndNode.ELEMENTNAME);
    serializeAttributes(out);
    serializeChildren(out);
    XmlWriterUtil.endTag(out, EndNode.ELEMENTNAME);
  }

  @Override
  protected void serializeAttributes(@NotNull final XmlWriter out) throws XmlException {
    super.serializeAttributes(out);
    if (getPredecessor()!=null) {
      XmlWriterUtil.writeAttribute(out, ATTR_PREDECESSOR, getPredecessor().getId());
    }
  }

  @Override
  public final <R> R visit(@NotNull final Visitor<R> visitor) {
    return visitor.visitEndNode(this);
  }

  @NotNull
  @Override
  public final QName getElementName() {
    return EndNode.ELEMENTNAME;
  }

  @Nullable
  public final Identifiable getPredecessor() {
    final Collection<? extends Identifiable> ps = getPredecessors();
    if ((ps == null) || (ps.size() != 1)) {
      return null;
    }
    return ps.iterator().next();
  }

  @Override
  public final int getMaxSuccessorCount() {
    return 0;
  }

  @NotNull
  @Override
  public final IdentifyableSet<Identifiable> getSuccessors() {
    return IdentifyableSet.empty();
  }

  @Override
  public final void setDefines(@Nullable final Collection<? extends IXmlDefineType> exports) {
    super.setDefines(exports);
  }

  public final void setPredecessor(final Identifiable predecessor) {
    setPredecessors(Arrays.asList(predecessor));
  }
}
