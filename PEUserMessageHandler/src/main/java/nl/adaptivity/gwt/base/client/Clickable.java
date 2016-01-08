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

package nl.adaptivity.gwt.base.client;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;


public class Clickable extends Widget {

  public static Clickable wrap(final Element element) {
    assert Document.get().getBody().isOrHasChild(element);

    final Clickable clickable = new Clickable(element);

    clickable.onAttach();

    RootPanel.detachOnWindowClose(clickable);

    return clickable;

  }

  public Clickable(final Element element) {
    setElement(element);
  }

  public HandlerRegistration addClickHandler(final ClickHandler clickHandler) {
    return addDomHandler(clickHandler, ClickEvent.getType());
  }

  public static Clickable wrapNoAttach(final Element element) {
    assert Document.get().getBody().isOrHasChild(element);

    final Clickable clickable = new Clickable(element);
    clickable.onAttach();
    return clickable;
  }

}
