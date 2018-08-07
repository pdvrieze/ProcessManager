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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.sync;

import android.content.ContentProviderClient;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.os.RemoteException;
import nl.adaptivity.sync.RemoteXmlSyncAdapterDelegate.DelegatingResources;
import nl.adaptivity.xmlutil.XmlException;

import java.io.IOException;


public interface ISyncAdapterDelegate {

  void updateItemDetails(DelegatingResources delegator, ContentProviderClient provider, SyncResult syncResult) throws RemoteException, IOException,
                                                                                                                      OperationApplicationException;

  void updateListFromServer(DelegatingResources delegator, ContentProviderClient provider, SyncResult syncResult) throws RemoteException, XmlException,
                                                                                                                         IOException, OperationApplicationException;

}
