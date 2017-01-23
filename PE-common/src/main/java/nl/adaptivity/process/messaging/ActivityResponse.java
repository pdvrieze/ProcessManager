/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2012.09.15 at 03:25:47 PM BST
//


package nl.adaptivity.process.messaging;

import nl.adaptivity.process.ProcessConsts;
import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.process.engine.processModel.NodeInstanceState;
import nl.adaptivity.util.xml.SimpleXmlDeserializable;
import nl.adaptivity.ws.soap.SoapHelper;
import nl.adaptivity.xml.*;
import nl.adaptivity.xml.schema.annotations.XmlName;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;


/**
 * <p>
 * The ActivityResponse type is a class that helps with process aware methods.
 * When returning this class from a method this signifies to the
 * {@link SoapHelper} that the method is ProcessAware, and wraps an actual
 * return value. The activityResponse is communicated through the header.
 * </p>
 * <p>
 * When used by JAXB, instances will result in the right header that signifies
 * task awareness to the process engine. This allows responding to a SOAP
 * message to not signify task completion.
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 *
 * <pre>
 * &lt;complexType name="ActivityResponseType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="taskState">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="Sent"/>
 *             &lt;enumeration value="Acknowledged"/>
 *             &lt;enumeration value="Taken"/>
 *             &lt;enumeration value="Started"/>
 *             &lt;enumeration value="Complete"/>
 *             &lt;enumeration value="Failed"/>
 *             &lt;enumeration value="Cancelled"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 * @param <T> The type of the actual return value returned in the result of the
 *          SOAP message.
 */
@XmlDeserializer(ActivityResponse.Factory.class)
public class ActivityResponse<T> implements XmlSerializable, SimpleXmlDeserializable {

  public static final String NAMESPACE = ProcessConsts.Engine.NAMESPACE;

  public static final String ELEMENTLOCALNAME = "ActivityResponse";
  public static final QName ELEMENTNAME = new QName(NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);

  public static final String ATTRTASKSTATE = "taskState";

  public static class Factory implements XmlDeserializerFactory<ActivityResponse<?>>{

    @Override
    public ActivityResponse<?> deserialize(final XmlReader reader) throws XmlException {
      return ActivityResponse.deserialize(reader);
    }
  }

  private T mReturnValue;

  private Class<T> mReturnType;

  private NodeInstanceState mNodeInstanceState;

  /** Default constructor for jaxb use */
  protected ActivityResponse() {}

  /**
   * Create a new ActivityResponse.
   *
   * @param nodeInstanceState The state of the task requested.
   * @param returnType The actual return type of the method.
   * @param returnValue The value to return.
   */
  protected ActivityResponse(final NodeInstanceState nodeInstanceState, final Class<T> returnType, final T returnValue) {
    mNodeInstanceState = nodeInstanceState;
    mReturnType = returnType;
    mReturnValue = returnValue;
  }

  /**
   * Static helper factory for creating a new ActivityResponse.
   *
   * @param nodeInstanceState The state of the task requested.
   * @param returnType The actual return type of the method.
   * @param returnValue The value to return.
   * @return
   */
  @NotNull
  public static <V> ActivityResponse<V> create(final NodeInstanceState nodeInstanceState, final Class<V> returnType, final V returnValue) {
    return new ActivityResponse<>(nodeInstanceState, returnType, returnValue);
  }

  public static <T> ActivityResponse<T> deserialize(final XmlReader in) throws XmlException {
    return XmlUtil.<ActivityResponse<T>>deserializeHelper(new ActivityResponse<T>(),in);
  }

  @Override
  public boolean deserializeChild(@NotNull final XmlReader reader) {
    return false;
  }

  @Override
  public boolean deserializeChildText(final CharSequence elementText) {
    return false;
  }

  @Override
  public boolean deserializeAttribute(@NotNull final CharSequence attributeNamespace, @NotNull final CharSequence attributeLocalName, @NotNull final CharSequence attributeValue) {
    switch(attributeLocalName.toString()) {
      case "taskState" : setTaskStateString(attributeValue.toString()); return true;
    }
    return false;
  }

  @Override
  public void onBeforeDeserializeChildren(@NotNull final XmlReader reader) {

  }

  @NotNull
  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  @Override
  public void serialize(@NotNull final XmlWriter out) throws XmlException {
    XmlWriterUtil.smartStartTag(out, ELEMENTNAME);
    XmlWriterUtil.writeAttribute(out, "taskState", mNodeInstanceState.name());

    XmlWriterUtil.endTag(out, ELEMENTNAME);
  }

  /**
   * Gets the value of the taskState property.
   *
   * @return the name of the {@link NodeInstanceState}
   */
  @XmlName(ATTRTASKSTATE)
  public String getTaskStateString() {
    return mNodeInstanceState.name();
  }

  /**
   * Gets the value of the taskState property.
   *
   * @return the task state.
   */
  public NodeInstanceState getNodeInstanceState() {
    return mNodeInstanceState;
  }

  /**
   * Sets the value of the taskState property.
   *
   * @param value the new task state.
   */
  public void setTaskStateString(final String value) {
    mNodeInstanceState = NodeInstanceState.valueOf(value);
  }

  /**
   * Sets the value of the taskState property.
   *
   * @param nodeInstanceState the new task state.
   */
  public void setNodeInstanceState(final NodeInstanceState nodeInstanceState) {
    mNodeInstanceState = nodeInstanceState;
  }

  /**
   * Get the embedded return type.
   *
   * @return The embedded return type.
   */
  public Class<T> getReturnType() {
    return mReturnType;
  }

  /**
   * Get the actual return value.
   *
   * @return The actual return value.
   */
  public T getReturnValue() {
    return mReturnValue;
  }

}
