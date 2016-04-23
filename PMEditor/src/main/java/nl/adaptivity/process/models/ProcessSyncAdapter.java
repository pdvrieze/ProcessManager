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

package nl.adaptivity.process.models;

import android.content.Context;
import nl.adaptivity.process.ui.main.SettingsActivity;
import nl.adaptivity.sync.DelegatingRemoteXmlSyncAdapter;

import java.net.URI;
import java.util.Arrays;

public class ProcessSyncAdapter extends DelegatingRemoteXmlSyncAdapter {

  public ProcessSyncAdapter(final Context context) {
    super(context, true, false, Arrays.asList(new ProcessModelSyncAdapter(), new ProcessInstanceSyncAdapter()));
  }

  @Override
  public URI getSyncSource() {
    return SettingsActivity.getSyncSource(getContext());
  }

  protected String getListUrl(final String base) {
    return base+"/processModels";
  }


}