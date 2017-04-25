/*
 * Copyright (c) 2017.
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

package nl.adaptivity.xml;

import android.app.Application;
import android.os.StrictMode;


/**
 * Simple application that takes care to register the correct streaming factory.
 */
public class XmlApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();
    // Don't use standard stax as it is not available on android.
    XmlStreaming.setFactory(new AndroidStreamingFactory());

    if (nl.adaptivity.xml.BuildConfig.DEBUG) {
      StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyDeath().build());
    }
  }
}
