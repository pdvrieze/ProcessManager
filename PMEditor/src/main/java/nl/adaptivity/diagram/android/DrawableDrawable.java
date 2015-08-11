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

  private nl.adaptivity.diagram.Drawable aImage;
  private Theme<AndroidStrategy, AndroidPen, AndroidPath> aTheme;
  private double aScale;

  public DrawableDrawable(nl.adaptivity.diagram.Drawable pImage, Theme<AndroidStrategy, AndroidPen, AndroidPath> pTheme) {
    aTheme = pTheme;
    aImage = pImage;
    DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();
    aScale = dm.density*160/96;
  }

  @SuppressWarnings("CloneDoesntCallSuperClone")
  @Override
  public DrawableDrawable clone() {
    if (getClass()==DrawableDrawable.class) {
      return new DrawableDrawable(aImage.clone(), aTheme);
    }
    throw new RuntimeException(new CloneNotSupportedException());
  }

  @Override
  public void draw(Canvas pCanvas) {
    if (aImage!=null) {
      AndroidCanvas androidCanvas =  new AndroidCanvas(pCanvas, aTheme);
      aImage.draw(androidCanvas.scale(aScale), null);
    }
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSPARENT;
  }

  @Override
  public void setAlpha(int pAlpha) {
    throw new UnsupportedOperationException("Setting alpha not supported");
  }

  @Override
  public void setColorFilter(ColorFilter pCf) {
    throw new UnsupportedOperationException("Color filters not supported");
  }

  @Override
  public boolean isStateful() {
    return true;
  }

  @Override
  public boolean setState(int[] pStateSet) {
    if (aImage!=null) {
      int dState = nl.adaptivity.diagram.Drawable.STATE_DISABLED;
      for(int state:pStateSet) {
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
      boolean result = aImage.getState()!=dState;
      aImage.setState(dState);
      super.setState(pStateSet);
      return result;
    }
    return super.setState(pStateSet);
  }

  @Override
  public int getIntrinsicWidth() {
    Rectangle bounds = aImage.getBounds();
    return (int) (Math.ceil(bounds.right()*aScale)-Math.floor(bounds.left*aScale));
  }

  @Override
  public int getIntrinsicHeight() {
    Rectangle bounds = aImage.getBounds();
    return (int) (Math.ceil(bounds.bottom()*aScale)-Math.floor(bounds.top*aScale));
  }

  public double getScale() {
    return aScale;
  }

  public void setScale(double pScale) {
    aScale = pScale;
  }

}
