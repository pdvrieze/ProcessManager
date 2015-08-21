//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2009.08.27 at 08:15:55 PM CEST
//


package nl.adaptivity.process.processModel;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import net.devrieze.util.Transaction;
import net.devrieze.util.db.DBTransaction;
import nl.adaptivity.process.engine.PETransformer;
import nl.adaptivity.process.engine.ProcessData;
import nl.adaptivity.process.exec.IProcessNodeInstance;

import nl.adaptivity.process.processModel.XmlDefineType.Adapter;
import nl.adaptivity.util.xml.XmlUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * May contain literal elements as content. In that case only the paramName
 * attribute is used.
 * <p>
 * Java class for ExportType complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 *
 * <pre>
 * &lt;complexType name="ExportType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;any maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="node" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="paramName" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="path" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlJavaTypeAdapter(Adapter.class)
public class XmlDefineType extends XPathHolder implements IXmlDefineType {

  @XmlRootElement(name=XmlDefineType.ELEMENTNAME)
  @XmlAccessorType(XmlAccessType.NONE)
  @XmlType(name = "DefineType", propOrder = { "content" })
  static class AdaptedDefine {
    @XmlMixed
    @XmlAnyElement(lax = true)
    protected List<Object> content = new ArrayList<>();

    @XmlAttribute(name="refnode")
    protected String refNode;

    @XmlAttribute(name="refname")
    protected String refName;

    @XmlAttribute(name="name", required = true)
    protected String name;

    @XmlAttribute(name="xpath")
    protected String mPath;

    // Compatibility attribute for reading old models
    public String getPath() { return null; }

    @XmlAttribute(name="path")
    public void setPath(String path) {
      this.mPath=path;
    }

  }

  static class Adapter extends XmlAdapter<AdaptedDefine, XmlDefineType> {

    @Override
    public XmlDefineType unmarshal(final AdaptedDefine v) throws Exception {
      ArrayList<Object> newContent = new ArrayList<>(v.content.size());
      for(Object o: v.content) {
        if (o instanceof Node) {
          try {
            newContent.add(XmlUtil.cannonicallize((Node) o));
          } catch (Exception e) {
            Logger.getAnonymousLogger().log(Level.WARNING, "Failure to cannonicalize node", e);
            newContent.add(o);
          }
        } else {
          newContent.add(o);
        }
      }
      return new XmlDefineType(v.mPath, newContent, v.refNode, v.refName, v.name);
    }

    @Override
    public AdaptedDefine marshal(final XmlDefineType v) throws Exception {
      AdaptedDefine result = new AdaptedDefine();
      result.content = v.content;
      result.name = v.name;
      result.refName = v.refName;
      result.refNode = v.getRefNode();
      result.mPath = v.getPath();
      return result;
    }
  }

  public static final String ELEMENTNAME = "define";

  private List<Object> content;

  private String refNode;

  private String refName;

  private String name;

  public XmlDefineType() {}

  public XmlDefineType(final String path, final List<Object> pContent, final String pRefNode, final String pRefName, final String pName) {
    setPath(path);
    content = pContent;
    refNode = pRefNode;
    refName = pRefName;
    name = pName;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.XmlImportType#getContent()
   */
  @Override
  public List<Object> getContent() {
    if (content == null) {
      content = new ArrayList<>();
    }
    return this.content;
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

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.XmlImportType#getParamName()
   */
  @Override
  public String getName() {
    return name;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.XmlImportType#setParamName(java.lang.String)
   */
  @Override
  public void setName(final String value) {
    this.name = value;
  }

  public static XmlDefineType get(IXmlDefineType pExport) {
    if (pExport instanceof XmlDefineType) { return (XmlDefineType) pExport; }
    XmlDefineType result = new XmlDefineType();
    result.content = pExport.getContent();
    result.refName = pExport.getRefName();
    result.refNode = pExport.getRefNode();
    result.name = pExport.getName();
    result.setPath(pExport.getPath());
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
            processData = new ProcessData(name, origpair.getDocumentFragment());
          } else {
            processData = new ProcessData(name, (NodeList) getXPath().evaluate(origpair.getNodeValue(), XPathConstants.NODESET));
          }
        } catch (XPathExpressionException e) {
          throw new RuntimeException(e);
        }
      }
    } else {
      processData = null;
    }
    if (content!=null && content.size()>0) {
      List<Node> result = PETransformer.create(processData).transform(content);
      return new ProcessData(name, result);
    } else {
      return processData;
    }
  }

}
