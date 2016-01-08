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

import java.util.ArrayList;
import java.util.Collection;

import nl.adaptivity.gwt.base.client.MyFormPanel.ResetEvent;
import nl.adaptivity.gwt.base.client.MyFormPanel.ResetHandler;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.Widget;


/**
 * An extended upload that can have a change handler.
 * 
 * @author Paul de Vrieze
 */
public class MyFileUpload extends FileUpload implements IWidgetController, ChangeHandler, ResetHandler {

  private Collection<FocusWidget> mWidgetsToEnable;

  private HandlerRegistration mResetHandler;

  private HandlerRegistration mChangeHandler;

  public MyFileUpload() {
    super();
  }

  private HandlerRegistration registerResetHandler() {
    Widget parent = getParent();
    while ((parent != null) && (!((parent instanceof MyFormPanel) || (parent instanceof FormPanel)))) {
      parent = parent.getParent();
    }
    if (parent != null) {
      if (parent instanceof MyFormPanel) {
        final MyFormPanel form = (MyFormPanel) parent;
        return form.addResetHandler(this);

      } else if (parent instanceof FormPanel) {
        // Figure out if we can do something in this case
        //        FormPanel form = (FormPanel) parent;

      }
    }
    return null;
  }

  public MyFileUpload(final Element element) {
    super(element);
  }

  public HandlerRegistration registerChangeHandler(final ChangeHandler handler) {
    return addDomHandler(handler, ChangeEvent.getType());
  }

  @Override
  public void addControlledWidget(final FocusWidget widget) {
    if (mWidgetsToEnable == null) {
      mWidgetsToEnable = new ArrayList<FocusWidget>();
      mChangeHandler = registerChangeHandler(this);
      mResetHandler = registerResetHandler();
    }
    mWidgetsToEnable.add(widget);
    widget.setEnabled(getFilename().length() > 0);
  }

  @Override
  public boolean removeControlledWidget(final FocusWidget widget) {
    final boolean result = mWidgetsToEnable.remove(widget);
    if (result) {
      if (mWidgetsToEnable.size() == 0) {
        mWidgetsToEnable = null;
        mResetHandler.removeHandler();
        mChangeHandler.removeHandler();
      }
    }
    return result;
  }

  @Override
  public void onChange(final ChangeEvent event) {
    refreshEnablement();
  }

  @Override
  public void onReset(final ResetEvent resetEvent) {

    refreshEnablement();
  }

  private void refreshEnablement() {
    final boolean enabled = getFilename().length() > 0;
    for (final FocusWidget widget : mWidgetsToEnable) {
      widget.setEnabled(enabled);
    }
  }

}
