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
