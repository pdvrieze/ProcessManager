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
import nl.adaptivity.util.xml.SingletonNodeList;
import nl.adaptivity.util.xml.XmlSerializable;
import nl.adaptivity.util.xml.XmlUtil;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Class to represent data attached to process instances. */
public class ProcessData implements Named, XmlSerializable {



  private static class MyNodeList implements NodeList{
    private final Node[] nodes;

    public MyNodeList(List<Node> pList) {
      nodes = new Node[pList.size()];
      for(int i=nodes.length-1; i>=0; --i) {
        nodes[i]=pList.get(i);
      }
    }

    @Override
    public Node item(int pIndex) {
      try {
        return nodes[pIndex];
      } catch (ArrayIndexOutOfBoundsException e) {
        return null;
      }
    }

    @Override
    public int getLength() {
      return nodes.length;
    }

  }

  private final String mName;
  private final Object mValue;
  private final boolean mIsNodeList;

  private ProcessData(String pName, Object pValue, boolean pIsNodeList) {
    mName = pName;
    mValue = pValue;
    mIsNodeList = pIsNodeList;
  }

  public ProcessData(String pName, Node pValue) {
    this(pName, pValue, false);
  }

  public ProcessData(String pName, NodeList pValue) {
    this(pName, pValue, true);
  }

  public ProcessData(String pName, List<Node> pValue) {
    this(pName, new MyNodeList(pValue));
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
    if (mValue instanceof Node) {
      return (Node) mValue;
    }
    if (mValue instanceof NodeList) {
      NodeList nl = (NodeList) mValue;
      if (nl.getLength()==1) {
        return nl.item(0);
      }
    }
    return null;
  }

  public NodeList getNodeListValue() {
    // First check for node as for some reason the implementation in java
    // also implements NodeList (but that would be the list of children)
    if (mValue instanceof Node) {
      return new SingletonNodeList((Node) mValue);
    } else if (mValue instanceof NodeList) {
      return (NodeList) mValue;
    }
    return null;
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
        XmlUtil.serialize(pOut, new DOMSource((Node) mValue));
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
