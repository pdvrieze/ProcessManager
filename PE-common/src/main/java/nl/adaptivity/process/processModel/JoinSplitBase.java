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
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.util.xml.SimpleXmlDeserializable;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlWriter;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlAttribute;

import java.util.Collection;


/**
 * Created by pdvrieze on 25/11/15.
 */
public abstract class JoinSplitBase<T extends ProcessNode<T, M>, M extends ProcessModelBase<T, M>> extends ProcessNodeBase<T,M> implements JoinSplit<T, M>, SimpleXmlDeserializable {

  protected int mMin = -1;
  protected int mMax = -1;

  public JoinSplitBase(final M ownerModel, final Collection<? extends Identifiable> predecessors, final int max, final int min) {
    super(ownerModel);
    setPredecessors(predecessors);
    mMax = max;
    mMin = min;
  }

  public JoinSplitBase(final M ownerModel) {super(ownerModel);}

  public JoinSplitBase(final JoinSplit<?, ?> orig) {
    super(orig);
    mMin = orig.getMin();
    mMax = orig.getMax();
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
  protected void serializeAttributes(@NotNull final XmlWriter out) throws XmlException {
    super.serializeAttributes(out);
    if (mMin>=0) { XmlUtil.writeAttribute(out, "min", mMin); }
    if (mMax>=0) { XmlUtil.writeAttribute(out, "max", mMax); }
  }

  @Override
  public boolean deserializeAttribute(final CharSequence attributeNamespace, @NotNull final CharSequence attributeLocalName, final CharSequence attributeValue) {
    if (StringUtil.isEqual("min", attributeLocalName)) {
      mMin = Integer.parseInt(attributeValue.toString());
    } else if (StringUtil.isEqual("max", attributeLocalName)) {
      mMax = Integer.parseInt(attributeValue.toString());
    } else {
      return super.deserializeAttribute(attributeNamespace, attributeLocalName, attributeValue);
    }
    return true;
  }

  @Override
  public int getMax() {
    return mMax;
  }

  @Override
  public int getMin() {
    return mMin;
  }

  @Override
  public void setMax(final int max) {
    mMax = max;
  }

  @Override
  public void setMin(final int min) {
    mMin = min;
  }
}
