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

package nl.adaptivity.process.ui.main;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.util.Log;
import nl.adaptivity.android.compat.Compat;
import nl.adaptivity.android.darwin.AuthenticatedWebClient;
import nl.adaptivity.android.darwin.AuthenticatedWebClientFactory;
import nl.adaptivity.android.graphics.AndroidTextMeasurer;
import nl.adaptivity.android.graphics.AndroidTextMeasurer.AndroidMeasureInfo;
import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.svg.SVGCanvas;
import nl.adaptivity.diagram.svg.SVGPath;
import nl.adaptivity.diagram.svg.SVGPen;
import nl.adaptivity.diagram.svg.SVGStrategy;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.clientProcessModel.RootClientProcessModel;
import nl.adaptivity.process.diagram.RootDrawableProcessModel;
import nl.adaptivity.process.processModel.RootProcessModelBase;
import nl.adaptivity.process.ui.ProcessSyncManager;
import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.editor.android.BuildConfig;
import nl.adaptivity.process.editor.android.PMParcelable;
import nl.adaptivity.process.editor.android.PMParser;
import nl.adaptivity.process.editor.android.PMProcessesFragment.ProcessesCallback;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.ui.UIConstants;
import nl.adaptivity.xml.AndroidXmlWriter;
import nl.adaptivity.xml.XmlException;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.*;


/**
 * Created by pdvrieze on 11/01/16.
 */
public abstract class ProcessBaseActivity extends AuthenticatedActivity implements ProcessesCallback {

  private class FileStoreListener {

    private final String mMimeType;
    private final int    mRequestCode;

    public FileStoreListener(final String mimeType, final int requestCode) {
      mMimeType = mimeType;
      mRequestCode = requestCode;
    }

    void afterSave(final File result) {
      mTmpFile = result;
      final Intent shareIntent = new Intent(Intent.ACTION_SEND);
      shareIntent.setType(mMimeType);
      shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(result));
      startActivityForResult(shareIntent, mRequestCode);
    }

  }

  public class FileStoreTask extends AsyncTask<ClientProcessModel<?, ?>, Object, File> {
    private       File              mFile;
    private final FileStoreListener mPostSave;
    private final int               mType;

    public FileStoreTask(final int type, final FileStoreListener postSave) {
      this(type, postSave, null);
    }

    public FileStoreTask(final int type, final FileStoreListener postSave, final File file) {
      mType = type;
      mPostSave = postSave;
      mFile = file;
    }

    @Override
    protected File doInBackground(final ClientProcessModel<?, ?>... params) {
      if (mFile == null) {
        try {
          final String ext = mType==TYPE_SVG ? ".svg" :".pm";
          mFile = File.createTempFile("tmp_", ext, getExternalCacheDir());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
      try {
        final FileOutputStream out = new FileOutputStream(mFile);
        try {
          if (mType == TYPE_SVG) {
            doExportSVG(out, RootDrawableProcessModel.get(params[0]));
          } else {
            doSaveFile(out, params[0].getRootModel());
          }
        } finally {
          out.close();
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return mFile;
    }
    @Override
    protected void onPostExecute(final File result) {
      mPostSave.afterSave(result);
    }

    @Override
    protected void onCancelled(final File result) {
      if (mFile!=null) {
        mFile.delete();
      }
    }



  }

  private static final String TAG = "ProcessBaseActivity";
  private static final int TYPE_FILE = 0;
  private static final int TYPE_SVG = 1;
  /** Process model that needs to be saved/exported. */
  protected ClientProcessModel<?, ?> mProcessModel;
  /** Temporary file for sharing. */
  protected File mTmpFile;
  private ProcessSyncManager mSyncManager;

  @Override
  protected void onCreate(@Nullable final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState!=null) {
      if (savedInstanceState.containsKey(UIConstants.KEY_TMPFILE)) {
        mTmpFile = new File (savedInstanceState.getString(UIConstants.KEY_TMPFILE));
      }
      if (savedInstanceState.containsKey(UIConstants.KEY_PROCESSMODEL)) {
        final PMParcelable pm = savedInstanceState.getParcelable(UIConstants.KEY_PROCESSMODEL);
        if (pm!=null) {
          mProcessModel = RootDrawableProcessModel.get(pm.getProcessModel());
        }
      }

    }
  }

  @CallSuper
  @Override
  protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case UIConstants.REQUEST_SHARE_PROCESSMODEL_FILE:
      case UIConstants.REQUEST_SHARE_PROCESSMODEL_SVG:
        mTmpFile.delete();
        break;
      case UIConstants.REQUEST_SAVE_PROCESSMODEL:
        if (resultCode == Activity.RESULT_OK) {
          doSaveFile(data, (RootClientProcessModel<?,?>)mProcessModel);
        }
        break;
      case UIConstants.REQUEST_EXPORT_PROCESSMODEL_SVG:
        if (resultCode==Activity.RESULT_OK) {
          doExportSVG(data, RootDrawableProcessModel.get(mProcessModel));
        }
        break;
    }

  }

  private OutputStream getOutputStreamFromSave(final Intent data) throws FileNotFoundException {
    return getContentResolver().openOutputStream(data.getData());
  }

  protected void doSaveFile(final Intent data, final RootClientProcessModel<?, ?> processModel) {
    try {
      final OutputStream out = getOutputStreamFromSave(data);
      try {
        doSaveFile(out, processModel);
      } finally {
        out.close();
      }
    } catch (RuntimeException| IOException e) {
      Log.e(TAG, "Failure to save file", e);
    }
  }

  private void doSaveFile(final Writer out, final RootClientProcessModel<?, ?> processModel) throws IOException {
    try {
      PMParser.exportProcessModel(out , processModel);
    } catch (XmlPullParserException | XmlException e) {
      throw new IOException(e);
    }
  }

  private void doSaveFile(final OutputStream out, final RootClientProcessModel<?, ?> processModel) throws IOException {
    try {
      PMParser.exportProcessModel(out , processModel);
    } catch (XmlException | XmlPullParserException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void requestShareFile(final RootClientProcessModel<?, ?> processModel) {
    if (BuildConfig.DEBUG && processModel == null) { throw new NullPointerException(); }
    final FileStoreTask task = new FileStoreTask(TYPE_FILE, new FileStoreListener("*/*", UIConstants.REQUEST_SHARE_PROCESSMODEL_FILE));
    task.execute(processModel);
  }

  @Override
  public void requestSaveFile(final RootClientProcessModel<?, ?> processModel) {
    if (BuildConfig.DEBUG && processModel == null) { throw new NullPointerException(); }
    mProcessModel = (ClientProcessModel<?, ?>) processModel;
    requestSaveFile("*/*", UIConstants.REQUEST_SAVE_PROCESSMODEL);
  }

  @Override
  public void requestShareSVG(final ClientProcessModel<?, ?> processModel) {
    if (BuildConfig.DEBUG && processModel == null) { throw new NullPointerException(); }
    final FileStoreTask task = new FileStoreTask(TYPE_SVG, new FileStoreListener("image/svg", UIConstants.REQUEST_SHARE_PROCESSMODEL_SVG));
    task.execute(processModel);
  }

  @Override
  public void requestExportSVG(final ClientProcessModel<?, ?> processModel) {
    if (BuildConfig.DEBUG && processModel == null) { throw new NullPointerException(); }
    mProcessModel = processModel;
    requestSaveFile("image/svg", UIConstants.REQUEST_EXPORT_PROCESSMODEL_SVG);
  }

  private void requestSaveFile(final String type, final int request) {
    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    if (prefs.getBoolean(SettingsActivity.PREF_KITKATFILE, true) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      startKitkatSaveActivity(type, request);
    } else {
      Intent intent = new Intent("org.openintents.action.PICK_FILE");
      if (supportsIntent(intent)) {
        intent.putExtra("org.openintents.extra.TITLE", getString(R.string.title_saveas));
        intent.putExtra("org.openintents.extra.BUTTON_TEXT", getString(R.string.btn_save));
        intent.setData(Uri.withAppendedPath(Uri.fromFile(Compat.getDocsDirectory()), "/"));
      } else {
        intent = new Intent("com.estrongs.action.PICK_FILE");
        if (supportsIntent(intent)) {
          intent.putExtra("com.estrongs.intent.extra.TITLE", getString(R.string.title_saveas));
//          intent.setData(Uri.withAppendedPath(Uri.fromFile(Compat.getDocsDirectory()),"/"));
        } else {
          requestShareFile(mProcessModel.getRootModel());
//          Toast.makeText(getActivity(), "Saving not yet supported without implementation", Toast.LENGTH_LONG).show();
          return;
        }
      }
      startActivityForResult(intent, request);
    }
  }

  private void doExportSVG(final Intent data, final DrawableProcessModel processModel) {
    try {
      final OutputStream out = getOutputStreamFromSave(data);
      try {
        doExportSVG(out, processModel);
      } finally {
        out.close();
      }
    } catch (RuntimeException| IOException e) {
      Log.e(TAG, "Failure to save file", e);
    }
  }

  private void doExportSVG(final OutputStream out, final DrawableProcessModel processModel) throws IOException {
    try {
      final XmlSerializer serializer = PMParser.getSerializer(out);
      doExportSVG(serializer, processModel);
    } catch (XmlPullParserException e) {
      throw new IOException(e);
    }
  }

  private void doExportSVG(final Writer out, final DrawableProcessModel processModel) throws IOException {
    try {
      final XmlSerializer serializer = PMParser.getSerializer(out);
      doExportSVG(serializer, processModel);
    } catch (XmlPullParserException e) {
      throw new IOException(e);
    }
  }

  private void doExportSVG(final XmlSerializer serializer, final DrawableProcessModel processModel) throws IOException {
    final SVGCanvas<AndroidMeasureInfo> canvas = new SVGCanvas<>(new AndroidTextMeasurer());
    final Rectangle modelBounds = processModel.getBounds();
    final Canvas<SVGStrategy<AndroidMeasureInfo>, SVGPen<AndroidMeasureInfo>, SVGPath> offsetCanvas = canvas
            .childCanvas(-modelBounds.left, -modelBounds.top, 1d);
    modelBounds.left=0d;// set the origin to the actual top left corner of the image.
    modelBounds.top=0d;
    canvas.setBounds(modelBounds);
    processModel.draw(offsetCanvas, null);
    serializer.startDocument(null, null);
    serializer.ignorableWhitespace("\n");
    serializer.comment("Generated by PMEditor");
    serializer.ignorableWhitespace("\n");
    try {
      canvas.serialize(new AndroidXmlWriter(serializer));
    } catch (XmlException e) {
      throw new IOException(e);
    }
    serializer.ignorableWhitespace("\n");
    serializer.flush();
  }

  private boolean supportsIntent(final Intent intent) {
    return ! getPackageManager().queryIntentActivities(intent, 0).isEmpty();
  }

  @TargetApi(Build.VERSION_CODES.KITKAT)
  private void startKitkatSaveActivity(final String type, final int request) {
    final Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
    i.setType(type);
    i.addCategory(Intent.CATEGORY_OPENABLE);
    startActivityForResult(i, request);
  }

  @CallSuper
  @Override
  protected void onSaveInstanceState(final Bundle outState) {
    super.onSaveInstanceState(outState);
    if (mProcessModel!=null) { outState.putParcelable(UIConstants.KEY_PROCESSMODEL, new PMParcelable(mProcessModel)); }
    if (mTmpFile!=null) { outState.putString(UIConstants.KEY_TMPFILE, mTmpFile.getPath()); }
  }

  public ProcessSyncManager getSyncManager() {
    final Account account = getAccount();
    if (account==null) {
      mSyncManager = null;
    } else if (mSyncManager==null) {
      mSyncManager = new ProcessSyncManager(this, AuthenticatedWebClientFactory.getStoredAccount(this));
    }
    return mSyncManager;
  }
}
