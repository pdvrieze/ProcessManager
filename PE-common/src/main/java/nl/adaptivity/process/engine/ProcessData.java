package nl.adaptivity.process.engine;

import net.devrieze.util.Named;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.xml.CompactFragment;
import nl.adaptivity.util.xml.XMLFragmentStreamReader;
import nl.adaptivity.util.xml.XmlSerializable;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXResult;

import java.util.List;


@XmlJavaTypeAdapter(XmlSerializable.JAXBAdapter.class)
/** Class to represent data attached to process instances. */
public class ProcessData implements Named/*, XmlSerializable*/ {

  private final String mName;
  private final CompactFragment mValue;
  private final boolean mIsNodeList = false;

// Object Initialization

  /**
   * @deprecated Initialise with compact fragment instead.
   * @see #ProcessData(String, CompactFragment)
   */
  @Deprecated
  public ProcessData(final String name, final Node value) throws XmlException {
    this(name, toCompactFragment(value));
  }
// Object Initialization end

  public ProcessData(final String name, final CompactFragment value) {
    mName = name;
    mValue = value;
  }

  @SuppressWarnings("deprecation")
  @Deprecated
  public ProcessData(final String name, @Nullable final NodeList value) throws XmlException {
    this(name, (value==null || value.getLength()<=1)? toNode(value) : XmlUtil.toDocFragment(value));
  }

  public DocumentFragment getContentFragment() throws XmlException {
    return XmlUtil.childrenToDocumentFragment(getContentStream());
  }

  @NotNull
  private static CompactFragment toCompactFragment(final Node value) throws XmlException {
    return XmlUtil.nodeToFragment(value);
  }

  @Nullable
  private static Node toNode(@Nullable final NodeList value) {
    if (value==null|| value.getLength()==0) { return null; }
    assert value.getLength()==1;
    return value.item(0);
  }

  @SuppressWarnings("deprecation")
  @Deprecated
  public ProcessData(final String name, final List<Node> value) throws XmlException {
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
  public XmlReader getContentStream() throws XmlException {
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

  @SuppressWarnings("Duplicates")
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

  public void serialize(@NotNull final XmlWriter out) throws XmlException {
    if (out.getNamespaceContext().getPrefix(Constants.PROCESS_ENGINE_NS)==null) {
      out.startTag(null, "value", Constants.PROCESS_ENGINE_NS_PREFIX);
      out.namespaceAttr(Constants.PROCESS_ENGINE_NS_PREFIX, Constants.PROCESS_ENGINE_NS);
    } else {
      out.startTag(Constants.PROCESS_ENGINE_NS, null, "value");
    }
    final XmlWriter strippedout = XmlUtil.stripMetatags(out);

    try {
      strippedout.attribute(null, "name", null, mName);
      XmlUtil.serialize(getContentStream(), strippedout);
    } finally {
      out.endTag(null, null, null);
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
