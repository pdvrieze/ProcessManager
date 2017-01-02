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

package nl.adaptivity.process.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import net.devrieze.util.CollectionUtil;
import nl.adaptivity.android.dialogs.DialogResultListener;
import nl.adaptivity.process.diagram.android.ParcelableActivity;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.editor.android.databinding.ActivityUserTaskEditorBinding;
import nl.adaptivity.process.processModel.XmlDefineType;
import nl.adaptivity.process.tasks.TaskItem;
import nl.adaptivity.process.ui.UIConstants;
import nl.adaptivity.process.ui.activity.ItemEditDialogFragment.ItemEditDialogListener;
import nl.adaptivity.process.util.VariableReference.ResultReference;

import java.util.List;


public class UserTaskEditorActivity extends AppCompatActivity implements ItemEditDialogListener, DialogResultListener {

  private ActivityUserTaskEditorBinding mBinding;
  private UserTaskEditorFragment mEditorFragment;

  public static Intent createIntent(final Context context, final ParcelableActivity activity, final String activityId, final List<? extends ResultReference> variables) {
    final Intent intent = new Intent(context, UserTaskEditorActivity.class);
    intent.putParcelableArrayListExtra(UIConstants.KEY_VARIABLES, CollectionUtil.toArrayList(variables));
    intent.putExtra(UIConstants.KEY_ACTIVITY_ID, activityId);
    intent.putExtra(UIConstants.KEY_ACTIVITY, activity);
    return intent;
  }

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mBinding = DataBindingUtil.setContentView(this, R.layout.activity_user_task_editor);

    final Toolbar toolbar = mBinding.toolbar;
    setSupportActionBar(toolbar);

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    final Intent intent = getIntent();
    mEditorFragment = UserTaskEditorFragment.newInstance(intent.<ParcelableActivity>getParcelableExtra(UIConstants.KEY_ACTIVITY), intent.<ResultReference>getParcelableArrayListExtra(UIConstants.KEY_VARIABLES));
    getSupportFragmentManager().beginTransaction().add(mBinding.fragment.getId(), mEditorFragment).commit();


  }

  @Override
  public void onPostCreate(final Bundle savedInstanceState, final PersistableBundle persistentState) {
    super.onPostCreate(savedInstanceState, persistentState);
    final UserTaskEditorFragment frag = (UserTaskEditorFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);

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
  public void onDialogSuccess(final DialogFragment source, final int id, final Object value) {
    for(final Fragment frag: getSupportFragmentManager().getFragments()) {
      if (frag instanceof DialogResultListener) {
        ((DialogResultListener) frag).onDialogSuccess(source, id, value);
      }
    }
  }

  @Override
  public void onDialogCancelled(final DialogFragment source, final int id) {
    for(final Fragment frag: getSupportFragmentManager().getFragments()) {
      if (frag instanceof DialogResultListener) {
        ((DialogResultListener) frag).onDialogCancelled(source, id);
      }
    }
  }

  @Override
  public void finish() {
    final Intent intent = new Intent();
    intent.putExtra(UIConstants.KEY_ACTIVITY, mEditorFragment.getParcelableResult());
    setResult(RESULT_OK, intent);
    super.finish();
  }

  @Override
  public void updateItem(final int itemNo, final TaskItem newItem) {
    mEditorFragment.updateItem(itemNo, newItem);
  }

  @Override
  public void updateDefine(final XmlDefineType define) {
    mEditorFragment.updateDefine(define);
  }
}
