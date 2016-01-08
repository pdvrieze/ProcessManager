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

package nl.adaptivity.process.userMessageHandler.client;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalSplitPanel;
import com.google.gwt.user.client.ui.Widget;


public class SplittedPanel extends Composite {

  private final HorizontalSplitPanel mMainPanel;

  public SplittedPanel() {
    mMainPanel = new HorizontalSplitPanel();

    int pos = (2 * Math.max(Window.getClientWidth(), 400)) / 9;
    pos = Math.max(pos, 100);
    mMainPanel.setSplitPosition(pos + "px");

    initWidget(mMainPanel);
  }

  protected void setLeftWidget(final Widget widget) {
    mMainPanel.setLeftWidget(widget);
  }

  protected void setRightWidget(final Widget widget) {
    mMainPanel.setRightWidget(widget);
  }

  public Widget getLeftWidget() {
    return mMainPanel.getLeftWidget();
  }

  public Widget getRightWidget() {
    return mMainPanel.getRightWidget();
  }
}
