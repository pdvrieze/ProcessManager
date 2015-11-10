package nl.adaptivity.process.models;

import java.util.Arrays;

import nl.adaptivity.process.editor.android.SettingsActivity;
import nl.adaptivity.sync.DelegatingRemoteXmlSyncAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class ProcessSyncAdapter extends DelegatingRemoteXmlSyncAdapter {

  public ProcessSyncAdapter(Context context) {
    super(context, true, false, Arrays.asList(new ProcessModelSyncAdapter(), new ProcessInstanceSyncAdapter()));
  }

  @Override
  public String getSyncSource() {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
    return prefs.getString(SettingsActivity.PREF_SYNC_SOURCE, "https://darwin.bournemouth.ac.uk/ProcessEngine/");
  }

  protected String getListUrl(String base) {
    return base+"/processModels";
  }


}