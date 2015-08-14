package uk.ac.bournemouth.darwin.services;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.annotation.XmlElementWrapper;

import nl.adaptivity.process.util.Constants;
import nl.adaptivity.rest.annotations.RestMethod;
import nl.adaptivity.rest.annotations.RestMethod.HttpMethod;
import nl.adaptivity.rest.annotations.RestParam;
import nl.adaptivity.rest.annotations.RestParam.ParamType;


public class ServicesEndpoint {

  @XmlElementWrapper(name = "actionsGroups", namespace = Constants.DARWIN_NS)
  @RestMethod(method = HttpMethod.GET, path = "/actions")
  public Collection<ActionDescriptorGroup> getAvailableActions(@RestParam(type = ParamType.PRINCIPAL) final Principal pUser) {
    final ArrayList<ActionDescriptorGroup> result = new ArrayList<>();

    // TODO actually get some actions out of the database and process model database.

    return result;
  }


}
