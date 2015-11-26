package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.Drawable;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.Theme;
import android.graphics.Canvas;
import android.graphics.RectF;


public class LWDrawableView implements LightView{

  private Drawable mItem;
  /** Cached canvas */
  private AndroidCanvas mAndroidCanvas;

  public LWDrawableView(Drawable item) {
    mItem = item;
  }

  @Override
  public void setFocussed(boolean focussed) {
    if (focussed) {
      mItem.setState(mItem.getState()|Drawable.STATE_FOCUSSED);
    } else {
      mItem.setState(mItem.getState()& ~Drawable.STATE_FOCUSSED);
    }
  }

  @Override
  public boolean isFocussed() {
    return (mItem.getState()&Drawable.STATE_FOCUSSED)!=0;
  }

  @Override
  public void setSelected(boolean selected) {
    if (selected) {
      mItem.setState(mItem.getState()|Drawable.STATE_SELECTED);
    } else {
      mItem.setState(mItem.getState()& ~Drawable.STATE_SELECTED);
    }
  }

  @Override
  public boolean isSelected() {
    return (mItem.getState()&Drawable.STATE_SELECTED)!=0;
  }

  @Override
  public void setTouched(boolean touched) {
    if (touched) {
      mItem.setState(mItem.getState()|Drawable.STATE_TOUCHED);
    } else {
      mItem.setState(mItem.getState()& ~Drawable.STATE_TOUCHED);
    }
  }

  @Override
  public boolean isTouched() {
    return (mItem.getState()&Drawable.STATE_TOUCHED)!=0;
  }

  @Override
  public void setActive(boolean active) {
    if (active) {
      mItem.setState(mItem.getState()|Drawable.STATE_ACTIVE);
    } else {
      mItem.setState(mItem.getState()& ~Drawable.STATE_ACTIVE);
    }
  }

  @Override
  public boolean isActive() {
    return (mItem.getState()&Drawable.STATE_ACTIVE)!=0;
  }

  @Override
  public void getBounds(RectF rect) {
    Rectangle bounds = mItem.getBounds();
    rect.set(bounds.leftf(), bounds.topf(), bounds.rightf(), bounds.bottomf());
  }


  /**
   * Craw this drawable onto an android canvas. The canvas has an ofset
   * preapplied so the top left of the drawing is 0,0.
   */
  @Override
  public  void draw(Canvas canvas, Theme<AndroidStrategy, AndroidPen, AndroidPath> theme, double scale) {
    if (mAndroidCanvas==null) {
      mAndroidCanvas=new AndroidCanvas(canvas, theme);
    } else {
      mAndroidCanvas.setCanvas(canvas);
    }
    mItem.draw(mAndroidCanvas.scale(scale), null);
  }

  @Override
  public void move(float x, float y) {
    mItem.translate(x, y);
  }

  @Override
  public void setPos(float left, float top) {
    mItem.setPos(left, top);
  }

}
