package uk.ac.bournemouth.darwin.services;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;

import nl.adaptivity.messaging.EndpointDescriptor;
import nl.adaptivity.messaging.IMessenger;
import nl.adaptivity.messaging.MessagingRegistry;
import nl.adaptivity.process.messaging.GenericEndpoint;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.rest.annotations.RestMethod;
import nl.adaptivity.rest.annotations.RestMethod.HttpMethod;


public class MessagingEndpoint implements GenericEndpoint {

  @XmlRootElement(namespace=Constants.DARWIN_NS, name="endpoint")
  private static class XmlEndpointDescriptor {

    @XmlAttribute(name="service")
    private QName mService;
    @XmlAttribute(name="endpoint")
    private String mEndpoint;
    @XmlAttribute(name="url")
    private URI mLocation;

    public XmlEndpointDescriptor() {}

    public XmlEndpointDescriptor(EndpointDescriptor endpoint) {
      mService = endpoint.getServiceName();
      mEndpoint = endpoint.getEndpointName();
      mLocation = endpoint.getEndpointLocation();
    }

  }

  public static final String ENDPOINT = "messaging";

  public static final QName SERVICENAME = new QName(Constants.DARWIN_NS, "messaging");

  private URI mURI;

  private EndpointDescriptor mEndpointDescriptor;

  public MessagingEndpoint() {
  }

  @Override
  public QName getServiceName() {
    return SERVICENAME;
  }

  @Override
  public String getEndpointName() {
    return ENDPOINT;
  }

  @Override
  public URI getEndpointLocation() {
    return mURI;
  }

  @Override
  public void initEndpoint(final ServletConfig config) {
    IMessenger messenger = MessagingRegistry.getMessenger();
    if (messenger!=null) {
      final StringBuilder path = new StringBuilder(config.getServletContext().getContextPath());
      path.append("/endpoints");
      mEndpointDescriptor = messenger.registerEndpoint(SERVICENAME, ENDPOINT, URI.create(path.toString()));
    } else {
      mEndpointDescriptor = null;
    }
  }

  @XmlElementWrapper(name = "endpoints", namespace = Constants.DARWIN_NS)
  @RestMethod(method = HttpMethod.GET, path = "/endpoints")
  public List<XmlEndpointDescriptor> getEndpoints() {
    IMessenger messenger = MessagingRegistry.getMessenger();
    List<EndpointDescriptor> endpoints = messenger.getRegisteredEndpoints();
    ArrayList<XmlEndpointDescriptor> result = new ArrayList<>(endpoints.size());
    for(EndpointDescriptor endpoint: endpoints) {
      result.add(new XmlEndpointDescriptor(endpoint));
    }
    return result;
  }

  @Override
  public void destroy() {
    if (mEndpointDescriptor!=null) {
      final IMessenger messenger = MessagingRegistry.getMessenger();
      if (messenger!=null) {
        messenger.unregisterEndpoint(mEndpointDescriptor);
      }
      mEndpointDescriptor = null;
    }

  }

}
