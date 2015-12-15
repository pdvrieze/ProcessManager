package nl.adaptivity.process.models;

import android.accounts.Account;
import android.app.Activity;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.provider.BaseColumns;
import nl.adaptivity.android.util.ContentProviderHelper;
import nl.adaptivity.process.android.ProviderHelper;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.diagram.DrawableProcessNode;
import nl.adaptivity.process.diagram.LayoutAlgorithm;
import nl.adaptivity.process.editor.android.PMParser;
import nl.adaptivity.process.processModel.ProcessModel;
import nl.adaptivity.sync.RemoteXmlSyncAdapter;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.XmlBaseColumns;
import nl.adaptivity.xml.XmlException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.UUID;


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


    public static final String COLUMN_UUID = "uuid";
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
        query = query.buildUpon().encodedFragment(null).build();
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

  public static void requestSyncProcessModelList(Account account, boolean expedited) {
    ProviderHelper.requestSync(account, AUTHORITY, expedited);
  }

  public static void requestSyncProcessModelList(Activity context, boolean expedited) {
    ProviderHelper.requestSync(context, AUTHORITY, expedited);
  }

  private static String[] appendArg(String[] args, String arg) {
    if (args==null || args.length==0) { return new String[] { arg }; }
    final String[] result = new String[args.length+1];
    System.arraycopy(args, 0, result, 0, args.length);
    result[args.length] = arg;
    return result;
  }

  private static boolean mimetypeMatches(String mimetype, String mimeTypeFilter) {
    if (mimeTypeFilter==null) { return true; }
    int splitIndex = mimetype.indexOf('/');
    String typeLeft = mimetype.substring(0, splitIndex);
    String typeRight = mimetype.substring(splitIndex+1);

    splitIndex = mimeTypeFilter.indexOf('/');
    String filterLeft = mimeTypeFilter.substring(0, splitIndex);
    String filterRight = mimeTypeFilter.substring(splitIndex+1);

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
  public String getType(Uri uri) {
    UriHelper helper = UriHelper.parseUri(uri);
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
  public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
    UriHelper helper = UriHelper.parseUri(uri);
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
    if (mimetypeMatches(mimetype, mimeTypeFilter)) {
      return new String[] { mimetype };
    } else {
      return null;
    }
  }

  @Override
  public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
    UriHelper helper = UriHelper.parseUri(uri);
    if (helper.mTarget!=QueryTarget.PROCESSMODELCONTENT || helper.mId<0) {
      throw new FileNotFoundException();
    }
    return ContentProviderHelper.createPipe(this, mDbHelper, ProcessModelsOpenHelper.TABLE_NAME, ProcessModels.COLUMN_MODEL, helper.mId, ProcessModels.COLUMN_SYNCSTATE, mode);
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    UriHelper helper = UriHelper.parseUri(uri);
    if (helper.mId>=0) {
      if (selection==null || selection.length()==0) {
        selection = BaseColumns._ID+" = ?";
      } else {
        selection = "( "+selection+" ) AND ( "+BaseColumns._ID+" = ? )";
      }
      selectionArgs = appendArg(selectionArgs, Long.toString(helper.mId));
    }

    SQLiteDatabase db = mDbHelper.getReadableDatabase();
    final Cursor result = db.query(helper.mTable, projection, selection, selectionArgs, null, null, sortOrder);
    result.setNotificationUri(getContext().getContentResolver(), uri);
    return result;
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    if (! values.containsKey(XmlBaseColumns.COLUMN_SYNCSTATE)) {
      values.put(XmlBaseColumns.COLUMN_SYNCSTATE, Long.valueOf(RemoteXmlSyncAdapter.SYNC_PUBLISH_TO_SERVER));
    }
    UriHelper helper = UriHelper.parseUri(uri);
    if (helper.mId>=0) {
      values.put(BaseColumns._ID, Long.valueOf(helper.mId));
    }
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    long id = db.insert(helper.mTable, ProcessModels.COLUMN_NAME, values);
    return notify(helper, id);
  }

  private Uri notify(UriHelper helper, long id) {
    ContentResolver cr = getContext().getContentResolver();
    Uri baseUri = helper.mTarget.baseUri;
    cr.notifyChange(baseUri, null, false);
    if (id>=0) {
      Uri result = ContentUris.withAppendedId(baseUri, id);
      cr.notifyChange(result, null, helper.mNetNotify);
      return result;
    }
    return baseUri;
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    UriHelper helper = UriHelper.parseUri(uri);
    if (helper.mId>=0) {
      if (selection==null || selection.length()==0) {
        selection = BaseColumns._ID+" = ?";
      } else {
        selection = "( "+selection+" ) AND ( "+BaseColumns._ID+" = ? )";
      }
      selectionArgs = appendArg(selectionArgs, Long.toString(helper.mId));
    }
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    final int result = db.delete(helper.mTable, selection, selectionArgs);
    if (result>0) {
      notify(helper, helper.mId);
    }
    return result;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    UriHelper helper = UriHelper.parseUri(uri);
    if (helper.mId>=0) {
      if (selection==null || selection.length()==0) {
        selection = BaseColumns._ID+" = ?";
      } else {
        selection = "( "+selection+" ) AND ( "+BaseColumns._ID+" = ? )";
      }
      selectionArgs = appendArg(selectionArgs, Long.toString(helper.mId));
    }
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    final int result = db.update(helper.mTable, values, selection, selectionArgs);
    if (result>0) {
      getContext().getContentResolver().notifyChange(uri, null, helper.mNetNotify);
    }
    return result;
  }

  /**
   * This implementation of applyBatch wraps the operations into a database transaction that fails on exceptions.
   */
  @Override
  public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    db.beginTransaction();
    try {
      ContentProviderResult[] result = super.applyBatch(operations);
      db.setTransactionSuccessful();
      return result;
    } finally {
      db.endTransaction();
    }
  }

  public static ProcessModel<?, ?> getProcessModelForHandle(Context context, long handle) {
    try {
      final ContentResolver contentResolver = context.getContentResolver();
      Cursor idresult = contentResolver.query(ProcessModels.CONTENT_URI, new String[] { BaseColumns._ID }, ProcessModels.COLUMN_HANDLE+" = ?", new String[] { Long.toString(handle)} , null);
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

  public static ProcessModel<?, ?> getProcessModelForId(Context context, long id) {
    Uri uri = ContentUris.withAppendedId(ProcessModels.CONTENT_ID_STREAM_BASE, id);
    return getProcessModel(context, uri);
  }

  public static ProcessModel<?, ?> getProcessModel(Context context, Uri uri) {
    try {
      InputStream in;
      if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())||
          ContentResolver.SCHEME_ANDROID_RESOURCE.equals(uri.getScheme())||
          ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
        final ContentResolver contentResolver = context.getContentResolver();
        in = contentResolver.openInputStream(uri);
      } else {
        in = URI.create(uri.toString()).toURL().openStream();
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

  private static ProcessModel<?, ?> getProcessModel(InputStream in) {
    final LayoutAlgorithm<DrawableProcessNode> layoutAlgorithm = new LayoutAlgorithm<>();
    return PMParser.parseProcessModel(in, layoutAlgorithm, layoutAlgorithm);
  }

  public static Uri newProcessModel(Context context, ClientProcessModel<?, ?> processModel) throws IOException {
    CharArrayWriter out = new CharArrayWriter();
    try {
      PMParser.serializeProcessModel(out, processModel);
    } catch (XmlPullParserException |XmlException e) {
      throw new IOException(e);
    }
    ContentProviderClient client = context.getContentResolver().acquireContentProviderClient(ProcessModels.CONTENT_ID_URI_BASE);
    try {
      ContentValues values = new ContentValues();
      values.put(ProcessModels.COLUMN_NAME, processModel.getName());
      values.put(ProcessModels.COLUMN_MODEL, out.toString());
      values.put(ProcessModels.COLUMN_UUID, processModel.getUuid().toString());
      try {
        return client.insert(ProcessModels.CONTENT_ID_URI_BASE, values);
      } catch (RemoteException e) {
        throw new IOException(e);
      }
    } finally {
      client.release();
    }
  }

  public static Uri instantiate(Context context, long modelId, String name) throws RemoteException {

    final ContentResolver contentResolver = context.getContentResolver();
    ContentProviderClient client = contentResolver.acquireContentProviderClient(ProcessInstances.CONTENT_ID_URI_BASE);
    try {
      final Uri modelUri = ContentUris.withAppendedId(ProcessModels.CONTENT_ID_STREAM_BASE, modelId);
      Cursor r = client.query(modelUri, new String[]{ProcessModels.COLUMN_HANDLE}, null, null, null);
      try {
        if (!r.moveToFirst()) { throw new RuntimeException("Model with id "+modelId+" not found"); }
        long pmhandle = r.getLong(0);
        ArrayList<ContentProviderOperation> batch = new ArrayList<>(2);
        batch.add(ContentProviderOperation
                .newAssertQuery(modelUri)
                .withValue(ProcessModels.COLUMN_HANDLE, pmhandle)
                .withExpectedCount(1)
                .build());

        batch.add(ContentProviderOperation
                .newInsert(ProcessInstances.CONTENT_ID_URI_BASE)
                .withValue(ProcessInstances.COLUMN_NAME, name)
                .withValue(ProcessInstances.COLUMN_PMHANDLE, Long.valueOf(pmhandle))
                .withValue(ProcessInstances.COLUMN_UUID, UUID.randomUUID().toString())
                .withValue(XmlBaseColumns.COLUMN_SYNCSTATE, Integer.valueOf(RemoteXmlSyncAdapter.SYNC_PUBLISH_TO_SERVER))
                .build());
        try {
          ContentProviderResult[] result = client.applyBatch(batch);
          return result[1].uri;
        } catch (OperationApplicationException e) {
          throw (RemoteException) new RemoteException().initCause(e);
        }
      } finally {
        r.close();
      }
    } finally {
      client.release();
    }

  }

}
