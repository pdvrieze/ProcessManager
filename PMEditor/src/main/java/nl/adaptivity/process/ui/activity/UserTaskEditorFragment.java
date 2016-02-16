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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import net.devrieze.util.CollectionUtil;
import net.devrieze.util.StringUtil;
import nl.adaptivity.android.recyclerview.ClickableAdapter;
import nl.adaptivity.android.recyclerview.ClickableAdapter.OnItemClickListener;
import nl.adaptivity.process.diagram.android.ParcelableActivity;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.editor.android.databinding.FragmentUserTaskEditorBinding;
import nl.adaptivity.process.processModel.XmlDefineType;
import nl.adaptivity.process.processModel.XmlResultType;
import nl.adaptivity.process.tasks.EditableUserTask;
import nl.adaptivity.process.tasks.TaskItem;
import nl.adaptivity.process.tasks.items.LabelItem;
import nl.adaptivity.process.tasks.items.ListItem;
import nl.adaptivity.process.tasks.items.PasswordItem;
import nl.adaptivity.process.tasks.items.TextItem;
import nl.adaptivity.process.ui.UIConstants;
import nl.adaptivity.process.ui.activity.UserTaskEditAdapter.ItemViewHolder;
import nl.adaptivity.process.util.ModifySequence;
import nl.adaptivity.process.util.VariableReference.ResultReference;
import nl.adaptivity.util.xml.Namespace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 * A placeholder fragment containing a simple view.
 */
public class UserTaskEditorFragment extends Fragment implements OnItemClickListener<ItemViewHolder> {

  public static final int ANIMATION_DURATION = 200;
  private FragmentUserTaskEditorBinding mBinding;
  private UserTaskEditAdapter mAdapter;
  private ParcelableActivity<?, ?> mActivity;
  /** The list of possible variables to use in here. */
  private List<ResultReference> mVariables;

  // Object Initialization
  public UserTaskEditorFragment() {
  }

  public static UserTaskEditorFragment newInstance(final ParcelableActivity activity, final Collection<? extends ResultReference> variables) {
    final Bundle args = new Bundle(2);
    args.putParcelable(UIConstants.KEY_ACTIVITY, activity);
    args.putParcelableArrayList(UIConstants.KEY_VARIABLES, CollectionUtil.toArrayList(variables));
    final UserTaskEditorFragment fragment = new UserTaskEditorFragment();
    fragment.setArguments(args);
    return fragment;
  }
// Object Initialization end

  @Override
  public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
    mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_user_task_editor, container, false);
    mBinding.setHandler(this);
    final View view = mBinding.getRoot();

    final FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(final View view) {
        toggleFabMenu();
      }
    });

    mAdapter = new UserTaskEditAdapter();
    mBinding.content.setAdapter(mAdapter);
    mAdapter.setOnItemClickListener(this);

    if (savedInstanceState!=null && savedInstanceState.containsKey(UIConstants.KEY_ACTIVITY)) {
      mActivity = savedInstanceState.getParcelable(UIConstants.KEY_ACTIVITY);
    } else if (getArguments()!=null && getArguments().containsKey(UIConstants.KEY_ACTIVITY)){
      mActivity = getArguments().getParcelable(UIConstants.KEY_ACTIVITY);
    }
    mVariables = getArguments().getParcelableArrayList(UIConstants.KEY_VARIABLES);
    if (mActivity != null) {
      final EditableUserTask EditableUserTask = mActivity.getUserTask();
      if (EditableUserTask!=null) {
        mAdapter.setItems(EditableUserTask.getItems());
      }
    }


    return view;
  }

  private void toggleFabMenu() {
    if (mBinding.fabMenu.getVisibility() == View.VISIBLE) {
      hideFabMenu();
    } else {
      showFabMenu();
    }
  }

  private void showFabMenu() {
    mBinding.fabMenu.setVisibility(View.VISIBLE);
    mBinding.fabMenu.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
    final int startHeight = mBinding.fab.getMeasuredHeight();
    final int startWidth = mBinding.fab.getMeasuredWidth();

    final int targetWidth = mBinding.fabMenu.getMeasuredWidth();
    final int targetHeight = mBinding.fabMenu.getMeasuredHeight();
    mBinding.fabMenu.setPivotX(targetWidth - startWidth / 2);
    mBinding.fabMenu.setPivotY(targetHeight);

    final ValueAnimator menuAnimator = ValueAnimator.ofFloat(0f, 1f);
    menuAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
    menuAnimator.addUpdateListener(new AnimatorUpdateListener() {
      boolean oldImage = true;

      @Override
      public void onAnimationUpdate(final ValueAnimator animation) {
        final float animatedFraction = animation.getAnimatedFraction();
        mBinding.fabMenu.setScaleX((startWidth + (targetWidth - startWidth) * animatedFraction) / targetWidth);
        mBinding.fabMenu.setScaleY((startHeight + (targetHeight - startHeight) * animatedFraction) / targetHeight);
        if (oldImage && animatedFraction > 0.5f) {
          mBinding.fab.setImageResource(R.drawable.ic_clear_black_24dp);
          oldImage = false;
        }
      }
    });
    menuAnimator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(final Animator animation) {
        mBinding.fabMenu.setScaleX(1f);
        mBinding.fabMenu.setScaleY(1f);
      }
    });
    menuAnimator.setDuration(ANIMATION_DURATION);
    menuAnimator.start();

  }

  private void hideFabMenu() {
    // TODO animate this

    mBinding.fabMenu.setVisibility(View.VISIBLE);
    mBinding.fabMenu.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
    final int targetHeight = mBinding.fab.getMeasuredHeight();
    final int targetWidth = mBinding.fab.getMeasuredWidth();

    final int startWidth = mBinding.fabMenu.getMeasuredWidth();
    final int startHeight = mBinding.fabMenu.getMeasuredHeight();
    mBinding.fabMenu.setPivotX(startWidth - targetWidth / 2);
    mBinding.fabMenu.setPivotY(startHeight);

    final ValueAnimator menuAnimator = ValueAnimator.ofFloat(0f, 1f);
    menuAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
    menuAnimator.addUpdateListener(new AnimatorUpdateListener() {
      boolean oldImage = true;

      @Override
      public void onAnimationUpdate(final ValueAnimator animation) {
        final float animatedFraction = animation.getAnimatedFraction();
        mBinding.fabMenu.setScaleX((startWidth + (targetWidth - startWidth) * animatedFraction) / startWidth);
        mBinding.fabMenu.setScaleY((startHeight + (targetHeight - startHeight) * animatedFraction) / startHeight);
        if (oldImage && animatedFraction > 0.5f) {
          mBinding.fab.setImageResource(R.drawable.ic_action_new);
          oldImage = false;
        }
      }
    });
    menuAnimator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(final Animator animation) {
        mBinding.fabMenu.setScaleX(1f);
        mBinding.fabMenu.setScaleY(1f);
        mBinding.fabMenu.setVisibility(View.GONE);
      }
    });
    menuAnimator.setDuration(ANIMATION_DURATION);
    menuAnimator.start();


  }

  public void onFabMenuItemClicked(final View v) {
    hideFabMenu();
    final String name = null; // TODO use a dialog to ask for a name.
    switch (v.getId()) {
      case R.id.fabMenuLabel:
        mAdapter.addItem(new LabelItem(name, null));
        ItemEditDialogFragment.newInstance(mAdapter.getItem(mAdapter.getItemCount()-1), mVariables, mActivity.getDefines(), mAdapter.getItemCount() - 1).show(getFragmentManager(), "itemdialog");
        break;
      case R.id.fabMenuList:
        mAdapter.addItem(new ListItem(name, "list", null, new ArrayList<String>()));
        ItemEditDialogFragment.newInstance(mAdapter.getItem(mAdapter.getItemCount()-1), mVariables, mActivity.getDefines(), mAdapter.getItemCount() - 1).show(getFragmentManager(), "itemdialog");
        break;
      case R.id.fabMenuOther:
        break;
      case R.id.fabMenuPassword:
        mAdapter.addItem(new PasswordItem(name, "password", null));
        ItemEditDialogFragment.newInstance(mAdapter.getItem(mAdapter.getItemCount()-1), mVariables, mActivity.getDefines(), mAdapter.getItemCount() - 1).show(getFragmentManager(), "itemdialog");
        break;
      case R.id.fabMenuText:
        mAdapter.addItem(new TextItem(name, "text", null, new ArrayList<String>()));
        ItemEditDialogFragment.newInstance(mAdapter.getItem(mAdapter.getItemCount()-1), mVariables, mActivity.getDefines(), mAdapter.getItemCount() - 1).show(getFragmentManager(), "itemdialog");
        break;
    }
  }

  @Override
  public boolean onClickItem(final ClickableAdapter<? extends ItemViewHolder> adapter, final ItemViewHolder viewHolder) {
    ItemEditDialogFragment.newInstance(mAdapter.getItem(viewHolder.getAdapterPosition()), mVariables, mActivity.getDefines(), viewHolder.getAdapterPosition()).show(getFragmentManager(), "itemdialog");
    return true;
  }

  public void updateItem(final int itemNo, final TaskItem newItem) {
    mAdapter.setItem(itemNo, newItem);
  }


  public void updateDefine(final XmlDefineType define) {
    mActivity.setDefine(define);
  }

  /**
   * From the fragment, retrieve a parcelable activity.
   * @return The parcelable activity that represents the activity state.
   */
  public ParcelableActivity getParcelableResult() {
    final List<TaskItem> items = mAdapter.getContent();
    final EditableUserTask EditableUserTask;
    if (mActivity.getUserTask()==null) {
      EditableUserTask = new EditableUserTask(null, null, null, items);
    } else {
      EditableUserTask = mActivity.getUserTask();
      EditableUserTask.setItems(items);
    }
    for(final TaskItem item: items) {
      if (! (item.isReadOnly() || StringUtil.isNullOrEmpty(item.getName()) || (item.getName() instanceof ModifySequence) )) {
        final XmlResultType result = mActivity.getResult(item.getName().toString());
        if (result==null) {
          final XmlResultType newResult = new XmlResultType(item.getName().toString(), "/values/" + item.getName() + "/text()", (char[]) null, Collections.<Namespace>emptyList());
          mActivity.getResults().add(newResult);
        }
      }
    }
    mActivity.setMessage(EditableUserTask.asMessage());

    return mActivity;
  }
}
