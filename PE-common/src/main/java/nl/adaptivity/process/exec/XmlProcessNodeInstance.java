//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2014.08.06 at 08:14:28 PM BST
//


package nl.adaptivity.process.exec;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import nl.adaptivity.process.engine.ProcessData;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Node;

import nl.adaptivity.process.exec.IProcessNodeInstance.TaskState;


/**
 * <p>
 * Java class for XmlProcessNodeInstance complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 *
 * <pre>
 * &lt;complexType name="XmlProcessNodeInstance">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="predecessor" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="body">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;any maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *       &lt;attribute name="handle" use="required" type="{http://www.w3.org/2001/XMLSchema}long" />
 *       &lt;attribute name="state" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="processinstance" type="{http://www.w3.org/2001/XMLSchema}long" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="nodeInstance")
public class XmlProcessNodeInstance {

  protected List<Long> predecessors;

  @XmlElement(required = true)
  protected XmlProcessNodeInstance.Body body;

  @XmlAttribute(name = "handle", required = true)
  protected long handle = -1L;

  @Nullable private TaskState mState;

  private long processinstance = -1;

  @XmlAttribute(name = "nodeid")
  private String mNodeId;
  private List<ProcessData> mResults;

  public XmlProcessNodeInstance() {}

  /**
   * Gets the value of the predecessor property.
   */
  @XmlElement(name="predecessor")
  public List<Long> getPredecessors() {
    if (predecessors == null) {
      predecessors = new ArrayList<>();
    }
    return predecessors;
  }

  /**
   * Gets the value of the body property.
   *
   * @return possible object is {@link XmlProcessNodeInstance.Body }
   */
  public XmlProcessNodeInstance.Body getBody() {
    return body;
  }

  /**
   * Sets the value of the body property.
   *
   * @param value allowed object is {@link XmlProcessNodeInstance.Body }
   */
  public void setBody(final XmlProcessNodeInstance.Body value) {
    this.body = value;
  }

  /**
   * Gets the value of the handle property.
   */
  public long getHandle() {
    return handle;
  }

  /**
   * Sets the value of the handle property.
   */
  public void setHandle(final long value) {
    this.handle = value;
  }

  /**
   * Gets the value of the state property.
   *
   * @return possible object is {@link String }
   */
  @XmlAttribute(name = "state")
  public String getStateXml() {
    return mState.name();
  }

  /**
   * Sets the value of the state property.
   *
   * @param value allowed object is {@link TaskState }
   */
  public void setStateXml(@Nullable final String value) {
    if (value == null) {
      mState = null;
    } else {
      mState = TaskState.valueOf(value);
    }
  }


  @Nullable
  public TaskState getState() {
    return mState;
  }

  public void setState(final TaskState state) {
    mState = state;
  }

  /**
   * Gets the value of the processinstance property.
   *
   * @return possible object is {@link Long }
   */
  @Nullable
  @XmlAttribute(name="processintance")
  public Long getXmlProcessinstance() {
    return processinstance==-1L ? null : Long.valueOf(processinstance);
  }

  /**
   * Sets the value of the processinstance property.
   *
   * @param value allowed object is {@link Long }
   */
  public void setXmlProcessinstance(@Nullable final Long value) {
    this.processinstance = value==null ? -1L : value.longValue();
  }

  /**
   * Gets the value of the processinstance property.
   *
   * @return possible object is {@link Long }
   */
  public long getProcessinstance() {
    return processinstance;
  }

  /**
   * Sets the value of the processinstance property.
   *
   * @param value allowed object is {@link Long }
   */
  public void setProcessinstance(final long value) {
    this.processinstance = value;
  }


  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Body {

    @XmlAnyElement(lax = true)
    protected List<Object> any;

    public Body() {}

    public Body(final Node node) {
      getAny().add(node);
    }

    public List<Object> getAny() {
      if (any == null) {
        any = new ArrayList<>();
      }
      return this.any;
    }

  }


  public void setNodeId(final String id) {
    mNodeId = id;
  }

  public String getNodeId() {
    return mNodeId;
  }

  public void setResults(final List<ProcessData> results) {
    mResults = results;
  }

  @XmlElement(name = "result")
  public List<ProcessData> getResults() {
    return mResults;
  }

}
