package nl.adaptivity.process.engine;

import net.devrieze.util.Named;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.xml.CompactFragment;
import nl.adaptivity.util.xml.XMLFragmentStreamReader;
import nl.adaptivity.util.xml.XmlSerializable;
import nl.adaptivity.util.xml.XmlUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.*;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.stream.*;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


@XmlJavaTypeAdapter(XmlSerializable.JAXBAdapter.class)
/** Class to represent data attached to process instances. */
public class ProcessData implements Named/*, XmlSerializable*/ {

  private final String mName;
  private final CompactFragment mValue;
  private final boolean mIsNodeList = false;

// Object Initialization
  @Deprecated
  public ProcessData(final String name, final Node value) {
    this(name, toCompactFragment(value));
  }
// Object Initialization end

  public ProcessData(final String name, final CompactFragment value) {
    mName = name;
    mValue = value;
  }

  @Deprecated
  public ProcessData(final String name, @Nullable final NodeList value) {
    this(name, (value==null || value.getLength()<=1)? toNode(value) : XmlUtil.toDocFragment(value));
  }

  public DocumentFragment getContentFragment() throws XMLStreamException {
    return XmlUtil.childrenToDocumentFragment(getContentStream());
  }

  @NotNull
  private static CompactFragment toCompactFragment(final Node value) {
    try {
      return XmlUtil.nodeToFragment(value);
    } catch (@NotNull final XMLStreamException e) {
      throw new RuntimeException(e);
    }
  }

  @Nullable
  private static Node toNode(@Nullable final NodeList value) {
    if (value==null|| value.getLength()==0) { return null; }
    assert value.getLength()==1;
    return value.item(0);
  }

  @Deprecated
  public ProcessData(final String name, final List<Node> value) {
    this(name, XmlUtil.toDocFragment(value));
  }


  @NotNull
  @Override
  public Named newWithName(final String name) {
    return new ProcessData(name, mValue);
  }


  @Override
  public String getName() {
    return mName;
  }

  public CompactFragment getContent() {
    return mValue;
  }

  @NotNull
  public XMLStreamReader getContentStream() throws XMLStreamException {
    return XMLFragmentStreamReader.from(getContent());
  }

  @SuppressWarnings("Duplicates")
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((mName == null) ? 0 : mName.hashCode());
    result = prime * result + ((mValue == null) ? 0 : mValue.hashCode());
    return result;
  }

  @Override
  public boolean equals(@Nullable final Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final ProcessData other = (ProcessData) obj;
    if (mName == null) {
      if (other.mName != null)
        return false;
    } else if (!mName.equals(other.mName))
      return false;
    if (mValue == null) {
      if (other.mValue != null)
        return false;
    } else if (!mValue.equals(other.mValue))
      return false;
    return true;
  }

  public void serialize(@NotNull final XMLStreamWriter out) throws XMLStreamException {
    if (out.getNamespaceContext().getPrefix(Constants.PROCESS_ENGINE_NS)==null) {
      out.writeStartElement(Constants.PROCESS_ENGINE_NS_PREFIX, "value", Constants.PROCESS_ENGINE_NS);
      out.writeNamespace(Constants.PROCESS_ENGINE_NS_PREFIX, Constants.PROCESS_ENGINE_NS);
    } else {
      out.writeStartElement(Constants.PROCESS_ENGINE_NS, "value");
    }
    final XMLStreamWriter strippedout = XmlUtil.stripMetatags(out);

    try {
      strippedout.writeAttribute("name", mName);
      XmlUtil.serialize(strippedout, new StAXSource(getContentStream()));
    } catch (@NotNull final Exception e) {
      Logger.getAnonymousLogger().log(Level.WARNING,"Error serializing children", e);
    } finally {
      out.writeEndElement();
    }
  }

  private void serializeNodeList(@NotNull final XMLStreamWriter out) throws XMLStreamException {
    final NodeList nodelist = (NodeList) mValue;
    try {
      final Transformer transformer = TransformerFactory
          .newInstance()
          .newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

      for(int i = 0; i<nodelist.getLength(); ++i) {
        transformer.transform(new DOMSource(nodelist.item(i)), new StAXResult(out));
      }
    } catch (@NotNull final TransformerException e) {
      throw new XMLStreamException(e);
    }
  }

  private void serializeDocumentFragment2(@NotNull final XMLStreamWriter out) throws XMLStreamException {
    final DocumentFragment nodelist = (DocumentFragment) mValue;
    try {
      final Transformer transformer = TransformerFactory
              .newInstance()
              .newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

      for (Node n=nodelist.getFirstChild(); n!=null; n= nodelist.getNextSibling()) {
        transformer.transform(new DOMSource(n), new StAXResult(out));
      }
    } catch (@NotNull final TransformerException e) {
      throw new XMLStreamException(e);
    }
  }

}
