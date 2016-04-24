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


import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.Arrays;

public abstract class RemoteXmlSyncAdapter extends DelegatingRemoteXmlSyncAdapter implements ISimpleSyncDelegate {

  public interface XmlBaseColumns extends BaseColumns {
    String COLUMN_SYNCSTATE = "syncstate";
  }

  public interface ContentValuesProvider {
    /** The values that need to be stored in the database for the item. */
    ContentValues getContentValues();

    /** Indicate whether the value has further details to parse. This allows different sync state depending on update
     * or new.
     * @return true if details need to be synced.
     */
    boolean syncDetails();
  }

  public static class SimpleContentValuesProvider implements ContentValuesProvider {

    private final ContentValues mContentValues;
    private final boolean mSyncDetails;

    public SimpleContentValuesProvider(final ContentValues contentValues, final boolean syncDetails) {
      mContentValues = contentValues;
      mSyncDetails = syncDetails;
    }

    @Override
    public ContentValues getContentValues() {
      return mContentValues;
    }

    public boolean syncDetails() {
      return mSyncDetails;
    }
  }

  public static class CVPair implements Comparable<CVPair> {
    public final ContentValuesProvider mCV;
    public final long mId;

    public CVPair(final long id, final ContentValuesProvider cV) {
      mCV = cV;
      mId = id;
    }

    @Override
    public int compareTo(final CVPair another) {
      final long rhs = another.mId;
      return mId < rhs ? -1 : (mId == rhs ? 0 : 1);
    }

  }

  public static final int SYNC_PUBLISH_TO_SERVER = 6;
  public static final int SYNC_DELETE_ON_SERVER = 7;
  public static final int SYNC_UPDATE_SERVER = 1;
  public static final int SYNC_UPTODATE = 0;
  public static final int SYNC_PENDING = 2;
  public static final int SYNC_NEWDETAILSPENDING = 3;
  public static final int SYNC_DETAILUPDATEPENDING = 8;
  public static final int SYNC_UPDATE_SERVER_PENDING = 4;
  public static final int SYNC_UPDATE_SERVER_DETAILSPENDING = 5;

  RemoteXmlSyncAdapterDelegate mCoordinator;

  public RemoteXmlSyncAdapter(final Context context, final boolean autoInitialize, final Uri listContentUri) {
    super(context, autoInitialize, null);
    init(listContentUri);
  }

  private void init(final Uri listContentUri) {
    listContentUri.buildUpon().encodedFragment("nonetnotify").build();
    mCoordinator = new RemoteXmlSyncAdapterDelegate(listContentUri, this);
    setDelegates(Arrays.asList(mCoordinator));
  }

  public RemoteXmlSyncAdapter(final Context context, final boolean autoInitialize, final boolean allowParallelSyncs, final Uri listContentUri) {
    super(context, autoInitialize, allowParallelSyncs, null);
    init(listContentUri);
  }

  @Override
  public String getListSelection() {
    return null;
  }

  @Override
  public String[] getListSelectionArgs() {
    return null;
  }

}