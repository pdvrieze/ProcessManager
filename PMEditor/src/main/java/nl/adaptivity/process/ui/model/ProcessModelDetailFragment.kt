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

package nl.adaptivity.process.ui.model

import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.databinding.DataBindingUtil
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.LoaderManager.LoaderCallbacks
import android.support.v4.content.Loader
import android.util.Log
import android.view.*
import android.view.View.OnClickListener
import android.view.View.OnLayoutChangeListener
import nl.adaptivity.android.util.GetNameDialogFragment
import nl.adaptivity.android.util.GetNameDialogFragment.GetNameDialogFragmentCallbacks
import nl.adaptivity.process.android.ProcessModelUtil
import nl.adaptivity.process.diagram.DrawableProcessModel.Builder
import nl.adaptivity.process.diagram.RootDrawableProcessModel
import nl.adaptivity.process.editor.android.BaseProcessAdapter
import nl.adaptivity.process.editor.android.PMEditor
import nl.adaptivity.process.editor.android.PMProcessesFragment
import nl.adaptivity.process.editor.android.PMProcessesFragment.PMProvider
import nl.adaptivity.process.editor.android.R
import nl.adaptivity.process.editor.android.databinding.FragmentProcessmodelDetailBinding
import nl.adaptivity.process.models.ProcessModelHolder
import nl.adaptivity.process.models.ProcessModelLoader
import nl.adaptivity.process.models.ProcessModelProvider
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels
import nl.adaptivity.process.ui.ProcessSyncManager
import nl.adaptivity.sync.RemoteXmlSyncAdapter
import java.io.IOException
import java.util.*

/**
 * A fragment representing a single ProcessModel detail screen. This fragment is
 * either contained in a [nl.adaptivity.process.ui.main.OverviewActivity] in two-pane mode (on
 * tablets) or a [ProcessModelDetailActivity] on handsets.
 */
/**
 * Mandatory empty constructor for the fragment manager to instantiate the
 * fragment (e.g. upon screen orientation changes).
 */
class ProcessModelDetailFragment : PMProcessesFragment(), LoaderCallbacks<ProcessModelHolder>, OnClickListener, PMProvider {

    private var callbacks: ProcessModelDetailFragmentCallbacks? = null

    /**
     * The process model represented by this fragment
     */
    private var item: BaseProcessAdapter? = null

    private val mProcessesFragment: PMProcessesFragment? = null

    private var mProcessModelId: Long = 0

    private var mModelHandle: Long? = null
    private lateinit var mBinding: FragmentProcessmodelDetailBinding

    private val currentProcessUri: Uri
        get() = ContentUris.withAppendedId(ProcessModelProvider.ProcessModels.CONTENT_ID_URI_BASE, mProcessModelId)

    interface ProcessModelDetailFragmentCallbacks {

        val syncManager: ProcessSyncManager?
        fun onProcessModelSelected(processModelId: Long)

        fun onInstantiateModel(modelId: Long, suggestedName: String)
    }

    private inner class ModelViewLayoutChangeListener : OnLayoutChangeListener {

        override fun onLayoutChange(v: View,
                                    left: Int,
                                    top: Int,
                                    right: Int,
                                    bottom: Int,
                                    oldLeft: Int,
                                    oldTop: Int,
                                    oldRight: Int,
                                    oldBottom: Int) {
            if (item != null && (oldRight - oldLeft != right - left || oldBottom - oldTop != bottom - top)) {
                updateDiagramScale()
            }
        }

    }

    override fun onAttach(activity: Activity?) {
        super.onAttach(activity)
        if (activity is ProcessModelDetailFragmentCallbacks) {
            callbacks = activity
        }
    }

    internal fun updateDiagramScale() {
        val diagramBounds = RectF()
        item!!.getBounds(diagramBounds)
        val diagramView = mBinding.diagramView1
        var scale = Math.min(diagramView.width / diagramBounds.width(), diagramView.height / diagramBounds.height())
        diagramView.scale = scale.toDouble()
        scale = diagramView.scale.toFloat()

        val w2 = diagramView.width / scale

        diagramView.offsetX = (diagramBounds.left - (w2 - diagramBounds.width()) / 2).toDouble()

        val h2 = diagramView.height / scale
        diagramView.offsetY = (diagramBounds.top - (h2 - diagramBounds.height()) / 2).toDouble()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (arguments!!.containsKey(ARG_ITEM_ID)) {
            loaderManager.initLoader(LOADER_ITEM, arguments, this)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_processmodel_detail, container, false)
        mBinding.data = ProcessModelHolder()

        mBinding.diagramView1.addOnLayoutChangeListener(ModelViewLayoutChangeListener())

        mBinding.btnPmEdit.setOnClickListener(this)

        mBinding.btnPmExec.setOnClickListener(this)

        mBinding.btnPmClone.setOnClickListener(this)

        mBinding.btnPmPublish.setOnClickListener(this)

        return mBinding.root
    }

    override fun onPause() {
        super.onPause()
        if (mModelHandle != null) {
            val checked = mBinding.checkboxFavourite.isChecked
            if (checked != mBinding.data!!.isFavourite()) {
                mBinding.data!!.setFavourite(mBinding.checkboxFavourite.isChecked)
                val uri = ContentUris.withAppendedId(ProcessModels.CONTENT_ID_STREAM_BASE, mProcessModelId)
                val cv = ContentValues(1)
                cv.put(ProcessModels.COLUMN_FAVOURITE, checked)
                activity!!.contentResolver.update(uri, cv, null, null)
            }
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<ProcessModelHolder> {
        mProcessModelId = args!!.getLong(ARG_ITEM_ID)
        val uri = ContentUris.withAppendedId(ProcessModelProvider.ProcessModels.CONTENT_ID_STREAM_BASE, mProcessModelId)
        return ProcessModelLoader(activity, uri)
    }

    override fun onLoadFinished(loader: Loader<ProcessModelHolder>, data: ProcessModelHolder?) {
        val pm: RootDrawableProcessModel
        Log.d(TAG, "onLoadFinished: ")
        mBinding.processmodelDetailSpinner.visibility = View.GONE
        mBinding.data = data

        if (data?.model === null) {
            mBinding.diagramView1.adapter = null
            item = null
            mModelHandle = null

        } else {
            mBinding.diagramView1.parent.requestLayout() // Do a layout
            item = data.model?.let { BaseProcessAdapter(it) }
            mModelHandle = data.handle
            mBinding.diagramView1.adapter = item
            updateDiagramScale()
        }
    }

    override fun onLoaderReset(loader: Loader<ProcessModelHolder>) {
        mBinding.processmodelName.text = null
        item = null
        mBinding.diagramView1.adapter = null
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_pm_edit    -> {
                btnPmEditClicked()
                return
            }
            R.id.btn_pm_clone   -> {
                btnPmCloneClicked()
                return
            }
            R.id.btn_pm_publish -> {
                btnPmPublishClicked()
                return
            }
            R.id.btn_pm_exec    -> {
                btnPmExecClicked()
                return
            }
        }
    }

    fun btnPmEditClicked() {
        val intent = Intent(activity, PMEditor::class.java)
        val id = arguments!!.getLong(ARG_ITEM_ID)
        intent.data = ContentUris.withAppendedId(ProcessModels.CONTENT_ID_STREAM_BASE, id)
        startActivity(intent)
    }

    fun btnPmExecClicked() {
        val id = arguments!!.getLong(ARG_ITEM_ID)
        callbacks!!.onInstantiateModel(id, mBinding.processmodelName.text.toString() + " Instance")
    }

    fun btnPmCloneClicked() {
        val previousName = mBinding.processmodelName.text
        val suggestedNewName = ProcessModelUtil.suggestNewName(activity!!, previousName)

        GetNameDialogFragment.show(fragmentManager!!, DLG_NEW_MODEL_NAME_CLONE, "Model name", "Provide the new name",
                                   object : GetNameDialogFragmentCallbacks {

                                       override fun onNameDialogCompletePositive(dialog: GetNameDialogFragment,
                                                                                 id: Int,
                                                                                 string: String) {
                                           cloneWithName(string)
                                       }

                                       override fun onNameDialogCompleteNegative(dialog: GetNameDialogFragment,
                                                                                 id: Int) {
                                           // ignore
                                       }
                                   }, suggestedNewName)
        // Don't do anything yet
    }

    protected fun cloneWithName(newName: String) {
        // TODO Auto-generated method stub
        val currentModel = (mBinding.diagramView1.adapter as BaseProcessAdapter).diagram
        if (currentModel is RootDrawableProcessModel.Builder) {

            val newModel = currentModel.copy()
            newModel.name = newName
            newModel.uuid = UUID.randomUUID()

            val uri: Uri
            try {
                uri = ProcessModelProvider.newProcessModel(activity, newModel.build())
            } catch (e: IOException) {
                throw RuntimeException(e)
            }

            if (callbacks != null) {
                callbacks!!.onProcessModelSelected(ContentUris.parseId(uri))
            }
        }
    }

    fun btnPmPublishClicked() {
        val itemUri = currentProcessUri
        val cv = ContentValues(1)
        cv.put(ProcessModels.COLUMN_SYNCSTATE, Integer.valueOf(RemoteXmlSyncAdapter.SYNC_PUBLISH_TO_SERVER))
        val contentResolver = activity!!.contentResolver
        contentResolver.update(itemUri, cv, null, null)
        mBinding.btnPmPublish.isEnabled = false
        // XXX verify that this is really not needed
        //    mCallbacks.getSyncManager().requestSyncProcessModelList(true, minAge);
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater!!.inflate(R.menu.pm_detail_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item!!.itemId == R.id.ac_delete) {
            onDeleteItem()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onDeleteItem(): Boolean {
        val uri = currentProcessUri
        val result: Boolean
        if (mModelHandle == null) {
            result = activity!!.contentResolver.delete(uri, null, null) > 0
        } else {
            val cv = ContentValues(1)
            cv.put(ProcessModels.COLUMN_SYNCSTATE, Integer.valueOf(RemoteXmlSyncAdapter.SYNC_DELETE_ON_SERVER))
            result = activity!!.contentResolver.update(uri, cv, null, null) > 0
            // XXX verify that this is really not needed
            //      mCallbacks.getSyncManager().requestSyncProcessModelList(true, minAge);
        }
        if (result && callbacks != null) {
            callbacks!!.onProcessModelSelected(-1)
        }
        return result
    }

    override fun getProcessModel(): Builder {
        return item!!.diagram
    }

    companion object {

        private val TAG = "ProcModelDetailFrag"

        /**
         * The fragment argument representing the item ID that this fragment
         * represents.
         */
        val ARG_ITEM_ID = "item_id"

        private val LOADER_ITEM = 0

        private val DLG_NEW_MODEL_NAME_CLONE = 3
    }
}
