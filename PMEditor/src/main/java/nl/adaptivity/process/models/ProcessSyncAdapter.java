package nl.adaptivity.process.models;

import android.content.Context;
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
    return SettingsActivity.getSyncSource(getContext());
  }

  protected String getListUrl(String base) {
    return base+"/processModels";
  }


}