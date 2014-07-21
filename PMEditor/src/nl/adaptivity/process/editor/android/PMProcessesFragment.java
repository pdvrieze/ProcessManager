package nl.adaptivity.process.editor.android;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import nl.adaptivity.android.compat.Compat;
import nl.adaptivity.android.graphics.AndroidTextMeasurer;
import nl.adaptivity.android.graphics.AndroidTextMeasurer.AndroidMeasureInfo;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.diagram.svg.SVGCanvas;

import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;


public class PMProcessesFragment extends Fragment {

  private static final String TAG=PMProcessesFragment.class.getSimpleName();
  private static final int REQUEST_SAVE_FILE = 42;
  private static final int REQUEST_EXPORT_SVG = 43;
  private static final int REQUEST_SHARE_FILE = 44;
  private static final int REQUEST_SHARE_SVG = 45;
  private static final int TYPE_FILE = 0;
  private static final int TYPE_SVG = 0;
  private static final String KEY_PROCESSMODEL = "pm";
  private static final String KEY_FILE = "tmpfile";
  public static final String ARG_MENU = "menu";

  public interface PMProvider {
    ClientProcessModel<?> getProcessModel();
  }

  private class FileStoreListener {

    private String mMimeType;
    private int mRequestCode;

    public FileStoreListener(String pMimeType, int pRequestCode) {
      mMimeType = pMimeType;
      mRequestCode = pRequestCode;
    }

    void afterSave(File pResult) {
      mTmpFile = pResult;
      Intent shareIntent = new Intent(Intent.ACTION_SEND);
      shareIntent.setType(mMimeType);
      shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(pResult));
      startActivityForResult(shareIntent, mRequestCode);
    }

  }



  public class FileStoreTask extends AsyncTask<ClientProcessModel<?>, Object, File> {
    private File mFile;
    private FileStoreListener mPostSave;
    private int mType;

    public FileStoreTask(int type, FileStoreListener postSave) {
      this(type, postSave, null);
    }

    public FileStoreTask(int type, FileStoreListener postSave, File pFile) {
      mType = type;
      mPostSave = postSave;
      mFile = pFile;
    }

    @Override
    protected File doInBackground(ClientProcessModel<?>... pParams) {
      if (mFile == null) {
        try {
          mFile = File.createTempFile("tmp_", ".pm", getActivity().getExternalCacheDir());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
      try {
        FileOutputStream out = new FileOutputStream(mFile);
        try {
          if (mType==TYPE_SVG) {
            doExportSVG(out, DrawableProcessModel.get(pParams[0]));
          } else {
            doSaveFile(out, pParams[0]);
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
    protected void onPostExecute(File pResult) {
      mPostSave.afterSave(pResult);
    }

    @Override
    protected void onCancelled(File pResult) {
      if (mFile!=null) {
        mFile.delete();
      }
    }



  }

  private ClientProcessModel<?> mProcessModel;
  protected File mTmpFile;
  private boolean mMenu;
  private PMProvider mProvider;

  @Override
  public void onCreate(Bundle pSavedInstanceState) {
    super.onCreate(pSavedInstanceState);
    if (pSavedInstanceState!=null) {
      final PMParcelable parcelable = pSavedInstanceState.<PMParcelable>getParcelable(KEY_PROCESSMODEL);
      mProcessModel = parcelable==null ? null : parcelable.getProcessModel();
      String s = pSavedInstanceState.getString(KEY_FILE);
      mTmpFile = s==null ? null : new File(s);
      mMenu = pSavedInstanceState.getBoolean(ARG_MENU, true);
    } else if (getArguments()!=null){
      mMenu = getArguments().getBoolean(ARG_MENU, true);
    } else {
      mMenu = true;
    }
    if (mMenu) {
      setHasOptionsMenu(mProvider!=null);
    }
  }



  @Override
  public void onAttach(Activity pActivity) {
    super.onAttach(pActivity);
    Fragment parent = getParentFragment();
    if (this instanceof PMProvider) {
      mProvider = (PMProvider) this;
    } else if (parent!=null && (parent instanceof PMProvider)) {
      mProvider = (PMProvider) parent;
    } else if (pActivity instanceof PMProvider){
      mProvider = (PMProvider) pActivity;
    }
    if (mMenu) {
      setHasOptionsMenu(mProvider!=null);
    }
  }



  @Override
  public void onDetach() {
    if (mTmpFile!=null) {
      mTmpFile.delete();
      mTmpFile=null;
    }
    super.onDetach();
  }



  public void doShareFile(ClientProcessModel<?> pProcessModel) {
    FileStoreTask task = new FileStoreTask(TYPE_FILE,new FileStoreListener("*/*", REQUEST_SHARE_FILE));
    task.execute(pProcessModel);
  }

  public void doSaveFile(ClientProcessModel<?> processModel) {
    mProcessModel = processModel;
    requestSaveFile("*/*", REQUEST_SAVE_FILE);
  }

  private OutputStream getOutputStreamFromSave(Intent pData) throws FileNotFoundException {
    return getActivity().getContentResolver().openOutputStream(pData.getData());
  }

  public void doSaveFile(Intent pData, ClientProcessModel<?> processModel) {
    try {
      OutputStream out = getOutputStreamFromSave(pData);
      try {
        doSaveFile(out, processModel);
      } finally {
        out.close();
      }
    } catch (RuntimeException| IOException e) {
      Log.e(TAG, "Failure to save file", e);
    }
  }

  public void doSaveFile(Writer out, ClientProcessModel<?> processModel) throws IOException {
    try {
      PMParser.serializeProcessModel(out , processModel);
    } catch (XmlPullParserException e) {
      throw new IOException(e);
    }
  }

  public void doSaveFile(OutputStream out, ClientProcessModel<?> processModel) throws IOException {
    try {
      PMParser.serializeProcessModel(out , processModel);
    } catch (XmlPullParserException e) {
      throw new IOException(e);
    }
  }

  public void doShareSVG(ClientProcessModel<?> pProcessModel) {
    FileStoreTask task = new FileStoreTask(TYPE_SVG,new FileStoreListener("image/svg", REQUEST_SHARE_SVG));
    task.execute(pProcessModel);
  }

  public void doExportSVG(ClientProcessModel<?> processModel) {
    mProcessModel = processModel;
    requestSaveFile("image/svg", REQUEST_EXPORT_SVG);
  }

  public void doExportSVG(Intent pData, DrawableProcessModel processModel) {
    try {
      OutputStream out = getOutputStreamFromSave(pData);
      try {
        doExportSVG(out, processModel);
      } finally {
        out.close();
      }
    } catch (RuntimeException| IOException e) {
      Log.e(TAG, "Failure to save file", e);
    }
  }

  public void doExportSVG(OutputStream out, DrawableProcessModel processModel) throws IOException {
    try {
      final XmlSerializer serializer = PMParser.getSerializer(out);
      doExportSVG(serializer, processModel);
    } catch (XmlPullParserException e) {
      throw new IOException(e);
    }
  }

  public void doExportSVG(Writer out, DrawableProcessModel processModel) throws IOException {
    try {
      final XmlSerializer serializer = PMParser.getSerializer(out);
      doExportSVG(serializer, processModel);
    } catch (XmlPullParserException e) {
      throw new IOException(e);
    }
  }

  public void doExportSVG(final XmlSerializer serializer, DrawableProcessModel processModel) throws IOException {
    SVGCanvas<AndroidMeasureInfo> canvas = new SVGCanvas<>(new AndroidTextMeasurer());
    canvas.setBounds(processModel.getBounds());
    processModel.draw(canvas, null);
    serializer.startDocument(null, null);
    serializer.ignorableWhitespace("\n");
    serializer.comment("Generated by PMEditor");
    serializer.ignorableWhitespace("\n");
    canvas.serialize(new PMParser.XmlSerializerAdapter(serializer));
    serializer.ignorableWhitespace("\n");
    serializer.flush();
  }

  private void requestSaveFile(final String type, final int request) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
    if (prefs.getBoolean(SettingsActivity.PREF_KITKATFILE, true) && Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT) {
      startKitkatSaveActivity(type, request);
    } else {
      Intent intent = new Intent("org.openintents.action.PICK_FILE");
      if (supportsIntent(intent)) {
        intent.putExtra("org.openintents.extra.TITLE", getString(R.string.title_saveas));
        intent.putExtra("org.openintents.extra.BUTTON_TEXT", getString(R.string.btn_save));
        intent.setData(Uri.withAppendedPath(Uri.fromFile(Compat.getDocsDirectory()),"/"));
      } else {
        intent = new Intent("com.estrongs.action.PICK_FILE");
        if (supportsIntent(intent)) {
          intent.putExtra("com.estrongs.intent.extra.TITLE", getString(R.string.title_saveas));
//          intent.setData(Uri.withAppendedPath(Uri.fromFile(Compat.getDocsDirectory()),"/"));
        } else {
          doShareFile(mProcessModel);
//          Toast.makeText(getActivity(), "Saving not yet supported without implementation", Toast.LENGTH_LONG).show();
          return;
        }
      }
      startActivityForResult(intent, request);
    }
  }

  private boolean supportsIntent(Intent pIntent) {
    return ! getActivity().getPackageManager().queryIntentActivities(pIntent, 0).isEmpty();
  }

  @TargetApi(Build.VERSION_CODES.KITKAT)
  public void startKitkatSaveActivity(final String type, final int request) {
    Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
    i.setType(type);
    i.addCategory(Intent.CATEGORY_OPENABLE);
    startActivityForResult(i, request);
  }

  public void setPMProvider(PMProvider pmProvider) {
    mProvider = pmProvider;
    if (mMenu) {
      setHasOptionsMenu(mProvider!=null);
    }
  }

  @Override
  public void onActivityResult(int pRequestCode, int pResultCode, Intent pData) {
    if (pRequestCode==REQUEST_SHARE_FILE ||pRequestCode==REQUEST_SHARE_SVG) {
      mTmpFile.delete();
    }
    if (pResultCode==Activity.RESULT_OK) {
      if (pRequestCode==REQUEST_SAVE_FILE) {
        doSaveFile(pData, mProcessModel);
      } else if (pRequestCode == REQUEST_EXPORT_SVG) {
        doExportSVG(pData, DrawableProcessModel.get(mProcessModel));
      }
    }
  }



  @Override
  public void onCreateOptionsMenu(Menu pMenu, MenuInflater pInflater) {
    pInflater.inflate(R.menu.pm_menu, pMenu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem pItem) {
    ClientProcessModel<?> pm = null;
    if ((pItem.getItemId()==R.id.ac_export||pItem.getItemId()==R.id.ac_export_svg)&&
        (mProvider==null|| (pm = mProvider.getProcessModel())==null)) {
      Toast.makeText(getActivity(), "No process model available", Toast.LENGTH_LONG).show();
      return true;
    }
    switch (pItem.getItemId()) {
      case R.id.ac_export:
        doSaveFile(pm);
        return true;
      case R.id.ac_export_svg:
        doExportSVG(pm);
        return true;
      default:
        return super.onOptionsItemSelected(pItem);
    }
  }

  @Override
  public void onSaveInstanceState(Bundle pOutState) {
    if (mProcessModel!=null) pOutState.putParcelable(KEY_PROCESSMODEL, new PMParcelable(mProcessModel));
    if (mTmpFile!=null)  pOutState.putString(KEY_FILE, mTmpFile.toString());
  }

}
