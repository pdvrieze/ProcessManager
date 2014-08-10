package nl.adaptivity.process.engine;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Node;

import net.devrieze.util.Named;

import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.xml.XmlSerializable;
import nl.adaptivity.util.xml.XmlUtil;

/** Class to represent data attached to process instances. */
public class ProcessData implements Named, XmlSerializable {

  private final String mName;
  private final Node mValue;

  public ProcessData(String pName, Node pValue) {
    mName = pName;
    mValue = pValue;
  }


  @Override
  public Named newWithName(String pName) {
    return new ProcessData(pName, mValue);
  }


  @Override
  public String getName() {
    return mName;
  }


  public Node getValue() {
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
      XmlUtil.serialize(pOut, new DOMSource(mValue));
    } finally {
      pOut.writeEndElement();
    }
  }

}
