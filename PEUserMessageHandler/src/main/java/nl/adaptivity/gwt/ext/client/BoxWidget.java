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

package nl.adaptivity.gwt.ext.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Widget;


public class BoxWidget extends Widget {


  private static BoxWidgetUiBinder uiBinder = GWT.create(BoxWidgetUiBinder.class);

  interface BoxWidgetUiBinder extends UiBinder<Element, BoxWidget> {}

  @UiField
  DivElement labelSpan;

  @UiField
  DivElement boxSpan;

  public BoxWidget(final String label) {
    setElement(uiBinder.createAndBindUi(this));
    labelSpan.setInnerText(label);
  }

  public DivElement getBox() {
    return boxSpan;
  }

  public String getLabel() {
    return labelSpan.getInnerHTML();
  }

  public void setLabel(final String innerHTML) {
    labelSpan.setInnerHTML(innerHTML);
  }

}
