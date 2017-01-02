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

package nl.adaptivity.android.graphics;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;


/**
 * Drawable that takes two initial drawables. One background, and one content.
 * @author Paul de Vrieze
 *
 */
public class BackgroundDrawable extends Drawable {

  private final Drawable mBackground;
  private final Drawable mForeground;
  private final Rect mTmpRect = new Rect();

  public BackgroundDrawable(final Context context, final int backgroundRes, final int foregroundRes) {
    final Resources resources = context.getResources();
    final Theme     theme     = context.getTheme();
    mBackground = ResourcesCompat.getDrawable(resources, backgroundRes, theme);
    mForeground = ResourcesCompat.getDrawable(resources, foregroundRes, theme);
  }

  public BackgroundDrawable(final Drawable background, final Drawable foreground) {
    mBackground = background;
    mForeground = foreground;
  }

  @Override
  public void draw(final Canvas canvas) {
    mBackground.draw(canvas);
    mForeground.draw(canvas);
  }

  @Override
  public void setAlpha(final int alpha) {
    mBackground.setAlpha(alpha);
    mForeground.setAlpha(alpha);
  }

  @Override
  public void setColorFilter(final ColorFilter cf) {
    mBackground.setColorFilter(cf);
    mForeground.setColorFilter(cf);
  }

  @Override
  public int getOpacity() {
    return resolveOpacity(mBackground.getOpacity(), mBackground.getOpacity());
  }

  @Override
  public boolean isStateful() {
    return mBackground.isStateful()||mForeground.isStateful();
  }

  @Override
  protected boolean onStateChange(final int[] stateSet) {
    boolean result = mBackground.setState(stateSet);
    if (result) {
      final Rect bounds = getBounds();
      mBackground.getCurrent().getPadding(mTmpRect);
      mForeground.setBounds(bounds.left+mTmpRect.left, bounds.top+mTmpRect.top, bounds.right-mTmpRect.right, bounds.bottom-mTmpRect.bottom);
    }
    result|=mForeground.setState(stateSet);
    return result;
  }

  @Override
  public Region getTransparentRegion() {
    return mForeground.getTransparentRegion();
  }

  @Override
  public int getIntrinsicWidth() {
    final int fgIntrinsicWidth = mForeground.getIntrinsicWidth();
    if (fgIntrinsicWidth<0) { return -1; }
    final Rect padding = mTmpRect ;
    mBackground.getCurrent().getPadding(padding);
    return fgIntrinsicWidth+padding.left+padding.right;
  }

  @Override
  public int getIntrinsicHeight() {
    final int fgIntrinsicHeight = mForeground.getIntrinsicHeight();
    if (fgIntrinsicHeight<0) { return -1; }
    final Rect padding = mTmpRect ;
    mBackground.getCurrent().getPadding(padding);
    return mForeground.getIntrinsicHeight()+padding.top+padding.bottom;
  }

  @Override
  public int getMinimumWidth() {
    final Rect padding = mTmpRect ;
    mBackground.getCurrent().getPadding(padding);
    return mForeground.getMinimumWidth()+padding.left+padding.right;
  }

  @Override
  public int getMinimumHeight() {
    final Rect padding = mTmpRect ;
    mBackground.getCurrent().getPadding(padding);
    return mForeground.getMinimumHeight()+padding.top+padding.bottom;
  }

  @Override
  public void setBounds(final int left, final int top, final int right, final int bottom) {
    mBackground.setBounds(left, top, right, bottom);
    mBackground.getCurrent().getPadding(mTmpRect);
    mForeground.setBounds(left+mTmpRect.left, top+mTmpRect.top, right-mTmpRect.right, bottom-mTmpRect.bottom);
    super.setBounds(left, top, right, bottom);
  }

  @Override
  public void setBounds(final Rect bounds) {
    // This is what the parent does as well.
    setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
  }

}
