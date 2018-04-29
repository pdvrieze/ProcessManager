/*
 * Copyright (c) 2018.
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

package uk.ac.bournemouth.darwin.services;

import nl.adaptivity.process.util.Constants;
import nl.adaptivity.rest.annotations.RestMethod;
import nl.adaptivity.rest.annotations.HttpMethod;
import nl.adaptivity.rest.annotations.RestParam;
import nl.adaptivity.rest.annotations.RestParamType;

import javax.xml.bind.annotation.XmlElementWrapper;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;


public class ServicesEndpoint {

  @XmlElementWrapper(name = "actionsGroups", namespace = Constants.DARWIN_NS)
  @RestMethod(method = HttpMethod.GET, path = "/actions")
  public Collection<ActionDescriptorGroup> getAvailableActions(@RestParam(type = RestParamType.PRINCIPAL) final Principal user) {
    final ArrayList<ActionDescriptorGroup> result = new ArrayList<>();

    // TODO actually get some actions out of the database and process model database.

    return result;
  }


}
