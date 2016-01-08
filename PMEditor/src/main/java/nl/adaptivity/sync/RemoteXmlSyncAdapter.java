package nl.adaptivity.sync;


import java.util.Arrays;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.BaseColumns;

public abstract class RemoteXmlSyncAdapter extends DelegatingRemoteXmlSyncAdapter implements ISimpleSyncDelegate {

  public interface XmlBaseColumns extends BaseColumns {
    public static final String COLUMN_SYNCSTATE = "syncstate";
  }

  public interface ContentValuesProvider {
    /** The values that need to be stored in the database for the item. */
    public ContentValues getContentValues();

    /** Indicate whether the value has further details to parse. This allows different sync state depending on update
     * or new.
     * @return true if details need to be synced.
     */
    public boolean syncDetails();
  }

  public static class SimpleContentValuesProvider implements ContentValuesProvider {

    private final ContentValues mContentValues;
    private final boolean mSyncDetails;

    public SimpleContentValuesProvider(ContentValues contentValues, boolean syncDetails) {
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

    public CVPair(long id, ContentValuesProvider cV) {
      mCV = cV;
      mId = id;
    }

    @Override
    public int compareTo(CVPair another) {
      long rhs = another.mId;
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

  public RemoteXmlSyncAdapter(Context context, boolean autoInitialize, Uri listContentUri) {
    super(context, autoInitialize, null);
    init(listContentUri);
  }

  private void init(Uri listContentUri) {
    listContentUri.buildUpon().encodedFragment("nonetnotify").build();
    mCoordinator = new RemoteXmlSyncAdapterDelegate(listContentUri, this);
    setDelegates(Arrays.asList(mCoordinator));
  }

  public RemoteXmlSyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs, Uri listContentUri) {
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