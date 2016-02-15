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

package nl.adaptivity.sync;

import java.io.IOException;
import java.net.HttpURLConnection;

import nl.adaptivity.android.darwin.AuthenticatedWebClient.GetRequest;
import nl.adaptivity.sync.RemoteXmlSyncAdapterDelegate.DelegatingResources;

import nl.adaptivity.xml.XmlException;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentProviderClient;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.os.RemoteException;


public interface ISyncAdapterDelegate {

  public void updateItemDetails(DelegatingResources delegator, ContentProviderClient provider, SyncResult syncResult) throws RemoteException, IOException,
      OperationApplicationException;

  public void updateListFromServer(DelegatingResources delegator, ContentProviderClient provider, SyncResult syncResult) throws RemoteException, XmlException,
      IOException, OperationApplicationException;

}
