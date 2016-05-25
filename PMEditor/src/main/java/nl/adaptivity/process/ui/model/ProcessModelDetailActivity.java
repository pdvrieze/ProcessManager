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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.ui.model;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.widget.Toast;
import nl.adaptivity.android.darwin.AuthenticatedWebClient;
import nl.adaptivity.android.darwin.AuthenticatedWebClientFactory;
import nl.adaptivity.android.util.GetNameDialogFragment;
import nl.adaptivity.android.util.GetNameDialogFragment.GetNameDialogFragmentCallbacks;
import nl.adaptivity.process.ui.ProcessSyncManager;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.models.ProcessModelProvider;
import nl.adaptivity.process.ui.main.OverviewActivity;
import nl.adaptivity.process.ui.main.ProcessBaseActivity;
import nl.adaptivity.process.ui.main.SettingsActivity;
import nl.adaptivity.process.ui.model.ProcessModelDetailFragment.ProcessModelDetailFragmentCallbacks;


/**
 * An activity representing a single ProcessModel detail screen. This activity
 * is only used on handset devices. On tablet-size devices, item details are
 * presented side-by-side with a list of items in a
 * {@link ProcessModelListOuterFragment}.
 * <p>
 * This activity is mostly just a 'shell' activity containing nothing more than
 * a {@link ProcessModelDetailFragment}.
 */
public class ProcessModelDetailActivity extends ProcessBaseActivity implements ProcessModelDetailFragmentCallbacks, GetNameDialogFragmentCallbacks {

  private static final int DLG_MODEL_INSTANCE_NAME = 1;
  private long mModelHandleToInstantiate;
  private ProcessSyncManager mSyncManager;

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_processmodel_detail);

    // Show the Up button in the action bar.
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    // savedInstanceState is non-null when there is fragment state
    // saved from previous configurations of this activity
    // (e.g. when rotating the screen from portrait to landscape).
    // In this case, the fragment will automatically be re-added
    // to its container so we don't need to manually add it.
    // For more information, see the Fragments API guide at:
    //
    // http://developer.android.com/guide/components/fragments.html
    //
    if (savedInstanceState == null) {
      // Create the detail fragment and add it to the activity
      // using a fragment transaction.
      final Bundle arguments = new Bundle();
      arguments.putLong(ProcessModelDetailFragment.ARG_ITEM_ID,
          getIntent().getLongExtra(ProcessModelDetailFragment.ARG_ITEM_ID,-1));
      final ProcessModelDetailFragment fragment = new ProcessModelDetailFragment();
      fragment.setArguments(arguments);
      getSupportFragmentManager().beginTransaction()
          .add(R.id.processmodel_detail_container, fragment)
          .commit();
    }

    requestAccount(SettingsActivity.getAuthBase(this));
  }

  @Override
  public boolean onOptionsItemSelected(final MenuItem item) {
    final int id = item.getItemId();
    if (id == android.R.id.home) {
      // This ID represents the Home or Up button. In the case of this
      // activity, the Up button is shown. Use NavUtils to allow users
      // to navigate up one level in the application structure. For
      // more details, see the Navigation pattern on Android Design:
      //
      // http://developer.android.com/design/patterns/navigation.html#up-vs-back
      //
      NavUtils.navigateUpTo(this, new Intent(this, OverviewActivity.class));
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onProcessModelSelected(final long processModelRowId) {
    if (processModelRowId>=0) {
      final Intent intent = new Intent(this, ProcessModelDetailActivity.class);
      intent.putExtra(ProcessModelDetailFragment.ARG_ITEM_ID, processModelRowId);
      startActivity(intent);
    }
    finish();
  }

  @Override
  public void onInstantiateModel(final long modelHandle, final String suggestedName) {
    mModelHandleToInstantiate = modelHandle;
    GetNameDialogFragment.show(getSupportFragmentManager(), DLG_MODEL_INSTANCE_NAME, "Instance name", "Provide a name for the process instance", this, suggestedName);
  }

  @Override
  public void onNameDialogCompletePositive(final GetNameDialogFragment dialog, final int id, final String name) {
    try {
      ProcessModelProvider.instantiate(this, mModelHandleToInstantiate, name);
    } catch (RemoteException e) {
      Toast.makeText(this, "Unfortunately the process could not be instantiated: "+e.getMessage(), Toast.LENGTH_SHORT).show();
    }
  }

// Property accessors start
  @Override
  public ProcessSyncManager getSyncManager() {
    Account account = getAccount();
    if (account == null) {
      mSyncManager = null;
    } else if (mSyncManager == null) {
      mSyncManager = new ProcessSyncManager(account);
    }
    return mSyncManager;
  }
// Property acccessors end

  @Override
  public void onNameDialogCompleteNegative(final GetNameDialogFragment dialog, final int id) {
    mModelHandleToInstantiate=-1L;
  }
}
