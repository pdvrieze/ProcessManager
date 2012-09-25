package uk.ac.bournemouth.darwin.services;

import java.net.URI;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.xml.JaxbUriAdapter;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="action", namespace=Constants.USER_MESSAGE_HANDLER_NS)
public class ActionDescriptor {
  
  private String aTitle;
  private String aDescription;
  private URI aIcon;
  private URI aLocation;
  
  @XmlAttribute(name="title")
  public String getTitle() {
    return aTitle;
  }
  
  public void setTitle(String pTitle) {
    aTitle = pTitle;
  }

  @XmlValue
  public String getDescription() {
    return aDescription;
  }

  public void setDescription(String pDescription) {
    aDescription = pDescription;
  }

  @XmlAttribute(name="icon")
  @XmlJavaTypeAdapter(type=URI.class, value=JaxbUriAdapter.class)
  public URI getIcon() {
    return aIcon;
  }

  
  public void setIcon(URI pIcon) {
    aIcon = pIcon;
  }

  
  @XmlAttribute(name="href")
  @XmlJavaTypeAdapter(type=URI.class, value=JaxbUriAdapter.class)
  public URI getLocation() {
    return aLocation;
  }

  
  public void setLocation(URI pLocation) {
    aLocation = pLocation;
  }
  

}
