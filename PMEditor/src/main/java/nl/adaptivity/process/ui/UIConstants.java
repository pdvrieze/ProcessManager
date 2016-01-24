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

package nl.adaptivity.process.ui;

/**
 * Created by pdvrieze on 15/01/16.
 */
public final class UIConstants {

  public static final String KEY_ACTIVITY = "activity";
  public static final String KEY_ACTIVITY_ID = "activityId";
  public static final String KEY_PROCESSMODEL = "processmodel";
  public static final String KEY_TMPFILE = "tmpFile";
  public static final String KEY_NODE_POS = "node_pos";

  public static final int REQUEST_SAVE_PROCESSMODEL = 42;
  public static final int REQUEST_EXPORT_PROCESSMODEL_SVG = 43;
  public static final int REQUEST_SHARE_PROCESSMODEL_FILE = 44;
  public static final int REQUEST_SHARE_PROCESSMODEL_SVG = 45;
  public static final int REQUEST_EDIT_HUMAN = 12;

  private UIConstants() {}
}
