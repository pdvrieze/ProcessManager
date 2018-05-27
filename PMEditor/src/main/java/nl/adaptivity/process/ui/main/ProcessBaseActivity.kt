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

package nl.adaptivity.process.ui.main

import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.annotation.CallSuper
import android.util.Log
import nl.adaptivity.android.compat.Compat
import nl.adaptivity.android.darwin.AuthenticatedWebClientFactory
import nl.adaptivity.android.graphics.AndroidTextMeasurer
import nl.adaptivity.android.graphics.AndroidTextMeasurer.AndroidMeasureInfo
import nl.adaptivity.diagram.svg.SVGCanvas
import nl.adaptivity.diagram.svg.SVGPath
import nl.adaptivity.diagram.svg.SVGPen
import nl.adaptivity.diagram.svg.SVGStrategy
import nl.adaptivity.process.diagram.DrawableProcessModel
import nl.adaptivity.process.diagram.RootDrawableProcessModel
import nl.adaptivity.process.editor.android.BuildConfig
import nl.adaptivity.process.editor.android.PMParcelable
import nl.adaptivity.process.editor.android.PMParser
import nl.adaptivity.process.editor.android.PMProcessesFragment.ProcessesCallback
import nl.adaptivity.process.editor.android.R
import nl.adaptivity.process.ui.ProcessSyncManager
import nl.adaptivity.process.ui.UIConstants
import nl.adaptivity.xml.AndroidXmlWriter
import nl.adaptivity.xml.XmlException
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlSerializer
import java.io.*


/**
 * Created by pdvrieze on 11/01/16.
 */
abstract class ProcessBaseActivity : AuthenticatedActivity(), ProcessesCallback {
    /** Process model that needs to be saved/exported.  */
    protected var mProcessModel: DrawableProcessModel? = null
    /** Temporary file for sharing.  */
    protected var mTmpFile: File? = null
    private var mSyncManager: ProcessSyncManager? = null

    open val syncManager: ProcessSyncManager?
        get() {
            val account = account
            if (account == null) {
                mSyncManager = null
            } else if (mSyncManager == null) {
                mSyncManager = ProcessSyncManager(this, AuthenticatedWebClientFactory.getStoredAccount(this))
            }
            return mSyncManager
        }

    private inner class FileStoreListener(private val mMimeType: String, private val mRequestCode: Int) {

        internal fun afterSave(result: File) {
            mTmpFile = result
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = mMimeType
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(result))
            startActivityForResult(shareIntent, mRequestCode)
        }

    }

    private inner class FileStoreTask @JvmOverloads constructor(private val mType: Int,
                                                        private val mPostSave: FileStoreListener,
                                                        private var mFile: File? = null) : AsyncTask<DrawableProcessModel, Any, File>() {

        override fun doInBackground(vararg params: DrawableProcessModel): File {
            val file:File = mFile ?: run {
                val ext = if (mType == TYPE_SVG) ".svg" else ".pm"
                File.createTempFile("tmp_", ext, externalCacheDir).also { mFile = it }

            }
            try {
                val out = FileOutputStream(file)
                try {
                    if (mType == TYPE_SVG) {
                        doExportSVG(out, RootDrawableProcessModel.get(params[0])!!)
                    } else {
                        doSaveFile(out, params[0].rootModel)
                    }
                } finally {
                    out.close()
                }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }

            return file
        }

        override fun onPostExecute(result: File) {
            mPostSave.afterSave(result)
        }

        override fun onCancelled(result: File) {
            if (mFile != null) {
                mFile!!.delete()
            }
        }


    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(UIConstants.KEY_TMPFILE)) {
                mTmpFile = File(savedInstanceState.getString(UIConstants.KEY_TMPFILE)!!)
            }
            if (savedInstanceState.containsKey(UIConstants.KEY_PROCESSMODEL)) {
                val pm = savedInstanceState.getParcelable<PMParcelable>(UIConstants.KEY_PROCESSMODEL)
                if (pm != null) {
                    mProcessModel = RootDrawableProcessModel.get(pm.processModel)
                }
            }

        }
    }

    @CallSuper
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            UIConstants.REQUEST_SHARE_PROCESSMODEL_FILE, UIConstants.REQUEST_SHARE_PROCESSMODEL_SVG -> mTmpFile!!.delete()
            UIConstants.REQUEST_SAVE_PROCESSMODEL                                                   -> if (resultCode == Activity.RESULT_OK) {
                doSaveFile(data, mProcessModel!!.rootModel)
            }
            UIConstants.REQUEST_EXPORT_PROCESSMODEL_SVG                                             -> if (resultCode == Activity.RESULT_OK) {
                doExportSVG(data, mProcessModel!!)
            }
        }

    }

    @Throws(FileNotFoundException::class)
    private fun getOutputStreamFromSave(data: Intent): OutputStream? {
        return contentResolver.openOutputStream(data.data!!)
    }

    protected fun doSaveFile(data: Intent, processModel: RootDrawableProcessModel) {
        try {
            val out = getOutputStreamFromSave(data)
            try {
                doSaveFile(out, processModel)
            } finally {
                out!!.close()
            }
        } catch (e: RuntimeException) {
            Log.e(TAG, "Failure to save file", e)
        } catch (e: IOException) {
            Log.e(TAG, "Failure to save file", e)
        }

    }

    @Throws(IOException::class)
    private fun doSaveFile(out: Writer, processModel: RootDrawableProcessModel) {
        try {
            PMParser.exportProcessModel(out, processModel)
        } catch (e: XmlPullParserException) {
            throw IOException(e)
        } catch (e: XmlException) {
            throw IOException(e)
        }

    }

    @Throws(IOException::class)
    private fun doSaveFile(out: OutputStream?, processModel: RootDrawableProcessModel) {
        try {
            PMParser.exportProcessModel(out, processModel)
        } catch (e: XmlException) {
            throw IOException(e)
        } catch (e: XmlPullParserException) {
            throw IOException(e)
        }

    }

    override fun requestShareFile(processModel: RootDrawableProcessModel?) {
        if (BuildConfig.DEBUG && processModel == null) {
            throw NullPointerException()
        }
        val task = FileStoreTask(TYPE_FILE, FileStoreListener("*/*", UIConstants.REQUEST_SHARE_PROCESSMODEL_FILE))
        task.execute(processModel as DrawableProcessModel?)
    }

    override fun requestSaveFile(processModel: RootDrawableProcessModel?) {
        if (BuildConfig.DEBUG && processModel == null) {
            throw NullPointerException()
        }
        mProcessModel = processModel
        requestSaveFile("*/*", UIConstants.REQUEST_SAVE_PROCESSMODEL)
    }

    override fun requestShareSVG(processModel: DrawableProcessModel?) {
        if (BuildConfig.DEBUG && processModel == null) {
            throw NullPointerException()
        }
        val task = FileStoreTask(TYPE_SVG, FileStoreListener("image/svg", UIConstants.REQUEST_SHARE_PROCESSMODEL_SVG))
        task.execute(processModel)
    }

    override fun requestExportSVG(processModel: DrawableProcessModel?) {
        if (BuildConfig.DEBUG && processModel == null) {
            throw NullPointerException()
        }
        mProcessModel = processModel
        requestSaveFile("image/svg", UIConstants.REQUEST_EXPORT_PROCESSMODEL_SVG)
    }

    private fun requestSaveFile(type: String, request: Int) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (prefs.getBoolean(SettingsActivity.PREF_KITKATFILE,
                             true) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            startKitkatSaveActivity(type, request)
        } else {
            var intent = Intent("org.openintents.action.PICK_FILE")
            if (supportsIntent(intent)) {
                intent.putExtra("org.openintents.extra.TITLE", getString(R.string.title_saveas))
                intent.putExtra("org.openintents.extra.BUTTON_TEXT", getString(R.string.btn_save))
                intent.data = Uri.withAppendedPath(Uri.fromFile(Compat.getDocsDirectory()), "/")
            } else {
                intent = Intent("com.estrongs.action.PICK_FILE")
                if (supportsIntent(intent)) {
                    intent.putExtra("com.estrongs.intent.extra.TITLE", getString(R.string.title_saveas))
                    //          intent.setData(Uri.withAppendedPath(Uri.fromFile(Compat.getDocsDirectory()),"/"));
                } else {
                    requestShareFile(mProcessModel!!.rootModel)
                    //          Toast.makeText(getActivity(), "Saving not yet supported without implementation", Toast.LENGTH_LONG).show();
                    return
                }
            }
            startActivityForResult(intent, request)
        }
    }

    private fun doExportSVG(data: Intent, processModel: DrawableProcessModel) {
        try {
            val out = getOutputStreamFromSave(data)
            try {
                doExportSVG(out, processModel)
            } finally {
                out!!.close()
            }
        } catch (e: RuntimeException) {
            Log.e(TAG, "Failure to save file", e)
        } catch (e: IOException) {
            Log.e(TAG, "Failure to save file", e)
        }

    }

    @Throws(IOException::class)
    private fun doExportSVG(out: OutputStream?, processModel: DrawableProcessModel) {
        try {
            val serializer = PMParser.getSerializer(out)
            doExportSVG(serializer, processModel)
        } catch (e: XmlPullParserException) {
            throw IOException(e)
        }

    }

    @Throws(IOException::class)
    private fun doExportSVG(out: Writer, processModel: DrawableProcessModel) {
        try {
            val serializer = PMParser.getSerializer(out)
            doExportSVG(serializer, processModel)
        } catch (e: XmlPullParserException) {
            throw IOException(e)
        }

    }

    @Throws(IOException::class)
    private fun doExportSVG(serializer: XmlSerializer, processModel: DrawableProcessModel) {
        val canvas = SVGCanvas(AndroidTextMeasurer())
        val modelBounds = processModel.bounds
        val offsetCanvas = canvas
                .childCanvas(-modelBounds.left, -modelBounds.top, 1.0)
        modelBounds.left = 0.0// set the origin to the actual top left corner of the image.
        modelBounds.top = 0.0
        canvas.bounds = modelBounds
        processModel.draw<SVGStrategy<AndroidMeasureInfo>, SVGPen<AndroidMeasureInfo>, SVGPath>(offsetCanvas, null)
        serializer.startDocument(null, null)
        serializer.ignorableWhitespace("\n")
        serializer.comment("Generated by PMEditor")
        serializer.ignorableWhitespace("\n")
        canvas.serialize(AndroidXmlWriter(serializer))
        serializer.ignorableWhitespace("\n")
        serializer.flush()
    }

    private fun supportsIntent(intent: Intent): Boolean {
        return !packageManager.queryIntentActivities(intent, 0).isEmpty()
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun startKitkatSaveActivity(type: String, request: Int) {
        val i = Intent(Intent.ACTION_CREATE_DOCUMENT)
        i.type = type
        i.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(i, request)
    }

    @CallSuper
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mProcessModel != null) {
            outState.putParcelable(UIConstants.KEY_PROCESSMODEL, PMParcelable(mProcessModel!!.rootModel))
        }
        if (mTmpFile != null) {
            outState.putString(UIConstants.KEY_TMPFILE, mTmpFile!!.path)
        }
    }

    companion object {

        private val TAG = "ProcessBaseActivity"
        private val TYPE_FILE = 0
        private val TYPE_SVG = 1
    }
}
