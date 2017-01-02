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

package nl.adaptivity.diagram.android;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import nl.adaptivity.diagram.Theme;


public class AndroidDrawableLightView implements LightView {

  private final Drawable mDrawable;
  private       double   mScale;
  private float mLeft = 0;
  private float mTop = 0;

  public AndroidDrawableLightView(final Drawable drawable, final double scale) {
    mDrawable = drawable;
    // Initialise bounds
    mDrawable.setBounds(0, 0, mDrawable.getIntrinsicWidth(), mDrawable.getIntrinsicHeight());
    mScale = scale;
  }

  @Override
  public void setFocussed(final boolean focussed) {
    setState(android.R.attr.state_focused, focussed);
  }

  @Override
  public boolean isFocussed() {
    return hasState(android.R.attr.state_focused);
  }

  @Override
  public void setSelected(final boolean selected) {
    setState(android.R.attr.state_selected, selected);
  }

  @Override
  public boolean isSelected() {
    return hasState(android.R.attr.state_focused);
  }

  @Override
  public void setTouched(final boolean touched) {
    setState(android.R.attr.state_pressed, touched);
  }

  @Override
  public boolean isTouched() {
    return hasState(android.R.attr.state_pressed);
  }

  @Override
  public void setActive(final boolean active) {
    setState(android.R.attr.state_active, active);
  }

  @Override
  public boolean isActive() {
    return hasState(android.R.attr.state_active);
  }

  private void setState(final int stateResource, final boolean desiredState) {
    final int[] oldState = mDrawable.getState();
    final int statePos = getStatePos(oldState, stateResource);
    if (desiredState) {
      if (statePos<0) {
        final int[] newState = new int[oldState.length + 1];
        System.arraycopy(oldState, 0, newState, 0, oldState.length);
        newState[oldState.length]=stateResource;
        mDrawable.setState(newState);
      }
    } else {
      if (statePos>=0) {
        final int[] newState = new int[oldState.length - 1];
        System.arraycopy(oldState, 0, newState, 0, statePos);
        System.arraycopy(oldState, statePos+1, newState, statePos, newState.length-statePos);
        mDrawable.setState(newState);
      }
    }
  }

  private static int getStatePos(final int[] states, final int stateResource) {
    final int len = states.length;
    for(int pos=0; pos<len; ++pos) {
      if (states[pos]== stateResource) {
        return pos;
      }
    }
    return -1;
  }

  private boolean hasState(final int stateResource) {
    return getStatePos(mDrawable.getState(), stateResource)>=0;
  }

  @Override
  public void getBounds(final RectF target) {
    target.top = mTop;
    target.left = mLeft;
    target.right = mLeft+ (float) (mDrawable.getIntrinsicWidth()/mScale);
    target.bottom = mTop+ (float) (mDrawable.getIntrinsicHeight()/mScale);
  }

  @Override
  public void setPos(final float left, final float top) {
    mLeft = left;
    mTop = top;
  }

  @Override
  public void draw(final Canvas canvas, final Theme<AndroidStrategy, AndroidPen, AndroidPath> theme, final double scale) {
    mScale = scale;
    mDrawable.draw(canvas);
  }

  @Override
  public void move(final float x, final float y) {
    mTop = mTop+y;
    mLeft = mLeft+x;
  }

}
