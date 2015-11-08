//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2009.08.27 at 08:15:55 PM CEST
//


package nl.adaptivity.process.processModel;

import net.devrieze.util.Transaction;
import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.process.engine.PETransformer;
import nl.adaptivity.process.engine.ProcessData;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.util.xml.*;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import java.sql.SQLException;


@XmlDeserializer(XmlDefineType.Factory.class)
public class XmlDefineType extends XPathHolder implements IXmlDefineType {

  public static class Factory implements XmlDeserializerFactory<XmlDefineType> {

    @Override
    public XmlDefineType deserialize(final XMLStreamReader in) throws XMLStreamException {
      return XmlDefineType.deserialize(in);
    }
  }

  public static final String ELEMENTLOCALNAME = "define";
  public static final QName ELEMENTNAME = new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);

  private String refNode;

  private String refName;

  public XmlDefineType() {}

  public XmlDefineType(final String pName, final String pRefNode, final String pRefName, String pPath, final char[] pContent, final Iterable<Namespace> pOriginalNSContext) {
    super(pContent, pOriginalNSContext, pPath, pName);
    refNode = pRefNode;
    refName = pRefName;
  }

  public static XmlDefineType deserialize(final XMLStreamReader pIn) throws XMLStreamException {
    return deserialize(pIn, new XmlDefineType());
  }

  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  @Override
  public boolean deserializeAttribute(final String pAttributeNamespace, final String pAttributeLocalName, final String pAttributeValue) {
    switch (pAttributeLocalName) {
      case "refnode": setRefNode(pAttributeValue); return true;
      case "refname": setRefName(pAttributeValue); return true;
      default:
        return super.deserializeAttribute(pAttributeNamespace, pAttributeLocalName, pAttributeValue);
    }
  }

  @Override
  protected void serializeStartElement(final XMLStreamWriter pOut) throws XMLStreamException {
    XmlUtil.writeStartElement(pOut, new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX));
  }

  @Override
  protected void serializeAttributes(final XMLStreamWriter out) throws XMLStreamException {
    super.serializeAttributes(out);
    XmlUtil.writeAttribute(out, "refnode", getRefNode());
    XmlUtil.writeAttribute(out, "refname", getRefName());
  }

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.IXmlDefineType#getRefNode()
     */
  @Override
  public String getRefNode() {
    return refNode;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IXmlDefineType#setRefNode(String)
   */
  @Override
  public void setRefNode(final String value) {
    this.refNode = value;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.XmlImportType#getName()
   */
  @Override
  public String getRefName() {
    return refName;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.XmlImportType#setName(java.lang.String)
   */
  @Override
  public void setRefName(final String value) {
    this.refName = value;
  }

  /**
   *
   * @param pExport
   * @return
   */
  public static XmlDefineType get(IXmlDefineType pExport) {
    if (pExport instanceof XmlDefineType) { return (XmlDefineType) pExport; }
    XmlDefineType result = new XmlDefineType(pExport.getName(), pExport.getRefNode(),pExport.getRefName(), pExport.getPath(), pExport.getContent(), pExport.getOriginalNSContext());
    return result;
  }

  @Override
  public <T extends IProcessNodeInstance<T>> ProcessData apply(Transaction pTransaction, IProcessNodeInstance<T> pNode) throws SQLException {
    final ProcessData processData;
    if (refNode!=null) {
      IProcessNodeInstance<T> predecessor = pNode.getPredecessor(pTransaction, refNode);
      ProcessData origpair = predecessor.getResult(pTransaction, refName);
      if (origpair==null) {
        processData = null;
      } else {
        try {
          if (getXPath()==null) {
            processData = new ProcessData(getName(), origpair.getContent());
          } else {
            processData = new ProcessData(getName(), (NodeList) getXPath().evaluate(origpair.getContent(), XPathConstants.NODESET));
          }
        } catch (XPathExpressionException e) {
          throw new RuntimeException(e);
        }
      }
    } else {
      processData = null;
    }
    char[] content = getContent();
    if (getContent()!=null && getContent().length>0) {
      try {
        PETransformer transformer = PETransformer.create(SimpleNamespaceContext.from(getOriginalNSContext()), processData);

        CompactFragment transformed = XmlUtil.siblingsToFragment(transformer.createFilter(getBodyStreamReader()));
        return new ProcessData(getName(), transformed);

      } catch (XMLStreamException pE) {
        throw new RuntimeException(pE);
      }
    } else {
      return processData;
    }
  }

}
