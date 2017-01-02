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

package nl.adaptivity.messaging;

import nl.adaptivity.util.xml.SimpleXmlDeserializable;
import nl.adaptivity.xml.*;
import nl.adaptivity.xml.schema.annotations.XmlName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.JAXB;
import javax.xml.namespace.QName;

import java.net.URI;


/**
 * Simple pojo implementation of {@link EndpointDescriptor} that supports
 * serialization through {@link JAXB}.
 *
 * @author Paul de Vrieze
 */
@XmlDeserializer(EndpointDescriptorImpl.Factory.class)
public class EndpointDescriptorImpl implements EndpointDescriptor, XmlSerializable, SimpleXmlDeserializable {

  public static final String MY_JBI_NS = "http://adaptivity.nl/jbi";
  public static final String ELEMENTLOCALNAME = "endpointDescriptor";
  public static final QName ELEMENTNAME = new QName(MY_JBI_NS, ELEMENTLOCALNAME, "jbi");

  public static class Factory implements XmlDeserializerFactory<EndpointDescriptorImpl> {

    @Override
    public EndpointDescriptorImpl deserialize(final XmlReader reader) throws XmlException {
      return EndpointDescriptorImpl.deserialize(reader);
    }
  }

  private String mServiceLocalName;

  private String mServiceNamespace;

  private String mEndpointName;

  private URI mEndpointLocation;

  public EndpointDescriptorImpl() {}

  public EndpointDescriptorImpl(@NotNull final QName serviceName, final String endpointName, final URI endpointLocation) {
    mServiceLocalName = serviceName.getLocalPart();
    mServiceNamespace = serviceName.getNamespaceURI();
    mEndpointName = endpointName;
    mEndpointLocation = endpointLocation;
  }


  private static EndpointDescriptorImpl deserialize(final XmlReader in) throws XmlException {
    return XmlUtil.<EndpointDescriptorImpl>deserializeHelper(new EndpointDescriptorImpl(), in);
  }

  @Override
  public boolean deserializeChild(final XmlReader in) throws XmlException {
    return false; // No children
  }

  @Override
  public boolean deserializeChildText(final CharSequence elementText) {
    return false; // No child text
  }

  @Override
  public boolean deserializeAttribute(@NotNull final CharSequence attributeNamespace, @NotNull final CharSequence attributeLocalName, @NotNull final CharSequence attributeValue) {
    switch (attributeLocalName.toString()) {
      case "endpointLocation": mEndpointLocation = URI.create(attributeValue.toString()); return true;
      case "endpointName": mEndpointName = attributeValue.toString(); return true;
      case "serviceLocalName": mServiceLocalName = attributeValue.toString(); return true;
      case "serviceNS": mServiceNamespace = attributeValue.toString(); return true;
    }
    return false;
  }

  @Override
  public void onBeforeDeserializeChildren(@NotNull final XmlReader reader) {
    // do nothing
  }

  @NotNull
  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  @Override
  public void serialize(@NotNull final XmlWriter out) throws XmlException {
    XmlWriterUtil.smartStartTag(out, ELEMENTNAME);
    XmlWriterUtil.writeAttribute(out, "endpointLocation", mEndpointLocation!=null ? mEndpointLocation.toString() : null );
    XmlWriterUtil.writeAttribute(out, "endpointName", mEndpointName);
    XmlWriterUtil.writeAttribute(out, "serviceLocalName", mServiceLocalName);
    XmlWriterUtil.writeAttribute(out, "serviceNS", mServiceNamespace);

    XmlWriterUtil.endTag(out, ELEMENTNAME);
  }

  @Override
  public String getEndpointName() {
    return mEndpointName;
  }

  public void setEndpointName(final String endpointName) {
    mEndpointName = endpointName;
  }

  @XmlName("endpointLocation")
  String getEndpointLocationString() {
    return mEndpointLocation.toString();
  }

  void setEndpointLocationString(@NotNull final String location) {
    mEndpointLocation = URI.create(location);
  }

  @Override
  public URI getEndpointLocation() {
    return mEndpointLocation;
  }

  public void setEndpointLocation(final URI location) {
    mEndpointLocation = location;
  }

  String getServiceLocalName() {
    return mServiceLocalName;
  }

  void setServiceLocalName(final String localName) {
    mServiceLocalName = localName;
  }

  @XmlName("serviceNS")
  String getServiceNamespace() {
    return mServiceNamespace;
  }

  void setServiceNamespace(final String serviceNamespace) {
    mServiceNamespace = serviceNamespace;
  }

  @NotNull
  @Override
  public QName getServiceName() {
    return new QName(mServiceNamespace, mServiceLocalName);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((mEndpointLocation == null) ? 0 : mEndpointLocation.hashCode());
    result = prime * result + ((mEndpointName == null) ? 0 : mEndpointName.hashCode());
    result = prime * result + ((mServiceLocalName == null) ? 0 : mServiceLocalName.hashCode());
    result = prime * result + ((mServiceNamespace == null) ? 0 : mServiceNamespace.hashCode());
    return result;
  }

  @Override
  public boolean equals(@Nullable final Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final EndpointDescriptorImpl other = (EndpointDescriptorImpl) obj;
    if (mEndpointLocation == null) {
      if (other.mEndpointLocation != null)
        return false;
    } else if (!mEndpointLocation.equals(other.mEndpointLocation))
      return false;
    if (mEndpointName == null) {
      if (other.mEndpointName != null)
        return false;
    } else if (!mEndpointName.equals(other.mEndpointName))
      return false;
    if (mServiceLocalName == null) {
      if (other.mServiceLocalName != null)
        return false;
    } else if (!mServiceLocalName.equals(other.mServiceLocalName))
      return false;
    if (mServiceNamespace == null) {
      if (other.mServiceNamespace != null)
        return false;
    } else if (!mServiceNamespace.equals(other.mServiceNamespace))
      return false;
    return true;
  }

  @Override
  public boolean isSameService(final EndpointDescriptor other) {
    return mServiceNamespace.equals(other.getServiceName().getNamespaceURI()) && mServiceLocalName.equals(other.getServiceName().getLocalPart()) && mEndpointName.equals(other.getEndpointName());
  }

}
