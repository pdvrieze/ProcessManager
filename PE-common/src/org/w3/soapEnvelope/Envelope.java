//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2009.09.24 at 08:12:58 PM CEST
//


package org.w3.soapEnvelope;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;


/**
 * <p>
 * Java class for Envelope complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * 
 * <pre>
 * &lt;complexType name="Envelope">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.w3.org/2003/05/soap-envelope}Header" minOccurs="0"/>
 *         &lt;element ref="{http://www.w3.org/2003/05/soap-envelope}Body"/>
 *       &lt;/sequence>
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = Envelope.ELEMENTNAME, propOrder = { "header", "body" })
@XmlRootElement(name = Envelope.ELEMENTNAME, namespace = Envelope.NAMESPACE)
public class Envelope {

  public static final String NAMESPACE = "http://www.w3.org/2003/05/soap-envelope";

  public static final String ELEMENTNAME = "Envelope";

  public static final String MIMETYPE = "application/soap+xml";

  @XmlElement(name = Header.ELEMENTNAME)
  protected Header header;

  @XmlElement(name = Body.ELEMENTNAME, required = true)
  protected Body body;

  @XmlAnyAttribute
  private final Map<QName, String> otherAttributes = new HashMap<QName, String>();

  private URI encodingStyle;

  /**
   * Gets the value of the header property.
   * 
   * @return possible object is {@link Header }
   */
  public Header getHeader() {
    return header;
  }

  /**
   * Sets the value of the header property.
   * 
   * @param value allowed object is {@link Header }
   */
  public void setHeader(final Header value) {
    this.header = value;
  }

  /**
   * Gets the value of the body property.
   * 
   * @return possible object is {@link Body }
   */
  public Body getBody() {
    return body;
  }

  /**
   * Sets the value of the body property.
   * 
   * @param value allowed object is {@link Body }
   */
  public void setBody(final Body value) {
    this.body = value;
  }

  /**
   * Gets a map that contains attributes that aren't bound to any typed property
   * on this class.
   * <p>
   * the map is keyed by the name of the attribute and the value is the string
   * value of the attribute. the map returned by this method is live, and you
   * can add new attribute by updating the map directly. Because of this design,
   * there's no setter.
   * 
   * @return always non-null
   */
  public Map<QName, String> getOtherAttributes() {
    return otherAttributes;
  }

  @XmlAttribute(name = "encodingStyle")
  public URI getEncodingStyle() {
    return encodingStyle;
  }

  public void setEncodingStyle(final URI encodingStyle) {
    this.encodingStyle = encodingStyle;
  }

}
