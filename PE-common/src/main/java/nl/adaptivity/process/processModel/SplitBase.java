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

import net.devrieze.util.StringUtil;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl;
import nl.adaptivity.process.processModel.engine.SplitImpl;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identifier;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.AbstractXmlWriter;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlWriter;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

import java.util.Collection;
import java.util.Collections;


/**
 * Created by pdvrieze on 26/11/15.
 */
public class SplitBase<T extends ProcessNode<T, M>, M extends ProcessModelBase<T,M>> extends JoinSplitBase<T, M> implements Split<T, M> {

  public SplitBase(final M ownerModel, final Collection<? extends Identifiable> predecessors, final int max, final int min) {super(ownerModel, predecessors, max, min);}

  public SplitBase(final M ownerModel) {super(ownerModel);}

  public SplitBase(final Split<?, ?> orig) {
    super(orig);
  }

  @Override
  public void serialize(@NotNull final XmlWriter out) throws XmlException {
    XmlUtil.writeStartElement(out, ELEMENTNAME);
    serializeAttributes(out);
    serializeChildren(out);
    AbstractXmlWriter.endTag(out, ELEMENTNAME);
  }

  @Override
  protected void serializeAttributes(@NotNull final XmlWriter out) throws XmlException {
    super.serializeAttributes(out);
    if (getPredecessors()!=null && getPredecessors().size()>0) {
      XmlUtil.writeAttribute(out, ATTR_PREDECESSOR, getPredecessors().get(0).getId());
    }
  }

  @Override
  public boolean deserializeAttribute(final CharSequence attributeNamespace, final CharSequence attributeLocalName, final CharSequence attributeValue) {
    if (ATTR_PREDECESSOR.equals(attributeLocalName)) {
      setPredecessors(Collections.singleton(new Identifier(StringUtil.toString(attributeValue))));
      return true;
    }
    return super.deserializeAttribute(attributeNamespace, attributeLocalName, attributeValue);
  }

  @Override
  public final <R> R visit(@NotNull final Visitor<R> visitor) {
    return visitor.visitSplit(this);
  }

  @NotNull
  @Override
  public final QName getElementName() {
    return ELEMENTNAME;
  }

  @Override
  public int getMaxSuccessorCount() {
    return Integer.MAX_VALUE;
  }

  @NotNull
  public static SplitImpl deserialize(final ProcessModelImpl ownerModel, final XmlReader in) throws XmlException {
    return XmlUtil.deserializeHelper(new SplitImpl(ownerModel), in);
  }
}
