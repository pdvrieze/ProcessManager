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
import android.support.annotation.IntDef;
import nl.adaptivity.diagram.Theme;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Lightview implementation that wraps another lightview and allows for relative positioning to be specified, as well
 * as direct manipulation of it's position.
 */
public class RelativeLightView implements LightView {

  @IntDef(flag=true, value={DEFAULT, HGRAVITY, LEFT, RIGHT, VGRAVITY, TOP, BOTTOM})
  @Retention(RetentionPolicy.SOURCE)
  public @interface LVGravity{}

  public static final int HGRAVITY=1;
  public static final int LEFT=1<<1;
  public static final int RIGHT=1<<2;
  public static final int HMASK=HGRAVITY|LEFT|RIGHT;
  public static final int VGRAVITY=1<<3;
  public static final int TOP=1<<4;
  public static final int BOTTOM=1<<5;
  public static final int VMASK=VGRAVITY|TOP|BOTTOM;
  public static final int DEFAULT=HGRAVITY|VGRAVITY;

  private final int mRelativePos;

  private final LightView mView;
  private float mLeft = 0.0f;
  private float mTop = 0.0f;

  public RelativeLightView(final LightView view, @LVGravity final int relativePos) {
    mView = view;
    mRelativePos = ((relativePos & HMASK)==0 ? HGRAVITY : relativePos) & ((relativePos & VMASK) == 0 ? VGRAVITY : relativePos);
  }

  @LVGravity
  public int getRelativePos() {
    return mRelativePos;
  }

  @Override
  public void setFocussed(final boolean focussed) {
    mView.setFocussed(focussed);
  }

  @Override
  public boolean isFocussed() {
    return mView.isFocussed();
  }

  @Override
  public void setSelected(final boolean selected) {
    mView.setSelected(selected);
  }

  @Override
  public boolean isSelected() {
    return mView.isSelected();
  }

  @Override
  public void setTouched(final boolean b) {
    mView.setTouched(b);
  }

  @Override
  public boolean isTouched() {
    return mView.isTouched();
  }

  @Override
  public void setActive(final boolean active) {
    mView.setActive(active);
  }

  @Override
  public boolean isActive() {
    return mView.isActive();
  }

  @Override
  public void getBounds(final RectF target) {
    mView.getBounds(target);

    // Adjust the bounds to the ones needed
    target.left+=mLeft;
    target.top+=mTop;
    target.right+=mLeft;
    target.bottom+=mTop;
  }

  @Override
  public void draw(final Canvas canvas, final Theme<AndroidStrategy, AndroidPen, AndroidPath> theme, final double scale) {
    mView.draw(canvas, theme, scale);
  }

  public void setPos(final float left, final float top) {
    RectF bounds = new RectF();
    mView.getBounds(bounds);
    mLeft = left - bounds.left;
    mTop = top - bounds.top;
  }

}
