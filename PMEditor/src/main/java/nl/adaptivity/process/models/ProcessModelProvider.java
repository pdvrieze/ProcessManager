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

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.provider.BaseColumns;
import net.devrieze.util.Tupple;
import nl.adaptivity.android.util.ContentProviderHelper;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.data.DataOpenHelper;
import nl.adaptivity.process.data.ProviderHelper;
import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.diagram.DrawableProcessNode;
import nl.adaptivity.process.diagram.LayoutAlgorithm;
import nl.adaptivity.process.editor.android.PMParser;
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

    public static final String COLUMN_INSTANCECOUNT = "instancecount";

    private ProcessModels(){}

    public static final String COLUMN_HANDLE="handle";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_MODEL = "model";
    public static final String COLUMN_UUID = "uuid";
    public static final String COLUMN_FAVOURITE = "favourite";

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

    /**
     * Check that the values contain columns that need (client) notification of updates. Either server or client
     * @param values The values
     * @return true if notification is needed.
     */
    private static boolean hasNotifyableColumns(final ContentValues values) {
      for (final String key: values.keySet()) {
        switch (key) {
          case COLUMN_SYNCSTATE:
          case COLUMN_UUID:
          case COLUMN_HANDLE:
            break; // These are not user visible
          default:
            return true;
        }
      }
      return false;
    }

    private static boolean hasServerNotifyableColumns(final ContentValues values) {
      for (final String key: values.keySet()) {
        switch (key) {
          case COLUMN_SYNCSTATE:
          case COLUMN_FAVOURITE:
            break; // These are not user visible
          default:
            return true;
        }
      }
      return false;
    }
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

    public static boolean hasNotifyableColumns(final ContentValues values) {
      for (final String key: values.keySet()) {
        switch (key) {
          case COLUMN_SYNCSTATE:
          case COLUMN_UUID:
          case COLUMN_HANDLE:
            break; // These are not user visible
          default:
            return true;
        }
      }
      return false;
    }
  }

  private enum QueryTarget{
    PROCESSMODELS(ProcessModels.CONTENT_URI),
    PROCESSMODEL(ProcessModels.CONTENT_ID_URI_PATTERN),
    PROCESSMODELCONTENT(ProcessModels.CONTENT_ID_STREAM_PATTERN),
    PROCESSINSTANCES(ProcessInstances.CONTENT_URI),
    PROCESSINSTANCE(ProcessInstances.CONTENT_ID_URI_PATTERN),
    PROCESSMODELSEXT,
    PROCESSMODELEXT;

    private final String path;
    private final Uri baseUri;

    QueryTarget() {
      baseUri=null;
      path=null;
    }

    QueryTarget(final Uri uri) {
      baseUri = Uri.fromParts(uri.getScheme(), uri.getAuthority()+'/'+uri.getPath(), null);
      final String frag = uri.getFragment();
      if (frag != null) {
        path = uri.getPath() + '#' + frag;
      } else {
        path = uri.getPath();
      }
    }
  }

  private static class UriHelper {

    private static final UriMatcher _uriMatcher;

    static {
      _uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
      for(final QueryTarget u:QueryTarget.values()) {
        if (u.path!=null) {
          String path = u.path;
          if (path.startsWith("/")) { path = path.substring(1); }
          _uriMatcher.addURI(AUTHORITY, path, u.ordinal());
        }
      }
    }

    final QueryTarget mTarget;
    final long        mId;
    final String      mTable;
    final boolean     mNetNotify;

    private UriHelper(final QueryTarget u, final boolean netNotify) {
      this(u, -1, netNotify);
    }

    private UriHelper(final QueryTarget u, final long id, final boolean netNotify) {
      mTarget = u;
      mId = id;
      mNetNotify = netNotify;
      switch (u) {
        case PROCESSINSTANCE:
        case PROCESSINSTANCES:
          mTable = DataOpenHelper.TABLE_NAME_INSTANCES;
          break;
        case PROCESSMODEL:
        case PROCESSMODELCONTENT:
        case PROCESSMODELS:
          mTable = DataOpenHelper.TABLE_NAME_MODELS;
          break;
        case PROCESSMODELEXT:
        case PROCESSMODELSEXT:
          mTable = DataOpenHelper.VIEW_NAME_PROCESSMODELEXT;
          break;
        default:
          throw new IllegalArgumentException("Unsupported query target "+u);
      }

    }

    public boolean hasNotifyableColumns(final ContentValues values) {
      switch (mTarget) {
        case PROCESSMODEL:
        case PROCESSMODELS:
        case PROCESSMODELCONTENT:
          return ProcessModels.hasNotifyableColumns(values);
        case PROCESSINSTANCE:
        case PROCESSINSTANCES:
          return ProcessInstances.hasNotifyableColumns(values);
        default:
          throw new IllegalStateException("The given target is unknown");
      }
    }

    public boolean hasServerNotifyableColumns(final ContentValues values) {
      switch (mTarget) {
        case PROCESSMODEL:
        case PROCESSMODELS:
        case PROCESSMODELCONTENT:
          return ProcessModels.hasServerNotifyableColumns(values);
        default:
          return hasNotifyableColumns(values);
      }
    }

    public static UriHelper parseUri(final Uri uri) {
      return parseUri(uri, null);
    }

    static UriHelper parseUri(Uri query, final String[] projection) {
      final boolean netNotify;
      if ("nonetnotify".equals(query.getFragment())) {
        query = query.buildUpon().encodedFragment(null).build();
        netNotify=false;
      } else {
        netNotify=true;
      }
      final int ord = _uriMatcher.match(query);
      if (ord<0) { throw new IllegalArgumentException("Unknown URI: "+query); }
      final QueryTarget u = QueryTarget.values()[ord];

      switch (u) {
      case PROCESSMODEL:
        return new UriHelper(ProviderHelper.isColumnInProjection(ProcessModels.COLUMN_INSTANCECOUNT, projection) ? QueryTarget.PROCESSMODELEXT : u, ContentUris.parseId(query), netNotify);
      case PROCESSMODELS:
        return new UriHelper(ProviderHelper.isColumnInProjection(ProcessModels.COLUMN_INSTANCECOUNT, projection) ? QueryTarget.PROCESSMODELSEXT: u, netNotify);
      case PROCESSMODELCONTENT:
        return new UriHelper(u, ContentUris.parseId(query), netNotify);
      case PROCESSINSTANCE:
        return new UriHelper(u, ContentUris.parseId(query), netNotify);
      default:
        return new UriHelper(u, netNotify);
      }
    }
  }

  private static String[] appendArg(final String[] args, final String arg) {
    if (args==null || args.length==0) { return new String[] { arg }; }
    final String[] result = new String[args.length+1];
    System.arraycopy(args, 0, result, 0, args.length);
    result[args.length] = arg;
    return result;
  }

  private static boolean mimetypeMatches(final String mimetype, final String mimeTypeFilter) {
    if (mimeTypeFilter==null) { return true; }
    int splitIndex = mimetype.indexOf('/');
    final String typeLeft = mimetype.substring(0, splitIndex);
    final String typeRight = mimetype.substring(splitIndex + 1);

    splitIndex = mimeTypeFilter.indexOf('/');
    final String filterLeft = mimeTypeFilter.substring(0, splitIndex);
    final String filterRight = mimeTypeFilter.substring(splitIndex + 1);

    if (! (filterLeft.equals(typeLeft)||"*".equals(filterLeft))) {
      return false;
    }

    if (! (filterRight.equals(typeRight)||"*".equals(filterRight))) {
      return false;
    }

    return true;
  }

  private DataOpenHelper mDbHelper;

  @Override
  public boolean onCreate() {
    mDbHelper = new DataOpenHelper(getContext());
    return true;
  }

  @Override
  public void shutdown() {
    super.shutdown();
    mDbHelper.close();
  }

  @Override
  public String getType(final Uri uri) {
    final UriHelper helper = UriHelper.parseUri(uri);
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
  public String[] getStreamTypes(final Uri uri, final String mimeTypeFilter) {
    final UriHelper helper = UriHelper.parseUri(uri);
    final String mimetype;
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
  public ParcelFileDescriptor openFile(final Uri uri, final String mode) throws FileNotFoundException {
    final UriHelper helper = UriHelper.parseUri(uri);
    if (helper.mTarget!=QueryTarget.PROCESSMODELCONTENT || helper.mId<0) {
      throw new FileNotFoundException();
    }
    return ContentProviderHelper.createPipe(this, mDbHelper, DataOpenHelper.TABLE_NAME_MODELS, ProcessModels.COLUMN_MODEL, helper.mId, ProcessModels.COLUMN_SYNCSTATE, mode, helper.mNetNotify);
  }

  @Override
  public Cursor query(final Uri uri, final String[] projection, String selection, String[] selectionArgs, final String sortOrder) {
    final UriHelper helper = UriHelper.parseUri(uri, projection);
    if (helper.mId>=0) {
      if (selection==null || selection.length()==0) {
        selection = BaseColumns._ID+" = ?";
      } else {
        selection = "( "+selection+" ) AND ( "+BaseColumns._ID+" = ? )";
      }
      selectionArgs = appendArg(selectionArgs, Long.toString(helper.mId));
    }

    final SQLiteDatabase db = mDbHelper.getReadableDatabase();
    final Cursor result;
    result = db.query(helper.mTable, projection, selection, selectionArgs, null, null, sortOrder);
    result.setNotificationUri(getContext().getContentResolver(), uri);
    return result;
  }

  @Override
  public Uri insert(final Uri uri, final ContentValues values) {
    final UriHelper helper = UriHelper.parseUri(uri);
    addSyncstateIfNeeded(helper, values);

    if (helper.mId>=0) {
      values.put(BaseColumns._ID, Long.valueOf(helper.mId));
    }
    final SQLiteDatabase db = mDbHelper.getWritableDatabase();
    final long id = db.insert(helper.mTable, ProcessModels.COLUMN_NAME, values);
    notifyPMStreamChangeIfNeeded(id, helper, values);
    return notify(helper, id);
  }

  private void addSyncstateIfNeeded(final UriHelper helper, final ContentValues values) {
    final boolean needsSync = isSyncStateNeeded(helper, values);
    if (needsSync) {
      values.put(XmlBaseColumns.COLUMN_SYNCSTATE, Long.valueOf(RemoteXmlSyncAdapter.SYNC_PUBLISH_TO_SERVER));
    }

  }

  private boolean isSyncStateNeeded(final UriHelper helper, final ContentValues values) {
    boolean needsSync = false;
    if (! values.containsKey(XmlBaseColumns.COLUMN_SYNCSTATE)) {
      if (DataOpenHelper.TABLE_NAME_MODELS.equals(helper.mTable)) {
        needsSync = ProcessModels.hasServerNotifyableColumns(values);
      } else {
        needsSync = true;
      }

    }
    return needsSync;
  }

  public void notifyPMStreamChangeIfNeeded(final long id, final UriHelper helper, final ContentValues values) {
    if (DataOpenHelper.TABLE_NAME_MODELS.equals(helper.mTable) && values.containsKey(ProcessModels.COLUMN_MODEL)) {
      getContext().getContentResolver().notifyChange(ContentUris.withAppendedId(ProcessModels.CONTENT_ID_STREAM_BASE, id), null, helper.mNetNotify);
    }
  }

  private Uri notify(final UriHelper helper, final long id) {
    final ContentResolver cr = getContext().getContentResolver();
    final Uri baseUri = helper.mTarget.baseUri;
    cr.notifyChange(baseUri, null, false);
    if (id>=0) {
      final Uri result = ContentUris.withAppendedId(baseUri, id);
      cr.notifyChange(result, null, helper.mNetNotify);
      return result;
    }
    return baseUri;
  }

  @Override
  public int delete(final Uri uri, String selection, String[] selectionArgs) {
    final UriHelper helper = UriHelper.parseUri(uri);
    if (helper.mId>=0) {
      if (selection==null || selection.length()==0) {
        selection = BaseColumns._ID+" = ?";
      } else {
        selection = "( "+selection+" ) AND ( "+BaseColumns._ID+" = ? )";
      }
      selectionArgs = appendArg(selectionArgs, Long.toString(helper.mId));
    }
    final SQLiteDatabase db = mDbHelper.getWritableDatabase();
    final int result = db.delete(helper.mTable, selection, selectionArgs);
    if (result>0) {
      notify(helper, helper.mId);
    }
    return result;
  }

  @Override
  public int update(final Uri uri, final ContentValues values, String selection, String[] selectionArgs) {
    final UriHelper helper = UriHelper.parseUri(uri);
    addSyncstateIfNeeded(helper, values);
    if (helper.mId>=0) {
      if (selection==null || selection.length()==0) {
        selection = BaseColumns._ID+" = ?";
      } else {
        selection = "( "+selection+" ) AND ( "+BaseColumns._ID+" = ? )";
      }
      selectionArgs = appendArg(selectionArgs, Long.toString(helper.mId));
    }
    final SQLiteDatabase db = mDbHelper.getWritableDatabase();
    final int result = db.update(helper.mTable, values, selection, selectionArgs);
    if (result>0 && helper.hasNotifyableColumns(values)) {
      notifyPMStreamChangeIfNeeded(helper.mId, helper, values);
      getContext().getContentResolver().notifyChange(uri, null, helper.mNetNotify && helper.hasServerNotifyableColumns(values));
    }
    return result;
  }

  /**
   * This implementation of applyBatch wraps the operations into a database transaction that fails on exceptions.
   */
  @Override
  public ContentProviderResult[] applyBatch(final ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
    final SQLiteDatabase db = mDbHelper.getWritableDatabase();
    db.beginTransaction();
    try {
      final ContentProviderResult[] result = super.applyBatch(operations);
      db.setTransactionSuccessful();
      return result;
    } finally {
      db.endTransaction();
    }
  }

  public static long getIdForHandle(final Context context, final long handle) {
    final ContentResolver contentResolver = context.getContentResolver();
    final Cursor idresult = contentResolver.query(ProcessModels.CONTENT_URI, new String[] { BaseColumns._ID }, ProcessModels.COLUMN_HANDLE + " = ?", new String[] { Long.toString(handle)} , null);
    if (idresult==null) { return 0; }
    try {
      if (! idresult.moveToFirst()) { return 0; }
      return idresult.getLong(0);
    } finally {
      idresult.close();
    }
  }

  public static Tupple<DrawableProcessModel, Long> getProcessModelForHandle(final Context context, final long handle) {
    try {
      final ContentResolver contentResolver = context.getContentResolver();
      final Cursor idresult = contentResolver.query(ProcessModels.CONTENT_URI, new String[] { BaseColumns._ID, ProcessModels.COLUMN_FAVOURITE }, ProcessModels.COLUMN_HANDLE + " = ?", new String[] { Long.toString(handle)} , null);
      if (idresult==null) { return null; }
      try {
        if (! idresult.moveToFirst()) { return null; }
        final long id = idresult.getLong(0); // first column
        final boolean favourite = idresult.getInt(1) != 0;
        final Uri uri = ContentUris.withAppendedId(ProcessModels.CONTENT_ID_STREAM_BASE, id);
        final InputStream in = contentResolver.openInputStream(uri);
        if (in==null) { throw new NullPointerException(); }
        try{
          return Tupple.tupple(getProcessModel(in, favourite), id);
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

  public static DrawableProcessModel getProcessModelForId(final Context context, final long id) {
    final Uri uri = ContentUris.withAppendedId(ProcessModels.CONTENT_ID_STREAM_BASE, id);
    return getProcessModel(context, uri);
  }

  public static DrawableProcessModel getProcessModel(final Context context, final Uri uri) {
    final ContentResolver contentResolver = context.getContentResolver();
    final Cursor cursor = contentResolver.query(uri, new String[] {ProcessModels.COLUMN_FAVOURITE}, null, null, null);
    try {
      if (cursor.moveToFirst()) {
        final boolean favourite = cursor.getInt(cursor.getColumnIndex(ProcessModels.COLUMN_FAVOURITE)) != 0;
        return getProcessModel(context, uri, favourite);
      } else { // no such model
        return null;
      }
    } finally {
      cursor.close();
    }
  }

  public static DrawableProcessModel getProcessModel(final Context context, final Uri uri, final boolean favourite) {
    try {
      final InputStream in;
      if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())||
          ContentResolver.SCHEME_ANDROID_RESOURCE.equals(uri.getScheme())||
          ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
        final ContentResolver contentResolver = context.getContentResolver();
        in = contentResolver.openInputStream(uri);
      } else {
        in = URI.create(uri.toString()).toURL().openStream();
      }
      try {
        return getProcessModel(new BufferedInputStream(in), favourite);
      } finally {
        in.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static DrawableProcessModel getProcessModel(final InputStream in, final boolean favourite) {
    final LayoutAlgorithm<DrawableProcessNode> layoutAlgorithm = new LayoutAlgorithm<>();
    final DrawableProcessModel drawableProcessModel = PMParser.parseProcessModel(in, layoutAlgorithm, layoutAlgorithm);
    drawableProcessModel.setFavourite(favourite);
    return drawableProcessModel;
  }

  public static Uri newProcessModel(final Context context, final ClientProcessModel<?, ?> processModel) throws IOException {
    final CharArrayWriter out = new CharArrayWriter();
    try {
      PMParser.serializeProcessModel(out, processModel);
    } catch (XmlPullParserException |XmlException e) {
      throw new IOException(e);
    }
    final ContentProviderClient client = context.getContentResolver().acquireContentProviderClient(ProcessModels.CONTENT_ID_URI_BASE);
    try {
      final ContentValues values = new ContentValues();
      values.put(ProcessModels.COLUMN_NAME, processModel.getName());
      values.put(ProcessModels.COLUMN_MODEL, out.toString());
      values.put(ProcessModels.COLUMN_UUID, processModel.getUuid().toString());
      if (processModel instanceof DrawableProcessModel) {
        values.put(ProcessModels.COLUMN_FAVOURITE, ((DrawableProcessModel) processModel).isFavourite());
      }
      try {
        return client.insert(ProcessModels.CONTENT_ID_URI_BASE, values);
      } catch (RemoteException e) {
        throw new IOException(e);
      }
    } finally {
      client.release();
    }
  }

  public static Uri instantiate(final Context context, final long modelId, final String name) throws RemoteException {

    final ContentResolver contentResolver = context.getContentResolver();
    final ContentProviderClient client = contentResolver.acquireContentProviderClient(ProcessInstances.CONTENT_ID_URI_BASE);
    try {
      final Uri modelUri = ContentUris.withAppendedId(ProcessModels.CONTENT_ID_STREAM_BASE, modelId);
      final Cursor r = client.query(modelUri, new String[]{ProcessModels.COLUMN_HANDLE}, null, null, null);
      try {
        if (!r.moveToFirst()) { throw new RuntimeException("Model with id "+modelId+" not found"); }
        final long pmhandle = r.getLong(0);
        final ArrayList<ContentProviderOperation> batch = new ArrayList<>(2);
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
          final ContentProviderResult[] result = client.applyBatch(batch);
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
