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
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.support.v7.widget.helper.ItemTouchHelper.Callback;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import nl.adaptivity.process.diagram.android.ParcelableActivity;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.editor.android.databinding.FragmentUserTaskEditorBinding;
import nl.adaptivity.process.tasks.TaskItem;
import nl.adaptivity.process.tasks.UserTask;
import nl.adaptivity.process.tasks.items.LabelItem;
import nl.adaptivity.process.tasks.items.ListItem;
import nl.adaptivity.process.tasks.items.PasswordItem;
import nl.adaptivity.process.tasks.items.TextItem;
import nl.adaptivity.process.ui.UIConstants;

import java.util.ArrayList;
import java.util.List;


/**
 * A placeholder fragment containing a simple view.
 */
public class UserTaskEditorFragment extends Fragment {

  public static final int ANIMATION_DURATION = 200;
  private FragmentUserTaskEditorBinding mBinding;
  private UserTaskEditAdapter mAdapter;
  private ParcelableActivity<?, ?> mActivity;

  // Object Initialization
  public UserTaskEditorFragment() {
  }
// Object Initialization end

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_user_task_editor, container, false);
    mBinding.setHandler(this);
    final View view = mBinding.getRoot();

    FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        toggleFabMenu();
      }
    });

    mAdapter = new UserTaskEditAdapter();
    mBinding.content.setAdapter(mAdapter);

    if (savedInstanceState!=null && savedInstanceState.containsKey(UIConstants.KEY_ACTIVITY)) {
      mActivity = savedInstanceState.getParcelable(UIConstants.KEY_ACTIVITY);
    } else if (getArguments()!=null && getArguments().containsKey(UIConstants.KEY_ACTIVITY)){
      mActivity = getArguments().getParcelable(UIConstants.KEY_ACTIVITY);
    }
    if (mActivity != null) {
      UserTask userTask = mActivity.getUserTask();
      if (userTask!=null) {
        mAdapter.setItems(userTask.getItems());
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

    ValueAnimator menuAnimator = ValueAnimator.ofFloat(0f, 1f);
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

    ValueAnimator menuAnimator = ValueAnimator.ofFloat(0f, 1f);
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
    String name = null; // TODO use a dialog to ask for a name.
    switch (v.getId()) {
      case R.id.fabMenuLabel:
        mAdapter.addItem(new LabelItem(name, null));
        break;
      case R.id.fabMenuList:
        mAdapter.addItem(new ListItem(name, "list", null, new ArrayList<String>()));
        break;
      case R.id.fabMenuOther:
        break;
      case R.id.fabMenuPassword:
        mAdapter.addItem(new PasswordItem(name, "password", null));
        break;
      case R.id.fabMenuText:
        mAdapter.addItem(new TextItem(name, "text", null, new ArrayList<String>()));
        break;
    }
  }

  public ParcelableActivity getParcelableResult() {
    List<TaskItem> items = mAdapter.getItems();
    if (mActivity.getUserTask()==null) {
      UserTask userTask = new UserTask(null, -1, null, null, items);
      mActivity.setMessage(userTask.asMessage());
    } else {
      mActivity.getUserTask().setItems(items);
    }

    return mActivity;
  }
}
