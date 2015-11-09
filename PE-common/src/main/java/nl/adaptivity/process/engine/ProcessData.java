package nl.adaptivity.process.engine;

import net.devrieze.util.Named;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.xml.CompactFragment;
import nl.adaptivity.util.xml.XMLFragmentStreamReader;
import nl.adaptivity.util.xml.XmlSerializable;
import nl.adaptivity.util.xml.XmlUtil;
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
  public ProcessData(String pName, Node pValue) {
    this(pName, toCompactFragment(pValue));
  }
// Object Initialization end

  public ProcessData(String pName, CompactFragment pValue) {
    mName = pName;
    mValue = pValue;
  }

  @Deprecated
  public ProcessData(String pName, NodeList pValue) {
    this(pName, (pValue==null || pValue.getLength()<=1)? toNode(pValue) : XmlUtil.toDocFragment(pValue));
  }

  public DocumentFragment getContentFragment() throws XMLStreamException {
    return XmlUtil.childrenToDocumentFragment(getContentStream());
  }

  private static CompactFragment toCompactFragment(final Node pValue) {
    try {
      return XmlUtil.nodeToFragment(pValue);
    } catch (XMLStreamException pE) {
      throw new RuntimeException(pE);
    }
  }

  private static Node toNode(NodeList pValue) {
    if (pValue==null|| pValue.getLength()==0) { return null; }
    assert pValue.getLength()==1;
    return pValue.item(0);
  }

  @Deprecated
  public ProcessData(String pName, List<Node> pValue) {
    this(pName, XmlUtil.toDocFragment(pValue));
  }


  @Override
  public Named newWithName(String pName) {
    return new ProcessData(pName, mValue);
  }


  @Override
  public String getName() {
    return mName;
  }

  public CompactFragment getContent() {
    return mValue;
  }

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
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ProcessData other = (ProcessData) obj;
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

  public void serialize(XMLStreamWriter pOut) throws XMLStreamException {
    if (pOut.getNamespaceContext().getPrefix(Constants.PROCESS_ENGINE_NS)==null) {
      pOut.writeStartElement(Constants.PROCESS_ENGINE_NS_PREFIX, "value", Constants.PROCESS_ENGINE_NS);
      pOut.writeNamespace(Constants.PROCESS_ENGINE_NS_PREFIX, Constants.PROCESS_ENGINE_NS);
    } else {
      pOut.writeStartElement(Constants.PROCESS_ENGINE_NS, "value");
    }
    XMLStreamWriter out = XmlUtil.stripMetatags(pOut);

    try {
      out.writeAttribute("name", mName);
      XmlUtil.serialize(out, new StAXSource(getContentStream()));
    } catch (Exception e) {
      Logger.getAnonymousLogger().log(Level.WARNING,"Error serializing children", e);
    } finally {
      out.writeEndElement();
    }
  }

  private void serializeNodeList(XMLStreamWriter pOut) throws XMLStreamException {
    NodeList nodelist = (NodeList) mValue;
    try {
      final Transformer transformer = TransformerFactory
          .newInstance()
          .newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

      for(int i = 0; i<nodelist.getLength(); ++i) {
        transformer.transform(new DOMSource(nodelist.item(i)), new StAXResult(pOut));
      }
    } catch (TransformerException e) {
      throw new XMLStreamException(e);
    }
  }

  private void serializeDocumentFragment2(XMLStreamWriter pOut) throws XMLStreamException {
    DocumentFragment nodelist = (DocumentFragment) mValue;
    try {
      final Transformer transformer = TransformerFactory
              .newInstance()
              .newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

      for (Node n=nodelist.getFirstChild(); n!=null; n= nodelist.getNextSibling()) {
        transformer.transform(new DOMSource(n), new StAXResult(pOut));
      }
    } catch (TransformerException e) {
      throw new XMLStreamException(e);
    }
  }

}
