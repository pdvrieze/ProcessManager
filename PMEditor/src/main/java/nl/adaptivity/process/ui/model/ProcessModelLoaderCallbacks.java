package nl.adaptivity.process.ui.model;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import nl.adaptivity.process.models.ProcessModelProvider;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import nl.adaptivity.process.tasks.data.TaskProvider.Tasks;
import nl.adaptivity.process.ui.main.ListCursorLoaderCallbacks;
import nl.adaptivity.process.ui.task.TaskCursorAdapter;
import nl.adaptivity.sync.RemoteXmlSyncAdapter;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.XmlBaseColumns;


/**
 * Created by pdvrieze on 28/12/15.
 */
public class ProcessModelLoaderCallbacks extends ListCursorLoaderCallbacks<PMCursorAdapter> {

  public ProcessModelLoaderCallbacks(final Context context, final PMCursorAdapter adapter) {
    super(context, adapter);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    return new CursorLoader(mContext, ProcessModelProvider.ProcessModels.CONTENT_ID_URI_BASE, new String[] {BaseColumns._ID, ProcessModels.COLUMN_NAME}, XmlBaseColumns.COLUMN_SYNCSTATE + " IS NULL OR ( " + XmlBaseColumns.COLUMN_SYNCSTATE + " != " + RemoteXmlSyncAdapter.SYNC_DELETE_ON_SERVER + " AND " + XmlBaseColumns.COLUMN_SYNCSTATE + " != " + RemoteXmlSyncAdapter.SYNC_NEWDETAILSPENDING + " )", null, null);
  }

}
