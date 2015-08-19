package nl.adaptivity.process.engine;

import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXResult;

import net.devrieze.util.Named;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.xml.XmlSerializable;
import nl.adaptivity.util.xml.XmlUtil;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Class to represent data attached to process instances. */
public class ProcessData implements Named, XmlSerializable {

  private final String mName;
  private final Node mValue;
  private final boolean mIsNodeList = false;

  private ProcessData(String pName, Node pValue, boolean pIsNodeList) {
    assert pIsNodeList==false;
    mName = pName;
    mValue = pValue;
  }

  public ProcessData(String pName, Node pValue) {
    this(pName, pValue, false);
  }

  public ProcessData(String pName, NodeList pValue) {
    this(pName, (pValue==null || pValue.getLength()<=1)? toNode(pValue) : toDocFragment(pValue), false);
  }

  private static Node toNode(NodeList pValue) {
    if (pValue==null|| pValue.getLength()==0) { return null; }
    assert pValue.getLength()==1;
    return pValue.item(0);
  }

  private static DocumentFragment toDocFragment(NodeList pValue) {
    if (pValue==null || pValue.getLength()==0) { return null; }
    Document document = pValue.item(0).getOwnerDocument();
    DocumentFragment fragment = document.createDocumentFragment();
    for(int i=0; i<pValue.getLength(); ++i) {
      final Node n = pValue.item(i);
      if (n.getOwnerDocument()!=document) {
        fragment.appendChild(document.adoptNode(n.cloneNode(true)));
      } else {
        fragment.appendChild(n.cloneNode(true));
      }
    }
    return fragment;
  }

  public ProcessData(String pName, List<Node> pValue) {
    this(pName, toDocFragment(pValue), false);
  }


  private static DocumentFragment toDocFragment(List<Node> pValue) {
    if (pValue==null || pValue.size()==0) { return null; }
    final Document document = pValue.get(0).getOwnerDocument();
    DocumentFragment fragment = document.createDocumentFragment();
    for(Node n: pValue) {
      if (n.getOwnerDocument()!=document) {
        fragment.appendChild(document.adoptNode(n.cloneNode(true)));
      } else {
        fragment.appendChild(n.cloneNode(true));
      }
    }
    return fragment;
  }

  @Override
  public Named newWithName(String pName) {
    return new ProcessData(pName, mValue, mIsNodeList);
  }


  @Override
  public String getName() {
    return mName;
  }

  public Node getNodeValue() {
    if (mValue instanceof DocumentFragment && mValue.getFirstChild()!=null && mValue.getFirstChild().getNextSibling()==null) {
      return mValue.getFirstChild();
    }
    return mValue;
  }

  public DocumentFragment getDocumentFragment() {
    if (mValue instanceof DocumentFragment) {
      return (DocumentFragment) mValue;
    } else if (mValue==null) {
      return null;
    }
    DocumentFragment fragment;
    if (mValue instanceof Document) {
      fragment = ((Document) mValue).createDocumentFragment();
      fragment.appendChild(((Document) mValue).getDocumentElement().cloneNode(true));
    } else {
      fragment = mValue.getOwnerDocument().createDocumentFragment();
      fragment.appendChild(mValue.cloneNode(true));
    }
    return fragment;
  }

  public Object getGenericValue() {
    return mValue;
  }

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


  @Override
  public void serialize(XMLStreamWriter pOut) throws XMLStreamException {
    pOut.writeStartElement(Constants.PROCESS_ENGINE_NS, "value");
    try {
      pOut.writeAttribute("name", mName);
      if (! mIsNodeList) {
        XmlUtil.serialize(pOut, new DOMSource(mValue));
      } else if (mValue instanceof NodeList) {
        serializeNodeList(pOut);
      }
    } finally {
      pOut.writeEndElement();
    }
  }

  protected void serializeNodeList(XMLStreamWriter pOut) throws XMLStreamException {
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

}
