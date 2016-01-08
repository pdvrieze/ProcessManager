package nl.adaptivity.process.processModel;

import nl.adaptivity.process.ProcessConsts;
import nl.adaptivity.util.xml.SimpleXmlDeserializable;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;


/**
 * Created by pdvrieze on 26/11/15.
 */
public abstract class StartNodeBase<T extends ProcessNode<T, M>, M extends ProcessModelBase<T,M>> extends ProcessNodeBase<T,M> implements StartNode<T,M>, SimpleXmlDeserializable {

  public StartNodeBase(@Nullable final M ownerModel) {
    super(ownerModel);
  }

  public StartNodeBase(final ProcessNode<?, ?> orig) {
    super(orig);
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
    XmlUtil.writeStartElement(out, ELEMENTNAME);
    serializeAttributes(out);
    serializeChildren(out);
    XmlUtil.writeEndElement(out, ELEMENTNAME);
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
