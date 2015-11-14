package nl.adaptivity.process.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.editor.android.SettingsActivity;
import nl.adaptivity.sync.DelegatingRemoteXmlSyncAdapter;

import java.net.URI;
import java.util.Arrays;

public class ProcessSyncAdapter extends DelegatingRemoteXmlSyncAdapter {

  public ProcessSyncAdapter(Context context) {
    super(context, true, false, Arrays.asList(new ProcessModelSyncAdapter(), new ProcessInstanceSyncAdapter()));
  }

  @Override
  public URI getSyncSource() {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
    if (prefs.contains(SettingsActivity.PREF_SYNC_SOURCE)) {
      String sync_source = prefs.getString(SettingsActivity.PREF_SYNC_SOURCE, "");
      if (! (sync_source.charAt(sync_source.length()-1)=='/')) {
        sync_source = sync_source+'/';
        prefs.edit().putString(SettingsActivity.PREF_SYNC_SOURCE, sync_source).apply();
      }
      return URI.create(sync_source);
    }
    return URI.create(getContext().getString(R.string.default_sync_location));
  }

  protected String getListUrl(String base) {
    return base+"/processModels";
  }


}