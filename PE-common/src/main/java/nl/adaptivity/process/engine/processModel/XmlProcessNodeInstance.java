//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2014.08.06 at 08:14:28 PM BST
//


package nl.adaptivity.process.engine.processModel;

import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.process.engine.ProcessData;
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.TaskState;
import nl.adaptivity.util.xml.*;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlWriter;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.List;

@XmlDeserializer(XmlProcessNodeInstance.Factory.class)
public class XmlProcessNodeInstance implements /*IProcessNodeInstance<XmlProcessNodeInstance>,*/ SimpleXmlDeserializable, XmlSerializable {

  public static class Factory implements XmlDeserializerFactory<XmlProcessNodeInstance> {

    @Override
    public XmlProcessNodeInstance deserialize(final XmlReader in) throws XmlException {
      return XmlProcessNodeInstance.deserialize(in);
    }
  }


  public static final String ELEMENTLOCALNAME = "nodeInstance";
  public static final QName ELEMENTNAME = new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);
  private static final String PREDECESSOR_LOCALNAME = "predecessor";
  static final QName PREDECESSOR_ELEMENTNAME = new QName(Engine.NAMESPACE, PREDECESSOR_LOCALNAME, Engine.NSPREFIX);
  private static final String RESULT_LOCALNAME = "result";
  private static final QName RESULT_ELEMENTNAME = new QName(Engine.NAMESPACE, RESULT_LOCALNAME, Engine.NSPREFIX);
  private static final String BODY_LOCALNAME = "body";
  static final QName BODY_ELEMENTNAME = new QName(Engine.NAMESPACE, BODY_LOCALNAME, Engine.NSPREFIX);

  private List<Long> mPredecessors;

  @XmlElement(required = true)
  protected CompactFragment mBody;

  @XmlAttribute(name = "handle", required = true)
  protected long mHandle = -1L;

  @Nullable private TaskState mState;

  private long mProcessInstance = -1;

  @XmlAttribute(name = "nodeid")
  private String mNodeId;

  private List<ProcessData> mResults;
  public XmlProcessNodeInstance() {}

  public static XmlProcessNodeInstance deserialize(final XmlReader in) throws XmlException {
    return XmlUtil.deserializeHelper(new XmlProcessNodeInstance(), in);
  }

  /**
   * Gets the value of the predecessor property.
   */
  @XmlElement(name="predecessor")
  public List<Long> getPredecessors() {
    if (mPredecessors == null) {
      mPredecessors = new ArrayList<>();
    }
    return mPredecessors;
  }

  @Override
  public boolean deserializeChild(final XmlReader in) throws XmlException {
    if (XmlUtil.isElement(in, Engine.NAMESPACE, "predecessor")) {
      if (mPredecessors == null) { mPredecessors = new ArrayList<>(); }
      mPredecessors.add(Long.parseLong(XmlUtil.readSimpleElement(in).toString()));
    } else if (XmlUtil.isElement(in, Engine.NAMESPACE, "body")) {
      mBody = XmlUtil.readerToFragment(in);
    } else if (XmlUtil.isElement(in, ProcessData.ELEMENTNAME)) {
      if (mResults==null) { mResults = new ArrayList<>(); }
      mResults.add(ProcessData.deserialize(in));
    }
    return false;
  }

  @Override
  public boolean deserializeChildText(final CharSequence elementText) {
    return false;
  }

  @Override
  public boolean deserializeAttribute(final CharSequence attributeNamespace, final CharSequence attributeLocalName, final CharSequence attributeValue) {
    switch (attributeLocalName.toString()) {
      case "state": mState = TaskState.valueOf(attributeValue.toString()); return true;
      case "processinstance": mProcessInstance = Long.parseLong(attributeValue.toString()); return true;
      case "handle":mHandle =  Long.parseLong(attributeValue.toString()); return true;
      case "nodeid": mNodeId = attributeValue.toString(); return true;
    }
    return false;
  }

  @Override
  public void onBeforeDeserializeChildren(final XmlReader in) throws XmlException {

  }

  @Override
  public QName getElementName() {
    return null;
  }

  /**
   * Gets the value of the body property.
   *
   * @return The body
   */
  public CompactFragment getBody() {
    return mBody;
  }

  /**
   * Sets the value of the body property.
   *
   * @param value The body
   */
  public void setBody(final CompactFragment value) {
    this.mBody = value;
  }

  /**
   * Gets the value of the handle property.
   */
  public long getHandle() {
    return mHandle;
  }

  /**
   * Sets the value of the handle property.
   */
  public void setHandle(final long value) {
    this.mHandle = value;
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
  @XmlAttribute(name="processinstance")
  public Long getXmlProcessinstance() {
    return mProcessInstance == -1L ? null : Long.valueOf(mProcessInstance);
  }

  /**
   * Sets the value of the processinstance property.
   *
   * @param value allowed object is {@link Long }
   */
  public void setXmlProcessinstance(@Nullable final Long value) {
    this.mProcessInstance = value == null ? -1L : value.longValue();
  }

  /**
   * Gets the value of the processinstance property.
   *
   * @return possible object is {@link Long }
   */
  public long getProcessInstance() {
    return mProcessInstance;
  }

  /**
   * Sets the value of the processinstance property.
   *
   * @param value allowed object is {@link Long }
   */
  public void setProcessInstance(final long value) {
    this.mProcessInstance = value;
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

  @Override
  public void serialize(final XmlWriter out) throws XmlException {
    XmlUtil.writeStartElement(out, ELEMENTNAME);
    if (mState!=null) { XmlUtil.writeAttribute(out, "state", mState.name()); }
    if (mProcessInstance!=-1) { XmlUtil.writeAttribute(out, "processinstance", mProcessInstance); }
    if (mHandle!=-1) { XmlUtil.writeAttribute(out, "handle", mHandle); }
    if (mNodeId!=null) { XmlUtil.writeAttribute(out, "nodeid", mNodeId); }
    if (mPredecessors!=null) {
      for (Long predecessor: mPredecessors) {
        XmlUtil.writeSimpleElement(out, PREDECESSOR_ELEMENTNAME, predecessor.toString());
      }
    }
    if (mResults!=null) {
      for (ProcessData result: mResults) {
        result.serialize(out);
      }
    }
    if (mBody!=null) {
      XmlUtil.writeStartElement(out, BODY_ELEMENTNAME);
      mBody.serialize(XmlUtil.stripMetatags(out));
      XmlUtil.writeEndElement(out, BODY_ELEMENTNAME);
    }
    XmlUtil.writeEndElement(out, ELEMENTNAME);
  }

}
