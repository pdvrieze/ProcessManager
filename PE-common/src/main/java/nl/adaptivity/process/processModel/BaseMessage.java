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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.processModel;

import nl.adaptivity.util.xml.CompactFragment;
import nl.adaptivity.util.xml.DomUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlWriter;
import nl.adaptivity.xml.XmlWriterUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import java.io.CharArrayReader;
import java.io.IOException;
import java.util.Arrays;


public abstract class BaseMessage extends XMLContainer implements IXmlMessage{

  private QName mService;
  private String mEndpoint;
  private String mOperation;
  private String mUrl;
  private String mMethod;
  private String mType;

  public BaseMessage() {
    super();
  }

  @Deprecated
  public BaseMessage(final QName service, final String endpoint, final String operation, final String url, final String method, final String contentType, final Node messageBody) throws
          XmlException {
    this(service, endpoint, operation, url, method, contentType, DomUtil.nodeToFragment(messageBody));
  }

  public BaseMessage(final QName service, final String endpoint, final String operation, final String url, final String method, final String contentType, final CompactFragment messageBody) {
    super(messageBody);
    this.mService = service;
    this.mEndpoint = endpoint;
    this.mOperation = operation;
    this.mUrl = url;
    this.mMethod = method;
    mType = contentType;
  }

  public BaseMessage(@NotNull final IXmlMessage message) throws XmlException {
    this(message.getService(),
         message.getEndpoint(),
         message.getOperation(),
         message.getUrl(),
         message.getMethod(),
         message.getContentType(),
         message.getMessageBody());
  }

  @Override
  protected void serializeAttributes(final XmlWriter out) throws XmlException {
    super.serializeAttributes(out);
    XmlWriterUtil.writeAttribute(out, "type", getContentType());
    XmlWriterUtil.writeAttribute(out, "serviceNS", getServiceNS());
    XmlWriterUtil.writeAttribute(out, "serviceName", getServiceName());
    XmlWriterUtil.writeAttribute(out, "endpoint", getEndpoint());
    XmlWriterUtil.writeAttribute(out, "operation", getOperation());
    XmlWriterUtil.writeAttribute(out, "url", getUrl());
    XmlWriterUtil.writeAttribute(out, "method", getMethod());
  }


  public boolean deserializeAttribute(final CharSequence attributeNamespace, final CharSequence attributeLocalName, final CharSequence attributeValue) {
    if (XMLConstants.NULL_NS_URI.equals(attributeNamespace)) {
      String value = attributeValue.toString();
      switch (attributeLocalName.toString()) {
        case "endpoint": mEndpoint = value; return true;
        case "operation": mOperation = value; return true;
        case "url": mUrl =value; return true;
        case "method": mMethod = value; return true;
        case "type": mType = value; return true;
        case "serviceNS": setServiceNS(value); return true;
        case "serviceName": setServiceName(value); return true;
      }
    }
    return false;
  }

  @Nullable
  @Override
  public String getServiceName() {
    return mService ==null ? null : mService.getLocalPart();
  }

  @Override
  public void setServiceName(final String name) {
    if (mService == null) {
      mService = new QName(name);
    } else {
      mService = new QName(mService.getNamespaceURI(), name);
    }
  }

  @Nullable
  @Override
  public String getServiceNS() {
    return mService ==null ? null : mService.getNamespaceURI();
  }

  @Override
  public void setServiceNS(final String namespace) {
    if (mService == null) {
      mService = new QName(namespace, "xx");
    } else {
      mService = new QName(namespace, mService.getLocalPart());
    }
  }

  @Override
  public QName getService() {
    return mService;
  }

  @Override
  public void setService(final QName value) {
    this.mService = value;
  }

  @Override
  public String getEndpoint() {
    return mEndpoint;
  }

  @Override
  public void setEndpoint(final String value) {
    this.mEndpoint = value;
  }

  @Override
  public String getOperation() {
    return mOperation;
  }

  @NotNull
  @Override
  public CompactFragment getMessageBody() {
    return new CompactFragment(getOriginalNSContext(), getContent());
  }

  public DocumentFragment getMessageBodyNode() {
    try {
      return DomUtil.tryParseXmlFragment(new CharArrayReader(getContent()));
    } catch (@NotNull final IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setOperation(final String value) {
    this.mOperation = value;
  }

  @Override
  public String getUrl() {
    return mUrl;
  }

  @Override
  public void setUrl(final String value) {
    this.mUrl = value;
  }

  @Override
  public String getMethod() {
    return mMethod;
  }

  @Override
  public void setMethod(final String value) {
    this.mMethod = value;
  }

  @Override
  public String getContentType() {
    if (mType == null) {
      return "application/soap+xml";
    } else {
      return mType;
    }
  }

  @Override
  public void setType(final String type) {
    this.mType = type;
  }

  @Override
  public String toString() {
    return nl.adaptivity.xml.XmlUtil.toString(this);
  }

  @Override
  public boolean equals(@Nullable final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final BaseMessage that = (BaseMessage) o;

    if (mService != null ? !mService.equals(that.mService) : that.mService != null) return false;
    if (mEndpoint != null ? !mEndpoint.equals(that.mEndpoint) : that.mEndpoint != null) return false;
    if (mOperation != null ? !mOperation.equals(that.mOperation) : that.mOperation != null) return false;
    if (mUrl != null ? !mUrl.equals(that.mUrl) : that.mUrl != null) return false;
    if (mMethod != null ? !mMethod.equals(that.mMethod) : that.mMethod != null) return false;
    if (mType != null ? !mType.equals(that.mType) : that.mType != null) return false;

    final char[] body = getContent();
    final char[] thatBody = that.getContent();
    if (body==null) return thatBody==null;
    if (thatBody==null) return false;
    return Arrays.equals(body,thatBody);

  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (mService != null ? mService.hashCode() : 0);
    result = 31 * result + (mEndpoint != null ? mEndpoint.hashCode() : 0);
    result = 31 * result + (mOperation != null ? mOperation.hashCode() : 0);
    result = 31 * result + (mUrl != null ? mUrl.hashCode() : 0);
    result = 31 * result + (mMethod != null ? mMethod.hashCode() : 0);
    result = 31 * result + (mType != null ? mType.hashCode() : 0);
    return result;
  }
}