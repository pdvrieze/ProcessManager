package uk.ac.bournemouth.darwin.services;

import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.xml.JaxbUriAdapter;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.net.URI;


@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = ActionDescriptor.ELEMENTNAME, namespace = Constants.USER_MESSAGE_HANDLER_NS)
public class ActionDescriptor {

  static final String ELEMENTNAME = "action";

  private String aTitle;

  private String aDescription;

  private URI aIcon;

  private URI aLocation;

  @XmlAttribute(name = "title")
  public String getTitle() {
    return aTitle;
  }

  public void setTitle(final String title) {
    aTitle = title;
  }

  @XmlValue
  public String getDescription() {
    return aDescription;
  }

  public void setDescription(final String description) {
    aDescription = description;
  }

  @XmlAttribute(name = "icon")
  @XmlJavaTypeAdapter(type = URI.class, value = JaxbUriAdapter.class)
  public URI getIcon() {
    return aIcon;
  }


  public void setIcon(final URI icon) {
    aIcon = icon;
  }


  @XmlAttribute(name = "href")
  @XmlJavaTypeAdapter(type = URI.class, value = JaxbUriAdapter.class)
  public URI getLocation() {
    return aLocation;
  }


  public void setLocation(final URI location) {
    aLocation = location;
  }


}
