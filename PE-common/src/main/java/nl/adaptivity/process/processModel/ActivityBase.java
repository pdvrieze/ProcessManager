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
import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identifier;
import nl.adaptivity.util.xml.SimpleXmlDeserializable;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

import java.util.Collection;
import java.util.Collections;


/**
 * Base class for activity implementations
 * Created by pdvrieze on 23/11/15.
 */
public abstract class ActivityBase<T extends ProcessNode<T, M>, M extends ProcessModelBase<T, M>> extends ProcessNodeBase<T, M> implements Activity<T, M>, SimpleXmlDeserializable {

  @Nullable private XmlMessage mMessage;

  @Nullable private String mName;

  public ActivityBase(final Activity<?, ?> orig) {
    super(orig);
    setMessage(orig.getMessage());
    setName(orig.getName());
  }

  // Object Initialization
  public ActivityBase(@Nullable final M ownerModel) {
    super(ownerModel);
  }
// Object Initialization end

  @Override
  public final <R> R visit(@NotNull final Visitor<R> visitor) {
    return visitor.visitActivity(this);
  }

  @Override
  public boolean deserializeChild(@NotNull final XmlReader in) throws XmlException {
    if (Engine.NAMESPACE.equals(in.getNamespaceUri())) {
      switch (in.getLocalName().toString()) {
        case XmlDefineType.ELEMENTLOCALNAME:
          getDefines().add(XmlDefineType.deserialize(in));
          return true;
        case XmlResultType.ELEMENTLOCALNAME:
          getResults().add(XmlResultType.deserialize(in));
          return true;
        case Condition.ELEMENTLOCALNAME:
          deserializeCondition(in);
          return true;
        case XmlMessage.ELEMENTLOCALNAME:
          setMessage(XmlMessage.deserialize(in));
          return true;
      }
    }
    return false;
  }

  protected abstract void deserializeCondition(final XmlReader in) throws XmlException;

  @Override
  public boolean deserializeChildText(final CharSequence elementText) {
    return false;
  }

  @Override
  public final void serialize(@NotNull final XmlWriter out) throws XmlException {
    XmlUtil.writeStartElement(out, ELEMENTNAME);
    serializeAttributes(out);
    serializeChildren(out);
    XmlUtil.writeEndElement(out, ELEMENTNAME);
  }

  @Override
  protected final void serializeAttributes(@NotNull final XmlWriter out) throws XmlException {
    super.serializeAttributes(out);
    Identifiable predecessor = getPredecessor();
    if (predecessor!=null) {
      XmlUtil.writeAttribute(out, ATTR_PREDECESSOR, predecessor.getId());
    }
    XmlUtil.writeAttribute(out, "name", getName());
  }

  protected final void serializeChildren(final XmlWriter out) throws XmlException {
    super.serializeChildren(out);
    serializeCondition(out);

    {
      final XmlMessage m = getMessage();
      if (m != null) { m.serialize(out); }
    }
  }

  protected abstract void serializeCondition(final XmlWriter out) throws XmlException;

  @Override
  public final boolean deserializeAttribute(final CharSequence attributeNamespace, @NotNull final CharSequence attributeLocalName, final CharSequence attributeValue) {
    switch (attributeLocalName.toString()) {
      case ATTR_PREDECESSOR:
        setPredecessor(new Identifier(attributeValue.toString()));
        return true;
      case "name":
        setName(StringUtil.toString(attributeValue));
        return true;
    }
    return super.deserializeAttribute(attributeNamespace, attributeLocalName, attributeValue);
  }

  @Override
  public final void setDefines(@Nullable final Collection<? extends IXmlDefineType> exports) {
    super.setDefines(exports);
  }

  @Override
  public final void setResults(@Nullable final Collection<? extends IXmlResultType> imports) { super.setResults(imports); }

  /* (non-Javadoc)
       * @see nl.adaptivity.process.processModel.IActivity#getName()
       */
  @Override
  public final String getName() {
    return mName;
  }

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.IActivity#setName(java.lang.String)
     */
  @Override
  public final void setName(final String name) {
    mName = name;
  }

  /* (non-Javadoc)
       * @see nl.adaptivity.process.processModel.IActivity#getPredecessor()
       */
  @Nullable
  @Override
  public final Identifiable getPredecessor() {
    final Collection<? extends Identifiable> ps = getPredecessors();
    if ((ps == null) || (ps.size() != 1)) {
      return null;
    }
    return ps.iterator().next();
  }

  @Override
  public final void setPredecessor(final Identifiable predecessor) {
    setPredecessors(Collections.singleton(predecessor));
  }

  @Override
  public final XmlMessage getMessage() {
    return mMessage;
  }

  @Override
  public final void setMessage(final IXmlMessage message) {
    setMessage(XmlMessage.get(message));
  }

  public final void setMessage(final XmlMessage message) {
    mMessage = XmlMessage.get(message);
  }

  @NotNull
  @Override
  public final QName getElementName() {
    return ELEMENTNAME;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }
    if (!super.equals(o)) { return false; }

    ActivityBase<?, ?> that = (ActivityBase<?, ?>) o;

    if (mMessage != null ? !mMessage.equals(that.mMessage) : that.mMessage != null) { return false; }
    final String condition = getCondition();
    if (condition != null ? ! condition.equals(that.getCondition()) : that.getCondition() != null) { return false; }
    return mName != null ? mName.equals(that.mName) : that.mName == null;

  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (mMessage != null ? mMessage.hashCode() : 0);
    final String condition = getCondition();
    result = 31 * result + (condition != null ? condition.hashCode() : 0);
    result = 31 * result + (mName != null ? mName.hashCode() : 0);
    return result;
  }

  // Property acccessors end
}
