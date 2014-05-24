package nl.adaptivity.process.models;

import nl.adaptivity.process.editor.android.SettingsActivity;
import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;
import android.preference.PreferenceManager;


public class ProcessModelSyncAdapter extends AbstractThreadedSyncAdapter {

  public ProcessModelSyncAdapter(Context pContext) {
    super(pContext, true, false);
  }

  @Override
  public void onPerformSync(Account pAccount, Bundle pExtras, String pAuthority, ContentProviderClient pProvider, SyncResult pSyncResult) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
    String base = prefs.getString(SettingsActivity.PREF_SYNC_SOURCE, "https://darwin.bournemouth.ac.uk/ProcessEngine/ProcessModels");


    // TODO Auto-generated method stub
    //
    throw new UnsupportedOperationException("Not yet implemented");
  }

}
