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

package nl.adaptivity.process.ui.activity;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.editor.android.databinding.ActivityUserTaskEditorBinding;
import nl.adaptivity.process.tasks.TaskItem;
import nl.adaptivity.process.ui.UIConstants;
import nl.adaptivity.process.ui.activity.ItemEditDialogFragment.ItemEditDialogListener;


public class UserTaskEditorActivity extends AppCompatActivity implements ItemEditDialogListener {

  private ActivityUserTaskEditorBinding mBinding;
  private UserTaskEditorFragment mEditorFragment;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mBinding = DataBindingUtil.setContentView(this, R.layout.activity_user_task_editor);

    Toolbar toolbar = mBinding.toolbar;
    setSupportActionBar(toolbar);

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    final Intent intent = getIntent();
    mEditorFragment = new UserTaskEditorFragment();
    if (intent.hasExtra(UIConstants.KEY_ACTIVITY)) {
      Bundle bundle = new Bundle(1);
      bundle.putParcelable(UIConstants.KEY_ACTIVITY, intent.getParcelableExtra(UIConstants.KEY_ACTIVITY));
      mEditorFragment.setArguments(bundle);
    }
    getSupportFragmentManager().beginTransaction().add(mBinding.fragment.getId(), mEditorFragment).commit();


  }

  @Override
  public void onPostCreate(final Bundle savedInstanceState, final PersistableBundle persistentState) {
    super.onPostCreate(savedInstanceState, persistentState);
    UserTaskEditorFragment frag = (UserTaskEditorFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);

  }

  @Override
  public boolean onOptionsItemSelected(final MenuItem item) {
    if (item.getItemId()==android.R.id.home) {
      // TODO update the activity result.
      finish(); // Make sure to call finish
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void finish() {
    Intent intent = new Intent();
    intent.putExtra(UIConstants.KEY_ACTIVITY, mEditorFragment.getParcelableResult());
    setResult(RESULT_OK, intent);
    super.finish();
  }

  @Override
  public void updateItem(final int itemNo, final TaskItem newItem) {
    mEditorFragment.updateItem(itemNo, newItem);
  }
}
