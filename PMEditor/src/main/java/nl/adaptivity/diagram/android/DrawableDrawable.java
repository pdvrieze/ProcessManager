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

package nl.adaptivity.diagram.android;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.Theme;


public class DrawableDrawable extends Drawable implements Cloneable {

  public static final float DEFAULT_SCALE = Resources.getSystem().getDisplayMetrics().density * 160 / 96;
  private final nl.adaptivity.diagram.Drawable                  mImage;
  private final Theme<AndroidStrategy, AndroidPen, AndroidPath> mTheme;
  private       double                                          mScale;
  private       boolean                                         mAutoscale;

  public DrawableDrawable(final nl.adaptivity.diagram.Drawable image, final Theme<AndroidStrategy, AndroidPen, AndroidPath> theme, final boolean autoScale) {
    mTheme = theme;
    mImage = image;
    mScale = DEFAULT_SCALE;
    mAutoscale = autoScale;
  }

  @SuppressWarnings("CloneDoesntCallSuperClone")
  @Override
  public DrawableDrawable clone() {
    if (getClass()==DrawableDrawable.class) {
      return new DrawableDrawable(mImage.clone(), mTheme, mAutoscale);
    }
    throw new RuntimeException(new CloneNotSupportedException());
  }

  @Override
  public void draw(final Canvas canvas) {
    if (mImage!=null) {
      IAndroidCanvas androidCanvas =  new AndroidCanvas(canvas, mTheme);
      if (mAutoscale) { // if autoscaling, also adjust the position to the bounds
        final Rectangle bounds = mImage.getBounds();
        androidCanvas=androidCanvas.translate(-bounds.left, -bounds.top);
      }
      mImage.draw(androidCanvas.scale(mScale), null);
    }
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSPARENT;
  }

  @Override
  public void setAlpha(final int alpha) {
    throw new UnsupportedOperationException("Setting alpha not supported");
  }

  @Override
  public void setColorFilter(final ColorFilter cf) {
    throw new UnsupportedOperationException("Color filters not supported");
  }

  @Override
  public boolean isStateful() {
    return true;
  }

  @Override
  public boolean setState(final int[] stateSet) {
    if (mImage!=null) {
      int dState = nl.adaptivity.diagram.Drawable.STATE_DISABLED;
      for(final int state:stateSet) {
        switch (state) {
        case android.R.attr.state_enabled:
          dState &= ~nl.adaptivity.diagram.Drawable.STATE_DISABLED;
          break;
        case android.R.attr.state_focused:
          dState |= nl.adaptivity.diagram.Drawable.STATE_FOCUSSED;
          break;
        case android.R.attr.state_pressed:
          dState |= nl.adaptivity.diagram.Drawable.STATE_TOUCHED;
          break;
        case android.R.attr.state_selected:
          dState |= nl.adaptivity.diagram.Drawable.STATE_SELECTED;
          break;
        }
      }
      final boolean result = mImage.getState() != dState;
      mImage.setState(dState);
      super.setState(stateSet);
      return result;
    }
    return super.setState(stateSet);
  }

  @Override
  public void setBounds(final int left, final int top, final int right, final int bottom) {
    super.setBounds(left, top, right, bottom);
    if (mAutoscale) {
      final Rectangle imgBounds = mImage.getBounds();
      mScale= Math.min((right - left) / imgBounds.width, (bottom - top) / imgBounds.height);
    }
  }

  @Override
  public int getIntrinsicWidth() {
    final Rectangle bounds = mImage.getBounds();
    final double scale = mAutoscale ? DEFAULT_SCALE : this.mScale;
    return (int) (Math.ceil(bounds.right()*scale)-Math.floor(bounds.left*scale));
  }

  @Override
  public int getIntrinsicHeight() {
    final Rectangle bounds = mImage.getBounds();
    final double scale = mAutoscale ? DEFAULT_SCALE : this.mScale;
    return (int) (Math.ceil(bounds.bottom()*scale)-Math.floor(bounds.top*scale));
  }

  public double getScale() {
    return mScale;
  }

  /**
   * Set the scale to use for drawing. This automatically disables autoscaling.
   * @param scale The new scale.
   */
  public void setScale(final double scale) {
    mScale = scale;
    mAutoscale = false;
  }

  public boolean isAutoscale() {
    return mAutoscale;
  }

  public void setAutoscale(final boolean autoscale) {
    mAutoscale = autoscale;
  }
}
