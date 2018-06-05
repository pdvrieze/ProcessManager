/*
 * Copyright (c) 2018.
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

package nl.adaptivity.process.ui.model

import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.content.SyncStatusObserver
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.BaseColumns
import android.support.v4.app.LoaderManager.LoaderCallbacks
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import android.widget.AdapterView
import nl.adaptivity.android.recyclerview.ClickableAdapter
import nl.adaptivity.android.recyclerview.ClickableAdapter.OnItemClickListener
import nl.adaptivity.android.recyclerview.ClickableViewHolder
import nl.adaptivity.android.recyclerview.SelectableAdapter
import nl.adaptivity.android.util.GetNameDialogFragment
import nl.adaptivity.android.util.GetNameDialogFragment.GetNameDialogFragmentCallbacks
import nl.adaptivity.android.util.MasterListFragment
import nl.adaptivity.process.diagram.DrawableProcessNode
import nl.adaptivity.process.diagram.LayoutAlgorithm
import nl.adaptivity.process.diagram.RootDrawableProcessModel
import nl.adaptivity.process.editor.android.PMEditor
import nl.adaptivity.process.editor.android.PMParser
import nl.adaptivity.process.editor.android.R
import nl.adaptivity.process.models.ProcessModelProvider
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels
import nl.adaptivity.process.ui.ProcessSyncManager
import nl.adaptivity.process.ui.main.SettingsActivity
import nl.adaptivity.process.ui.model.PMCursorAdapter.PMViewHolder
import nl.adaptivity.sync.RemoteXmlSyncAdapter
import nl.adaptivity.sync.RemoteXmlSyncAdapter.XmlBaseColumns
import nl.adaptivity.sync.SyncManager.SyncStatusObserverData
import java.io.IOException
import java.util.*


/**
 * A list fragment representing a list of ProcessModels. This fragment also
 * supports tablet devices by allowing list items to be given an 'activated'
 * state upon selection. This helps indicate which item is currently being
 * viewed in a [ProcessModelDetailFragment].
 *
 *
 * Activities containing this fragment MUST implement the [ListCallbacks]
 * interface.
 */
/**
 * Mandatory empty constructor for the fragment manager to instantiate the
 * fragment (e.g. upon screen orientation changes).
 */
class ProcessModelListFragment : MasterListFragment<ProcessSyncManager>(), LoaderCallbacks<Cursor>, GetNameDialogFragmentCallbacks, OnRefreshListener, SelectableAdapter.OnSelectionListener<ClickableViewHolder>, OnItemClickListener<PMViewHolder> {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var syncObserver: SyncStatusObserver? = null
    private var syncObserverHandle: SyncStatusObserverData? = null
    private var manualSync: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        loaderManager.initLoader(LOADERID, null, this)
        val adapter = PMCursorAdapter(activity, null)
        adapter.onSelectionListener = this
        adapter.setOnItemClickListener(this)
        setListAdapter(adapter)

        syncObserver = SyncStatusObserver { activity!!.runOnUiThread { updateSyncState() } }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.refreshablerecyclerview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView!!.layoutManager = LinearLayoutManager(activity)
        swipeRefresh = view.findViewById<View>(R.id.swipeRefresh) as SwipeRefreshLayout
        swipeRefresh.setOnRefreshListener(this)
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onSelectionChanged(adapter: SelectableAdapter<ClickableViewHolder>) {
        if (adapter.selectedId != RecyclerView.NO_ID) {
            doOnItemSelected(adapter.selectedPos, adapter.selectedId)
        }

    }

    override fun onClickItem(adapter: ClickableAdapter<out PMViewHolder>, viewHolder: PMViewHolder): Boolean {
        return doOnItemClicked(viewHolder.adapterPosition, viewHolder.itemId)
    }

    override fun getListAdapter(): PMCursorAdapter? {
        return super.getListAdapter() as PMCursorAdapter?
    }

    override fun onResume() {
        super.onResume()
        syncObserverHandle = callbacks.syncManager?.addOnStatusChangeObserver(ProcessModelProvider.AUTHORITY,
                                                                               syncObserver!!)
        syncObserver!!.onStatusChanged(0) // trigger status sync
    }

    override fun onPause() {
        super.onPause()
        val syncManager = callbacks.syncManager
        if (syncObserverHandle != null) {
            syncManager!!.removeOnStatusChangeObserver(syncObserverHandle!!)
            syncObserverHandle = null
        }
        syncManager?.verifyNoObserversActive()

    }

    /**
     * Called by the refresh view when a manual refresh is requested.
     */
    override fun onRefresh() {
        doManualRefresh()
    }

    private fun doManualRefresh() {
        callbacks.syncManager?.run {
          requestSyncProcessModelList(true, ProcessSyncManager.DEFAULT_MIN_AGE)
          manualSync = true
          updateSyncState()
        }

    }

    private fun updateSyncState() {
        callbacks.syncManager?.let { syncManager ->
          if (!syncManager.isSyncable(ProcessModelProvider.AUTHORITY)) {
            swipeRefresh.isRefreshing = false
          } else {
            val syncActive = syncManager.isProcessModelSyncActive
            val syncPending = syncManager.isProcessModelSyncPending
            if (syncActive || !syncPending) {
              manualSync = false
            }
            val sync = syncActive || manualSync
            swipeRefresh.isRefreshing = sync
          }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater!!.inflate(R.menu.pmlist_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.menu_add_pm    -> {
                createNewPM()
                return true
            }
            R.id.ac_import      -> {
                val importIntent = Intent(Intent.ACTION_GET_CONTENT)
                importIntent.addCategory(Intent.CATEGORY_OPENABLE)
                importIntent.type = "*/*"
                startActivityForResult(Intent.createChooser(importIntent, "Import from"), REQUEST_IMPORT)
                return true
            }
            R.id.menu_settings  -> {
                val settingsIntent = Intent(activity, SettingsActivity::class.java)
                startActivity(settingsIntent)
                return true
            }
            R.id.ac_sync_models -> {
                doManualRefresh()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_IMPORT) {
                try {
                    val `in` = activity!!.contentResolver.openInputStream(data!!.data!!)
                    try {
                        val pm = PMParser.parseProcessModel(`in`, LayoutAlgorithm.nullalgorithm<DrawableProcessNode>(),
                                                            LayoutAlgorithm())
                        pm!!.handle = -1L
                        val uri = ProcessModelProvider.newProcessModel(activity, pm.build())
                        val id = ContentUris.parseId(uri)
                        if (isVisible && !isRemoving) {
                            doOnItemSelected(AdapterView.INVALID_POSITION, id)
                        }
                    } finally {
                        `in`!!.close()
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Failure to import file", e)
                }

            }
        }
    }

    private fun createNewPM() {
        GetNameDialogFragment.show(fragmentManager!!, DLG_NEW_PM_NAME, "Model name", "Provide the new name", this)
    }

    override fun onNameDialogCompletePositive(dialog: GetNameDialogFragment, id: Int, string: String) {
        createNewPM(string)
    }

    override fun onNameDialogCompleteNegative(dialog: GetNameDialogFragment, id: Int) {
        // ignore
    }

    internal fun createNewPM(name: String) {

        val model = RootDrawableProcessModel(UUID.randomUUID(), name, ArrayList())
        val uri: Uri
        try {
            uri = ProcessModelProvider.newProcessModel(activity, model)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        val editIntent = Intent(activity, PMEditor::class.java)
        editIntent.data = ContentUris.withAppendedId(ProcessModels.CONTENT_ID_STREAM_BASE, ContentUris.parseId(uri))
        startActivity(editIntent)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        return CursorLoader(activity!!, ProcessModelProvider.ProcessModels.CONTENT_ID_URI_BASE,
                            arrayOf(BaseColumns._ID, ProcessModels.COLUMN_NAME),
                            XmlBaseColumns.COLUMN_SYNCSTATE + " IS NULL OR ( " + XmlBaseColumns.COLUMN_SYNCSTATE + " != " + RemoteXmlSyncAdapter.SYNC_DELETE_ON_SERVER + " AND " + XmlBaseColumns.COLUMN_SYNCSTATE + " != " + RemoteXmlSyncAdapter.SYNC_NEWDETAILSPENDING + " )",
                            null, null)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        if (data != null) {
            listAdapter!!.changeCursor(data)
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        listAdapter!!.changeCursor(null)
    }

    companion object {

        private val LOADERID = 3

        private val REQUEST_IMPORT = 31

        private val TAG = ProcessModelListFragment::class.java.simpleName

        private val DLG_NEW_PM_NAME = 2
    }
}
