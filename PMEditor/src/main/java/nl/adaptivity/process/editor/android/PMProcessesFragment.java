package nl.adaptivity.process.editor.android;

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
import nl.adaptivity.android.compat.Compat;
import nl.adaptivity.android.graphics.AndroidTextMeasurer;
import nl.adaptivity.android.graphics.AndroidTextMeasurer.AndroidMeasureInfo;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.diagram.svg.SVGCanvas;
import nl.adaptivity.xml.AndroidXmlWriter;
import nl.adaptivity.xml.XmlException;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.*;


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
    ClientProcessModel<?, ?> getProcessModel();
  }

  private class FileStoreListener {

    private String mMimeType;
    private int mRequestCode;

    public FileStoreListener(String mimeType, int requestCode) {
      mMimeType = mimeType;
      mRequestCode = requestCode;
    }

    void afterSave(File result) {
      mTmpFile = result;
      Intent shareIntent = new Intent(Intent.ACTION_SEND);
      shareIntent.setType(mMimeType);
      shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(result));
      startActivityForResult(shareIntent, mRequestCode);
    }

  }



  public class FileStoreTask extends AsyncTask<ClientProcessModel<?, ?>, Object, File> {
    private File mFile;
    private FileStoreListener mPostSave;
    private int mType;

    public FileStoreTask(int type, FileStoreListener postSave) {
      this(type, postSave, null);
    }

    public FileStoreTask(int type, FileStoreListener postSave, File file) {
      mType = type;
      mPostSave = postSave;
      mFile = file;
    }

    @Override
    protected File doInBackground(ClientProcessModel<?, ?>... params) {
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
            doExportSVG(out, DrawableProcessModel.get(params[0]));
          } else {
            doSaveFile(out, params[0]);
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
    protected void onPostExecute(File result) {
      mPostSave.afterSave(result);
    }

    @Override
    protected void onCancelled(File result) {
      if (mFile!=null) {
        mFile.delete();
      }
    }



  }

  private ClientProcessModel<?, ?> mProcessModel;
  protected File mTmpFile;
  private boolean mMenu;
  private PMProvider mProvider;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState!=null) {
      final PMParcelable parcelable = savedInstanceState.<PMParcelable>getParcelable(KEY_PROCESSMODEL);
      mProcessModel = parcelable==null ? null : parcelable.getProcessModel();
      String s = savedInstanceState.getString(KEY_FILE);
      mTmpFile = s==null ? null : new File(s);
      mMenu = savedInstanceState.getBoolean(ARG_MENU, true);
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
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    Fragment parent = getParentFragment();
    if (this instanceof PMProvider) {
      mProvider = (PMProvider) this;
    } else if (parent!=null && (parent instanceof PMProvider)) {
      mProvider = (PMProvider) parent;
    } else if (activity instanceof PMProvider){
      mProvider = (PMProvider) activity;
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



  public void doShareFile(ClientProcessModel<?, ?> processModel) {
    FileStoreTask task = new FileStoreTask(TYPE_FILE,new FileStoreListener("*/*", REQUEST_SHARE_FILE));
    task.execute(processModel);
  }

  public void doSaveFile(ClientProcessModel<?, ?> processModel) {
    mProcessModel = processModel;
    requestSaveFile("*/*", REQUEST_SAVE_FILE);
  }

  private OutputStream getOutputStreamFromSave(Intent data) throws FileNotFoundException {
    return getActivity().getContentResolver().openOutputStream(data.getData());
  }

  public void doSaveFile(Intent data, ClientProcessModel<?, ?> processModel) {
    try {
      OutputStream out = getOutputStreamFromSave(data);
      try {
        doSaveFile(out, processModel);
      } finally {
        out.close();
      }
    } catch (RuntimeException| IOException e) {
      Log.e(TAG, "Failure to save file", e);
    }
  }

  public void doSaveFile(Writer out, ClientProcessModel<?, ?> processModel) throws IOException {
    try {
      PMParser.serializeProcessModel(out , processModel);
    } catch (XmlPullParserException| XmlException e) {
      throw new IOException(e);
    }
  }

  public void doSaveFile(OutputStream out, ClientProcessModel<?, ?> processModel) throws IOException {
    try {
      PMParser.serializeProcessModel(out , processModel);
    } catch (XmlException | XmlPullParserException e) {
      throw new IOException(e);
    }
  }

  public void doShareSVG(ClientProcessModel<?, ?> processModel) {
    FileStoreTask task = new FileStoreTask(TYPE_SVG,new FileStoreListener("image/svg", REQUEST_SHARE_SVG));
    task.execute(processModel);
  }

  public void doExportSVG(ClientProcessModel<?, ?> processModel) {
    mProcessModel = processModel;
    requestSaveFile("image/svg", REQUEST_EXPORT_SVG);
  }

  public void doExportSVG(Intent data, DrawableProcessModel processModel) {
    try {
      OutputStream out = getOutputStreamFromSave(data);
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
    try {
      canvas.serialize(new AndroidXmlWriter(serializer));
    } catch (XmlException e) {
      throw new IOException(e);
    }
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

  private boolean supportsIntent(Intent intent) {
    return ! getActivity().getPackageManager().queryIntentActivities(intent, 0).isEmpty();
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
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode==REQUEST_SHARE_FILE ||requestCode==REQUEST_SHARE_SVG) {
      mTmpFile.delete();
    }
    if (resultCode==Activity.RESULT_OK) {
      if (requestCode==REQUEST_SAVE_FILE) {
        doSaveFile(data, mProcessModel);
      } else if (requestCode == REQUEST_EXPORT_SVG) {
        doExportSVG(data, DrawableProcessModel.get(mProcessModel));
      }
    }
  }



  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.pm_menu, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    ClientProcessModel<?, ?> pm = null;
    if ((item.getItemId()==R.id.ac_export||item.getItemId()==R.id.ac_export_svg||item.getItemId()==R.id.ac_share_pm||item.getItemId()==R.id.ac_share_pm_svg)&&
        (mProvider==null|| (pm = mProvider.getProcessModel())==null)) {
      Toast.makeText(getActivity(), "No process model available", Toast.LENGTH_LONG).show();
      return true;
    }
    switch (item.getItemId()) {
      case R.id.ac_export:
        doSaveFile(pm);
        return true;
      case R.id.ac_export_svg:
        doExportSVG(pm);
        return true;
      case R.id.ac_share_pm:
        doShareFile(pm);;
        return true;
      case R.id.ac_share_pm_svg:
        doShareSVG(pm);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    if (mProcessModel!=null) outState.putParcelable(KEY_PROCESSMODEL, new PMParcelable(mProcessModel));
    if (mTmpFile!=null)  outState.putString(KEY_FILE, mTmpFile.toString());
  }

}
