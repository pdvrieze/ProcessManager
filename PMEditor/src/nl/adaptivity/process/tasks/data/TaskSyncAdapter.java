package nl.adaptivity.process.tasks.data;

import static nl.adaptivity.process.tasks.UserTask.NS_TASKS;
import static nl.adaptivity.process.tasks.UserTask.TAG_TASK;
import static nl.adaptivity.process.tasks.UserTask.TAG_TASKS;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import nl.adaptivity.android.darwin.AuthenticatedWebClient;
import nl.adaptivity.process.editor.android.SettingsActivity;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import nl.adaptivity.process.tasks.data.TaskProvider.Tasks;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;


public class TaskSyncAdapter extends AbstractThreadedSyncAdapter {

  private static final String TAG = TaskSyncAdapter.class.getSimpleName();
  private AuthenticatedWebClient mHttpClient;
  private String mBase;
  private XmlPullParserFactory mXpf;

  public TaskSyncAdapter(Context pContext) {
    super(pContext, true, false);
  }

  @Override
  public void onPerformSync(Account pAccount, Bundle pExtras, String pAuthority, ContentProviderClient pProvider, SyncResult pSyncResult) {
    try {
      mXpf = XmlPullParserFactory.newInstance();
    } catch (XmlPullParserException e1) {
      pSyncResult.stats.numParseExceptions++;
      return;
    }
    mXpf.setNamespaceAware(true);

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
    mBase = prefs.getString(SettingsActivity.PREF_SYNC_SOURCE, "https://darwin.bournemouth.ac.uk/PEUserMessageHandler/UserMessageService/");
    if (! mBase.endsWith("/")) {mBase = mBase+'/'; }
    {
      String authbase = AuthenticatedWebClient.getAuthBase(mBase);
      mHttpClient = new AuthenticatedWebClient(getContext(), authbase);
    }
    HttpGet getProcessesRequest = new HttpGet(mBase+"pendingTasks");
    HttpResponse result;
    try {
      result = mHttpClient.execute(getProcessesRequest);
    } catch (IOException e) {
      pSyncResult.stats.numIoExceptions++;
      return;
    }
    if (result!=null) {
      final int statusCode = result.getStatusLine().getStatusCode();
      if (statusCode>=200 && statusCode<400) {
        try {
          updateTaskList(pProvider, pSyncResult, result.getEntity().getContent());
        } catch (IllegalStateException|XmlPullParserException e) {
          pSyncResult.stats.numParseExceptions++;
          Log.e(TAG, "Error parsing process model list", e);
        } catch (IOException e) {
          pSyncResult.stats.numIoExceptions++;
          Log.e(TAG, "Error parsing process model list", e);
        } catch (RemoteException e) {
          pSyncResult.databaseError=true;
          Log.e(TAG, "Error parsing process model list", e);
        }
      } else {
        pSyncResult.stats.numIoExceptions++;
      }
    } else {
      pSyncResult.stats.numAuthExceptions++;
    }
  }

  private void updateTaskList(ContentProviderClient pProvider, SyncResult pSyncResult, InputStream pContent) throws XmlPullParserException, IOException, RemoteException {
    XmlPullParser parser = mXpf.newPullParser();
    parser.setInput(pContent, "UTF8");
    List<Long> handles = updateTaskList(pProvider, pSyncResult, parser);
  }

  private List<Long> updateTaskList(ContentProviderClient pProvider, SyncResult pSyncResult, XmlPullParser parser) throws XmlPullParserException, IOException, RemoteException {
    ContentValues values = new ContentValues(1);
    values.put(Tasks.COLUMN_SYNCSTATE, Integer.valueOf(Tasks.SYNC_PENDING));
    pProvider.update(Tasks.CONTENT_ID_URI_BASE, values, Tasks.COLUMN_SYNCSTATE + " = "+ProcessModels.SYNC_UPTODATE, null);
    try {
      List<Long> result = new ArrayList<>();
      parser.next();
      parser.require(START_TAG, NS_TASKS, TAG_TASKS);
      int type;
      while ((type = parser.next()) != END_TAG) {
        switch (type) {
          case START_TAG:
            result.add(parseTask(pProvider, pSyncResult, parser, -1)); // unknown id
            break;
          default:
            throw new XmlPullParserException("Unexpected tag type: " + type);
        }
      }
      return result;
    } finally {
      values.clear();
      values.put(ProcessModels.COLUMN_SYNCSTATE, Integer.valueOf(ProcessModels.SYNC_UPTODATE));
      pProvider.update(ProcessModels.CONTENT_ID_URI_BASE, values, ProcessModels.COLUMN_SYNCSTATE + " = "+ProcessModels.SYNC_PENDING, null);
    }
  }


  private Long parseTask(ContentProviderClient pProvider, SyncResult pSyncResult, XmlPullParser parser, long pId) throws XmlPullParserException, IOException, RemoteException {
    parser.require(START_TAG, NS_TASKS, TAG_TASK);
    String name = parser.getAttributeValue(null, "name");
    long handle;
    try {
      handle = Long.parseLong(parser.getAttributeValue(null, "handle"));
    } catch (NullPointerException|NumberFormatException e) {
      pSyncResult.stats.numSkippedEntries++;
      pSyncResult.stats.numParseExceptions++;
      return null;
    }
    parser.next();
    parser.require(END_TAG, NS_TASKS, TAG_TASK);
    final long id;
    Cursor localModel;
    if (pId<0) {
      localModel = pProvider.query(ProcessModels.CONTENT_ID_URI_BASE, ProcessModels.BASE_PROJECTION, ProcessModels.SELECT_HANDLE, new String[] { Long.toString(handle)}, null);
    } else {
      localModel = pProvider.query(ContentUris.withAppendedId(ProcessModels.CONTENT_ID_URI_BASE, pId), ProcessModels.BASE_PROJECTION, null, null, null);
    }
    if (localModel.moveToFirst()) {
      int col_id = localModel.getColumnIndex(BaseColumns._ID);
      int col_name = localModel.getColumnIndex(ProcessModels.COLUMN_NAME);
      id = localModel.getLong(col_id);
      Uri uri = ContentUris.withAppendedId(ProcessModels.CONTENT_ID_URI_BASE, id);
      ContentValues values = new ContentValues(2);
      boolean count = false;
      if (name!=null && (!name.equals(localModel.getString(col_name)))) {
        values.put(Tasks.COLUMN_SUMMARY, name);
        count = true;
      }
      values.put(Tasks.COLUMN_SYNCSTATE, Integer.valueOf(ProcessModels.SYNC_UPTODATE));
      if (handle!=-1) {
        values.put(Tasks.COLUMN_HANDLE, handle);
      }
      int cnt = pProvider.update(uri, values, null, null);
      if (count && cnt>0) {
        pSyncResult.stats.numUpdates+=cnt;
      }
    } else if (pId>=0) {
      pSyncResult.databaseError=true;
      throw new IllegalStateException("The database does not contain the expected id");
    } else {
      ContentValues values = new ContentValues(3);
      if (name!=null) {
        values.put(Tasks.COLUMN_SUMMARY, name);
      }
      values.put(Tasks.COLUMN_HANDLE, Long.valueOf(handle));
      values.put(Tasks.COLUMN_SYNCSTATE, Integer.valueOf(ProcessModels.SYNC_UPTODATE));
      Uri iduri = pProvider.insert(ProcessModels.CONTENT_ID_URI_BASE, values);
      id = ContentUris.parseId(iduri);
      pSyncResult.stats.numInserts++;
    }
    if (id>=0 && handle!=-1) {
      // TODO implement loading actual models from the server.
    }

    return Long.valueOf(handle);
  }
}