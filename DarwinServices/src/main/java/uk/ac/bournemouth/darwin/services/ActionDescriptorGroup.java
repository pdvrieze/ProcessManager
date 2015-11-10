package uk.ac.bournemouth.darwin.services;

import nl.adaptivity.process.util.Constants;

import javax.xml.bind.annotation.*;

import java.util.Collection;


@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "actionGroup", namespace = Constants.USER_MESSAGE_HANDLER_NS)
public class ActionDescriptorGroup {

  @XmlAttribute(name = "title")
  String mTitle;

  @XmlElement(name = ActionDescriptor.ELEMENTNAME)
  Collection<ActionDescriptor> mActions;


  public String getTitle() {
    return mTitle;
  }


  public void setTitle(final String title) {
    mTitle = title;
  }


  public Collection<ActionDescriptor> getActions() {
    return mActions;
  }


  public void setActions(final Collection<ActionDescriptor> actions) {
    mActions = actions;
  }


}
