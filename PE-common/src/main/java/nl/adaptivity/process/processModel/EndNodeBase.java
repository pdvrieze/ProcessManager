package nl.adaptivity.process.processModel;

import nl.adaptivity.process.ProcessConsts;
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

import java.util.Arrays;
import java.util.Collection;


/**
 * Created by pdvrieze on 24/11/15.
 */
public abstract class EndNodeBase<T extends ProcessNode<T>> extends ProcessNodeBase<T> implements EndNode<T>, SimpleXmlDeserializable {

  public EndNodeBase(@Nullable final ProcessModelBase<T> ownerModel) {
    super(ownerModel);
  }

  @Override
  public boolean deserializeChild(@NotNull final XmlReader in) throws XmlException {
    if (ProcessConsts.Engine.NAMESPACE.equals(in.getNamespaceUri())) {
      switch (in.getLocalName().toString()) {
        case "export":
        case XmlDefineType.ELEMENTLOCALNAME:
          getDefines().add(XmlDefineType.deserialize(in)); return true;
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
    XmlUtil.writeStartElement(out, EndNode.ELEMENTNAME);
    serializeAttributes(out);
    serializeChildren(out);
    XmlUtil.writeEndElement(out, EndNode.ELEMENTNAME);
  }

  @Override
  protected void serializeAttributes(@NotNull final XmlWriter out) throws XmlException {
    super.serializeAttributes(out);
    if (getPredecessor()!=null) {
      XmlUtil.writeAttribute(out, ATTR_PREDECESSOR, getPredecessor().getId());
    }
  }

  @Override
  public <R> R visit(@NotNull final Visitor<R> visitor) {
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
  public final ProcessNodeSet<? extends Identifiable> getSuccessors() {
    return ProcessNodeSet.empty();
  }

  @Override
  public final void setDefines(@Nullable final Collection<? extends IXmlDefineType> exports) {
    super.setDefines(exports);
  }

  public final void setPredecessor(final Identifiable predecessor) {
    setPredecessors(Arrays.asList(predecessor));
  }
}
