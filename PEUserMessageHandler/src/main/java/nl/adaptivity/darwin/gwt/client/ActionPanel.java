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

package nl.adaptivity.darwin.gwt.client;

import nl.adaptivity.gwt.base.client.CompletionListener;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiTemplate;


public class ActionPanel {

  private static ActionPanelUiBinder groupUiBinder = GWT.create(ActionPanelUiBinder.class);

  @UiTemplate("ActionPanelGroup.ui.xml")
  interface ActionPanelUiBinder extends UiBinder<Element, ActionPanel> { /**/}

  DivElement mDivElement;

  public ActionPanel() {
    //    mDivElement = Document.get().createDivElement();
    //    setElement(mDivElement);
  }

  public static void load(final CompletionListener completionListener) {
    new RequestBuilder(RequestBuilder.GET, "PEUserMessageHandler/actions");

    // TODO Auto-generated method stub
    // 
    throw new UnsupportedOperationException("Not yet implemented");
  }

}
