package nl.adaptivity.process.ui.main;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import nl.adaptivity.android.util.CursorRecyclerViewAdapter;


/**
 * Created by pdvrieze on 28/12/15.
 */
public abstract class ListCursorLoaderCallbacks<A extends CursorRecyclerViewAdapter> implements LoaderCallbacks<Cursor> {

  @NonNull protected final Context mContext;
  @NonNull protected final A mAdapter;

  public ListCursorLoaderCallbacks(final Context context, @NonNull final A adapter) {
    mContext = context;
    mAdapter = adapter;
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

    if (data!=null) {
      mAdapter.changeCursor(data);
    }
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    mAdapter.changeCursor(null);
  }
}
