package uk.ac.bournemouth.darwin.services;

import nl.adaptivity.process.util.Constants;

import javax.xml.bind.annotation.*;

import java.util.Collection;


@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "actionGroup", namespace = Constants.USER_MESSAGE_HANDLER_NS)
public class ActionDescriptorGroup {

  @XmlAttribute(name = "title")
  String aTitle;

  @XmlElement(name = ActionDescriptor.ELEMENTNAME)
  Collection<ActionDescriptor> aActions;


  public String getTitle() {
    return aTitle;
  }


  public void setTitle(final String title) {
    aTitle = title;
  }


  public Collection<ActionDescriptor> getActions() {
    return aActions;
  }


  public void setActions(final Collection<ActionDescriptor> actions) {
    aActions = actions;
  }


}
