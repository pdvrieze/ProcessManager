package nl.adaptivity.process.models;

import java.io.BufferedInputStream;
import java.io.CharArrayWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParserException;

import nl.adaptivity.android.util.ContentProviderHelper;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.diagram.DrawableProcessNode;
import nl.adaptivity.process.diagram.LayoutAlgorithm;
import nl.adaptivity.process.editor.android.PMParser;
import nl.adaptivity.process.processModel.ProcessModel;
import nl.adaptivity.sync.RemoteXmlSyncAdapter;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.XmlBaseColumns;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.provider.BaseColumns;


public class ProcessModelProvider extends ContentProvider {

  public static final String AUTHORITY = "nl.adaptivity.process.models";

  public static class ProcessModels implements XmlBaseColumns {

    private ProcessModels(){}

    public static final String COLUMN_HANDLE="handle";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_MODEL = "model";
    public static final String COLUMN_UUID = "uuid";

    private static final String SCHEME = "content://";

    private static final String PATH_MODELS = "/processmodels";
    private static final String PATH_MODEL_ID = PATH_MODELS+'/';
    private static final String PATH_MODEL_STREAM = "/streams/";

    public static final Uri CONTENT_URI = Uri.parse(SCHEME+AUTHORITY+PATH_MODELS);

    public static final Uri CONTENT_ID_URI_BASE = Uri.parse(SCHEME+AUTHORITY+PATH_MODEL_ID);
    public static final Uri CONTENT_ID_URI_PATTERN = Uri.parse(SCHEME+AUTHORITY+PATH_MODEL_ID+'#');

    public static final Uri CONTENT_ID_STREAM_BASE = Uri.parse(SCHEME+AUTHORITY+PATH_MODEL_STREAM);
    public static final Uri CONTENT_ID_STREAM_PATTERN = Uri.parse(SCHEME+AUTHORITY+PATH_MODEL_STREAM+'#');
    public static final String[] BASE_PROJECTION = new String[] { BaseColumns._ID, COLUMN_HANDLE, COLUMN_NAME };
    public static final String SELECT_HANDLE = COLUMN_HANDLE+" = ?";

  }

  public static class ProcessInstances implements XmlBaseColumns {
    private ProcessInstances(){}

    public static final String COLUMN_HANDLE="handle";
    public static final String COLUMN_PMHANDLE="pmhandle";
    public static final String COLUMN_NAME="name";


    private static final String SCHEME = "content://";

    private static final String PATH_INSTANCES = "/processinstances";
    private static final String PATH_INSTANCE_ID = PATH_INSTANCES+'/';
    private static final String PATH_MODEL_STREAM = "/streams/";

    public static final Uri CONTENT_URI = Uri.parse(SCHEME+AUTHORITY+PATH_INSTANCES);

    public static final Uri CONTENT_ID_URI_BASE = Uri.parse(SCHEME+AUTHORITY+PATH_INSTANCE_ID);
    public static final Uri CONTENT_ID_URI_PATTERN = Uri.parse(SCHEME+AUTHORITY+PATH_INSTANCE_ID+'#');

    public static final String[] BASE_PROJECTION = new String[] { BaseColumns._ID, COLUMN_HANDLE, COLUMN_PMHANDLE, COLUMN_NAME };
    public static final String SELECT_HANDLE = COLUMN_HANDLE+" = ?";
  }

  private static enum QueryTarget{
    PROCESSMODELS(ProcessModels.CONTENT_URI),
    PROCESSMODEL(ProcessModels.CONTENT_ID_URI_PATTERN),
    PROCESSMODELCONTENT(ProcessModels.CONTENT_ID_STREAM_PATTERN),
    PROCESSINSTANCES(ProcessInstances.CONTENT_URI),
    PROCESSINSTANCE(ProcessInstances.CONTENT_ID_URI_PATTERN),
    ;

    private String path;
    private Uri baseUri;

    private QueryTarget(Uri uri) {
      baseUri = Uri.fromParts(uri.getScheme(), uri.getAuthority()+'/'+uri.getPath(), null);
      String frag = uri.getFragment();
      if (frag != null) {
        path = uri.getPath() + '#' + frag;
      } else {
        path = uri.getPath();
      }
    }
  }

  private static class UriHelper {

    private static UriMatcher _uriMatcher;

    static {
      _uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
      for(QueryTarget u:QueryTarget.values()) {
        String path = u.path;
        if (path.startsWith("/")) { path = path.substring(1); }
        _uriMatcher.addURI(AUTHORITY, path, u.ordinal() );
      }
    }

    final QueryTarget mTarget;
    final long mId;
    final String mTable;
    boolean mNetNotify;

    private UriHelper(QueryTarget u, boolean netNotify) {
      this(u, -1, netNotify);
    }

    private UriHelper(QueryTarget u, long id, boolean netNotify) {
      mTarget = u;
      mId = id;
      mNetNotify = netNotify;
      mTable = u==QueryTarget.PROCESSINSTANCE|| u==QueryTarget.PROCESSINSTANCES ? ProcessModelsOpenHelper.TABLE_INSTANCES_NAME: ProcessModelsOpenHelper.TABLE_NAME;
    }

    static UriHelper parseUri(Uri query) {
      boolean netNotify;
      if ("nonetnotify".equals(query.getFragment())) {
        query = Uri.fromParts(query.getScheme(), query.getEncodedSchemeSpecificPart(), null);
        netNotify=false;
      } else {
        netNotify=true;
      }
      int ord = _uriMatcher.match(query);
      if (ord<0) { throw new IllegalArgumentException("Unknown URI: "+query); }
      QueryTarget u = QueryTarget.values()[ord];

      switch (u) {
      case PROCESSMODEL:
        return new UriHelper(u, ContentUris.parseId(query), netNotify);
      case PROCESSMODELCONTENT:
        return new UriHelper(u, ContentUris.parseId(query), netNotify);
      case PROCESSINSTANCE:
        return new UriHelper(u, ContentUris.parseId(query), netNotify);
      default:
        return new UriHelper(u, netNotify);
      }
    }
  }

  private static String[] appendArg(String[] pArgs, String pArg) {
    if (pArgs==null || pArgs.length==0) { return new String[] { pArg }; }
    final String[] result = new String[pArgs.length+1];
    System.arraycopy(pArgs, 0, result, 0, pArgs.length);
    result[pArgs.length] = pArg;
    return result;
  }

  private static boolean mimetypeMatches(String pMimetype, String pMimeTypeFilter) {
    if (pMimeTypeFilter==null) { return true; }
    int splitIndex = pMimetype.indexOf('/');
    String typeLeft = pMimetype.substring(0, splitIndex);
    String typeRight = pMimetype.substring(splitIndex+1);

    splitIndex = pMimeTypeFilter.indexOf('/');
    String filterLeft = pMimeTypeFilter.substring(0, splitIndex);
    String filterRight = pMimeTypeFilter.substring(splitIndex+1);

    if (! (filterLeft.equals(typeLeft)||"*".equals(filterLeft))) {
      return false;
    }

    if (! (filterRight.equals(typeRight)||"*".equals(filterRight))) {
      return false;
    }

    return true;
  }

  private ProcessModelsOpenHelper mDbHelper;

  @Override
  public boolean onCreate() {
    mDbHelper = new ProcessModelsOpenHelper(getContext());
    return true;
  }

  @Override
  public void shutdown() {
    super.shutdown();
    mDbHelper.close();
  }

  @Override
  public String getType(Uri pUri) {
    UriHelper helper = UriHelper.parseUri(pUri);
    switch (helper.mTarget) {
    case PROCESSMODEL:
      return "vnd.android.cursor.item/vnd.nl.adaptivity.process.processmodel";
    case PROCESSMODELS:
      return "vnd.android.cursor.dir/vnd.nl.adaptivity.process.processmodel";
    case PROCESSINSTANCE:
      return "vnd.android.cursor.item/vnd.nl.adaptivity.process.processinstance";
    case PROCESSINSTANCES:
      return "vnd.android.cursor.dir/vnd.nl.adaptivity.process.processinstance";
    case PROCESSMODELCONTENT:
      return "application/vnd.nl.adaptivity.process.processmodel";
    }
    return null;
  }

  @Override
  public String[] getStreamTypes(Uri pUri, String pMimeTypeFilter) {
    UriHelper helper = UriHelper.parseUri(pUri);
    String mimetype;
    switch (helper.mTarget) {
      case PROCESSMODEL:
        mimetype = "vnd.android.cursor.item/vnd.nl.adaptivity.process.processmodel";break;
      case PROCESSMODELS:
        mimetype = "vnd.android.cursor.dir/vnd.nl.adaptivity.process.processmodel";break;
      case PROCESSINSTANCE:
        mimetype = "vnd.android.cursor.item/vnd.nl.adaptivity.process.processinstance";break;
      case PROCESSINSTANCES:
        mimetype = "vnd.android.cursor.dir/vnd.nl.adaptivity.process.processinstance";break;
      case PROCESSMODELCONTENT:
        mimetype = "application/vnd.nl.adaptivity.process.processmodel";break;
      default:
        return null;
    }
    if (mimetypeMatches(mimetype, pMimeTypeFilter)) {
      return new String[] { mimetype };
    } else {
      return null;
    }
  }

  @Override
  public ParcelFileDescriptor openFile(Uri pUri, String pMode) throws FileNotFoundException {
    UriHelper helper = UriHelper.parseUri(pUri);
    if (helper.mTarget!=QueryTarget.PROCESSMODELCONTENT || helper.mId<0) {
      throw new FileNotFoundException();
    }
    return ContentProviderHelper.createPipe(this, mDbHelper, ProcessModelsOpenHelper.TABLE_NAME, ProcessModels.COLUMN_MODEL, helper.mId, ProcessModels.COLUMN_SYNCSTATE, pMode);
  }

  @Override
  public Cursor query(Uri pUri, String[] pProjection, String pSelection, String[] pSelectionArgs, String pSortOrder) {
    UriHelper helper = UriHelper.parseUri(pUri);
    if (helper.mId>=0) {
      if (pSelection==null || pSelection.length()==0) {
        pSelection = BaseColumns._ID+" = ?";
      } else {
        pSelection = "( "+pSelection+" ) AND ( "+BaseColumns._ID+" = ? )";
      }
      pSelectionArgs = appendArg(pSelectionArgs, Long.toString(helper.mId));
    }

    SQLiteDatabase db = mDbHelper.getReadableDatabase();
    final Cursor result = db.query(helper.mTable, pProjection, pSelection, pSelectionArgs, null, null, pSortOrder);
    result.setNotificationUri(getContext().getContentResolver(), pUri);
    return result;
  }

  @Override
  public Uri insert(Uri pUri, ContentValues pValues) {
    if (! pValues.containsKey(XmlBaseColumns.COLUMN_SYNCSTATE)) {
      pValues.put(XmlBaseColumns.COLUMN_SYNCSTATE, Long.valueOf(RemoteXmlSyncAdapter.SYNC_PUBLISH_TO_SERVER));
    }
    UriHelper helper = UriHelper.parseUri(pUri);
    if (helper.mId>=0) {
      pValues.put(BaseColumns._ID, Long.valueOf(helper.mId));
    }
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    long id = db.insert(helper.mTable, ProcessModels.COLUMN_NAME, pValues);
    return notify(helper, id);
  }

  private Uri notify(UriHelper pHelper, long pId) {
    ContentResolver cr = getContext().getContentResolver();
    Uri baseUri = pHelper.mTarget.baseUri;
    cr.notifyChange(baseUri, null, false);
    if (pId>=0) {
      Uri result = ContentUris.withAppendedId(baseUri, pId);
      cr.notifyChange(result, null, pHelper.mNetNotify);
      return result;
    }
    return baseUri;
  }

  @Override
  public int delete(Uri pUri, String pSelection, String[] pSelectionArgs) {
    UriHelper helper = UriHelper.parseUri(pUri);
    if (helper.mId>=0) {
      if (pSelection==null || pSelection.length()==0) {
        pSelection = BaseColumns._ID+" = ?";
      } else {
        pSelection = "( "+pSelection+" ) AND ( "+BaseColumns._ID+" = ? )";
      }
      pSelectionArgs = appendArg(pSelectionArgs, Long.toString(helper.mId));
    }
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    final int result = db.delete(helper.mTable, pSelection, pSelectionArgs);
    if (result>0) {
      notify(helper, helper.mId);
    }
    return result;
  }

  @Override
  public int update(Uri pUri, ContentValues pValues, String pSelection, String[] pSelectionArgs) {
    UriHelper helper = UriHelper.parseUri(pUri);
    if (helper.mId>=0) {
      if (pSelection==null || pSelection.length()==0) {
        pSelection = BaseColumns._ID+" = ?";
      } else {
        pSelection = "( "+pSelection+" ) AND ( "+BaseColumns._ID+" = ? )";
      }
      pSelectionArgs = appendArg(pSelectionArgs, Long.toString(helper.mId));
    }
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    final int result = db.update(helper.mTable, pValues, pSelection, pSelectionArgs);
    if (result>0) {
      getContext().getContentResolver().notifyChange(pUri, null, helper.mNetNotify);
    }
    return result;
  }

  /**
   * This implementation of applyBatch wraps the operations into a database transaction that fails on exceptions.
   */
  @Override
  public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> pOperations) throws OperationApplicationException {
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    db.beginTransaction();
    try {
      ContentProviderResult[] result = super.applyBatch(pOperations);
      db.setTransactionSuccessful();
      return result;
    } finally {
      db.endTransaction();
    }
  }

  public static ProcessModel<?> getProcessModelForHandle(Context pContext, long pHandle) {
    try {
      final ContentResolver contentResolver = pContext.getContentResolver();
      Cursor idresult = contentResolver.query(ProcessModels.CONTENT_URI, new String[] { BaseColumns._ID }, ProcessModels.COLUMN_HANDLE+" = ?", new String[] { Long.toString(pHandle)} , null);
      try {
        if (! idresult.moveToFirst()) { return null; }
        long id = idresult.getLong(1); // only one column
        Uri uri = ContentUris.withAppendedId(ProcessModels.CONTENT_ID_STREAM_BASE, id);
        InputStream in = contentResolver.openInputStream(uri);
        try{
          return getProcessModel(in);
        } finally {
          in.close();
        }
      } finally {
        idresult.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static ProcessModel<?> getProcessModelForId(Context pContext, long pId) {
    Uri uri = ContentUris.withAppendedId(ProcessModels.CONTENT_ID_STREAM_BASE, pId);
    return getProcessModel(pContext, uri);
  }

  public static ProcessModel<?> getProcessModel(Context pContext, Uri pUri) {
    try {
      InputStream in;
      if (ContentResolver.SCHEME_CONTENT.equals(pUri.getScheme())||
          ContentResolver.SCHEME_ANDROID_RESOURCE.equals(pUri.getScheme())||
          ContentResolver.SCHEME_FILE.equals(pUri.getScheme())) {
        final ContentResolver contentResolver = pContext.getContentResolver();
        in = contentResolver.openInputStream(pUri);
      } else {
        in = URI.create(pUri.toString()).toURL().openStream();
      }
      try {
        return getProcessModel(new BufferedInputStream(in));
      } finally {
        in.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static ProcessModel<?> getProcessModel(InputStream in) {
    final LayoutAlgorithm<DrawableProcessNode> layoutAlgorithm = new LayoutAlgorithm<>();
    return PMParser.parseProcessModel(in, layoutAlgorithm, layoutAlgorithm);
  }

  public static Uri newProcessModel(Context context, ClientProcessModel<?> pProcessModel) throws IOException {
    CharArrayWriter out = new CharArrayWriter();
    try {
      PMParser.serializeProcessModel(out, pProcessModel);
    } catch (XmlPullParserException e) {
      throw new IOException(e);
    }
    ContentProviderClient client = context.getContentResolver().acquireContentProviderClient(ProcessModels.CONTENT_ID_URI_BASE);
    try {
      ContentValues values = new ContentValues();
      values.put(ProcessModels.COLUMN_NAME, pProcessModel.getName());
      values.put(ProcessModels.COLUMN_MODEL, out.toString());
      values.put(ProcessModels.COLUMN_UUID, pProcessModel.getUuid().toString());
      try {
        return client.insert(ProcessModels.CONTENT_ID_URI_BASE, values);
      } catch (RemoteException e) {
        throw new IOException(e);
      }
    } finally {
      client.release();
    }
  }

  public static Uri instantiate(Context context, long pModelHandle, String pName) throws RemoteException {

    final ContentResolver contentResolver = context.getContentResolver();
    ContentProviderClient client = contentResolver.acquireContentProviderClient(ProcessInstances.CONTENT_ID_URI_BASE);
    try {
      ContentValues cv = new ContentValues(3);
      cv.put(ProcessInstances.COLUMN_PMHANDLE, Long.valueOf(pModelHandle));
      cv.put(ProcessInstances.COLUMN_NAME, pName);
      cv.put(XmlBaseColumns.COLUMN_SYNCSTATE, Integer.valueOf(RemoteXmlSyncAdapter.SYNC_PUBLISH_TO_SERVER));
      try {
        return client.insert(ProcessInstances.CONTENT_ID_URI_BASE, cv);
      } catch (RemoteException e) {
        throw e;
//        throw new IOException(e);
      }
    } finally {
      client.release();
    }

  }

}
