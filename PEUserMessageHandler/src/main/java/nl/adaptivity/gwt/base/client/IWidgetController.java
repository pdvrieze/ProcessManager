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

import com.google.gwt.user.client.ui.FocusWidget;


public interface IWidgetController {

  /**
   * Add a widget to be automatically enabled based on whether an item is
   * allowed. This also sets the current enabled state based on the current list
   * state.
   */
  public void addControlledWidget(FocusWidget widget);

  public boolean removeControlledWidget(FocusWidget widget);

}