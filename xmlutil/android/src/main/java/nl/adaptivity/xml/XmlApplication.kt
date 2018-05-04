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

package nl.adaptivity.xml

import android.app.Application
import android.os.StrictMode
import nl.adaptivity.lib.xmlutil.android.BuildConfig


/**
 * Simple application that takes care to register the correct streaming factory.
 */
@Deprecated("No longer needed. With kotlin multiplatform the default android factory is already loaded by default", ReplaceWith("Application", "android.app.Application"))
class XmlApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Don't use standard stax as it is not available on android.
        XmlStreaming.setFactory(AndroidStreamingFactory())

        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectAll().penaltyDeath().build())
        }
    }
}
