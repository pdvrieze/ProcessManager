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

package nl.adaptivity.android.graphics;

import android.annotation.TargetApi;
import android.content.Context;
import android.support.annotation.IdRes;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RadioButton;

import java.util.ArrayList;


/**
 * Created by pdvrieze on 14/01/16.
 */
public class RadioButtonHelper extends FrameLayout {

  public interface OnCheckedChangeListener {
    void onCheckedChanged(RadioButtonHelper source, int oldCheckedId, int newCheckedId);
  }

  private final OnClickListener mClickListener = new OnClickListener() {
    @Override
    public void onClick(final View v) {
      radioButtonClicked(v);
    }
  };

  @IdRes private int mCheckedId = 0;
  private OnCheckedChangeListener mOnCheckedChangedListener;

  private void radioButtonClicked(final View v) {
    check(v.getId());
  }

  ArrayList<RadioButton> mRadioChildren;

  public RadioButtonHelper(final Context context) {
    super(context);
  }

  public RadioButtonHelper(final Context context, final AttributeSet attrs) {
    super(context, attrs);
  }

  public RadioButtonHelper(final Context context, final AttributeSet attrs, final int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @TargetApi(21)
  public RadioButtonHelper(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  public void check(@IdRes final int id) {
    final int oldCheckedId = mCheckedId;
    if (mRadioChildren!=null) {
      for (final RadioButton b : mRadioChildren) {
        b.setChecked(b.getId()==id);
      }
    }

    mCheckedId = id;
    if (mOnCheckedChangedListener!=null) {
      mOnCheckedChangedListener.onCheckedChanged(this, oldCheckedId, mCheckedId);
    }
  }

  public int getCheckedRadioButtonId() {
    return mCheckedId;
  }

  public void clearCheck() {
    mCheckedId=0;
    if (mRadioChildren!=null) {
      for (final RadioButton button : mRadioChildren) {
        button.setChecked(false);
      }
    }
  }

  public void setOnCheckedChangeListener(final OnCheckedChangeListener listener) {
    mOnCheckedChangedListener = listener;
  }

  @Override
  public void requestLayout() {
    // Just hook into this to find children
    final ArrayList<RadioButton> newRadioChildren = new ArrayList<>();
    final int                    oldChecked       = mCheckedId;
    addRadioChildren(newRadioChildren, this);
    if (mRadioChildren!=null) {
      final int origCount = mRadioChildren.size();
      mRadioChildren.removeAll(newRadioChildren);
      final int commonCount = origCount - mRadioChildren.size();
      for(final RadioButton removedButton:mRadioChildren) {
        removedButton.setOnClickListener(null);
      }
    }
    mRadioChildren = newRadioChildren;

    // Call super last to accomodate changes of checked state.
    super.requestLayout();

    // Only notify entirely last
    if (oldChecked!=mCheckedId && mOnCheckedChangedListener!=null) {
      mOnCheckedChangedListener.onCheckedChanged(this, oldChecked, mCheckedId);
    }
  }

  private void addRadioChildren(final ArrayList<RadioButton> target, final ViewGroup parent) {
    for (int i = 0; i < parent.getChildCount(); i++) {
      final View child = parent.getChildAt(i);
      if (child instanceof RadioButton) {
        final RadioButton radioButton = (RadioButton) child;
        radioButton.setOnClickListener(mClickListener);
        if (mCheckedId==0) {
          if (radioButton.isChecked()) { mCheckedId = radioButton.getId(); }
        } else {
          radioButton.setChecked(radioButton.getId()==mCheckedId);
        }
        target.add(radioButton);
      } else if (child instanceof ViewGroup) {
        addRadioChildren(target, (ViewGroup) child);
      }
    }

  }
}
