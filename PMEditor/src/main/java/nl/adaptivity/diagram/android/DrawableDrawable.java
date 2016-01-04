package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.Theme;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;


public class DrawableDrawable extends Drawable implements Cloneable {

  private nl.adaptivity.diagram.Drawable mImage;
  private Theme<AndroidStrategy, AndroidPen, AndroidPath> mTheme;
  private double mScale;
  private boolean mAutoscale;

  public DrawableDrawable(nl.adaptivity.diagram.Drawable image, Theme<AndroidStrategy, AndroidPen, AndroidPath> theme, final boolean autoScale) {
    mTheme = theme;
    mImage = image;
    DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();
    mScale = dm.density*160/96;
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
  public void draw(Canvas canvas) {
    if (mImage!=null) {
      IAndroidCanvas androidCanvas =  new AndroidCanvas(canvas, mTheme);
      if (mAutoscale) { // if autoscaling, also adjust the position to the bounds
        Rectangle bounds = mImage.getBounds();
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
  public void setAlpha(int alpha) {
    throw new UnsupportedOperationException("Setting alpha not supported");
  }

  @Override
  public void setColorFilter(ColorFilter cf) {
    throw new UnsupportedOperationException("Color filters not supported");
  }

  @Override
  public boolean isStateful() {
    return true;
  }

  @Override
  public boolean setState(int[] stateSet) {
    if (mImage!=null) {
      int dState = nl.adaptivity.diagram.Drawable.STATE_DISABLED;
      for(int state:stateSet) {
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
      boolean result = mImage.getState()!=dState;
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
      Rectangle imgBounds = mImage.getBounds();
      mScale= Math.min((right - left) / imgBounds.width, (bottom - top) / imgBounds.height);
    }
  }

  @Override
  public int getIntrinsicWidth() {
    Rectangle bounds = mImage.getBounds();
    return (int) (Math.ceil(bounds.right()*mScale)-Math.floor(bounds.left*mScale));
  }

  @Override
  public int getIntrinsicHeight() {
    Rectangle bounds = mImage.getBounds();
    return (int) (Math.ceil(bounds.bottom()*mScale)-Math.floor(bounds.top*mScale));
  }

  public double getScale() {
    return mScale;
  }

  /**
   * Set the scale to use for drawing. This automatically disables autoscaling.
   * @param scale The new scale.
   */
  public void setScale(double scale) {
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
