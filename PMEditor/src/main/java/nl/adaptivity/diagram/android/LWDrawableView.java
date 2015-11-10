package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.Drawable;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.Theme;
import android.graphics.Canvas;
import android.graphics.RectF;


public class LWDrawableView implements LightView{

  private Drawable aItem;
  /** Cached canvas */
  private AndroidCanvas aAndroidCanvas;

  public LWDrawableView(Drawable item) {
    aItem = item;
  }

  @Override
  public void setFocussed(boolean focussed) {
    if (focussed) {
      aItem.setState(aItem.getState()|Drawable.STATE_FOCUSSED);
    } else {
      aItem.setState(aItem.getState()& ~Drawable.STATE_FOCUSSED);
    }
  }

  @Override
  public boolean isFocussed() {
    return (aItem.getState()&Drawable.STATE_FOCUSSED)!=0;
  }

  @Override
  public void setSelected(boolean selected) {
    if (selected) {
      aItem.setState(aItem.getState()|Drawable.STATE_SELECTED);
    } else {
      aItem.setState(aItem.getState()& ~Drawable.STATE_SELECTED);
    }
  }

  @Override
  public boolean isSelected() {
    return (aItem.getState()&Drawable.STATE_SELECTED)!=0;
  }

  @Override
  public void setTouched(boolean touched) {
    if (touched) {
      aItem.setState(aItem.getState()|Drawable.STATE_TOUCHED);
    } else {
      aItem.setState(aItem.getState()& ~Drawable.STATE_TOUCHED);
    }
  }

  @Override
  public boolean isTouched() {
    return (aItem.getState()&Drawable.STATE_TOUCHED)!=0;
  }

  @Override
  public void setActive(boolean active) {
    if (active) {
      aItem.setState(aItem.getState()|Drawable.STATE_ACTIVE);
    } else {
      aItem.setState(aItem.getState()& ~Drawable.STATE_ACTIVE);
    }
  }

  @Override
  public boolean isActive() {
    return (aItem.getState()&Drawable.STATE_ACTIVE)!=0;
  }

  @Override
  public void getBounds(RectF rect) {
    Rectangle bounds = aItem.getBounds();
    rect.set(bounds.leftf(), bounds.topf(), bounds.rightf(), bounds.bottomf());
  }


  /**
   * Craw this drawable onto an android canvas. The canvas has an ofset
   * preapplied so the top left of the drawing is 0,0.
   */
  @Override
  public  void draw(Canvas canvas, Theme<AndroidStrategy, AndroidPen, AndroidPath> theme, double scale) {
    if (aAndroidCanvas==null) {
      aAndroidCanvas=new AndroidCanvas(canvas, theme);
    } else {
      aAndroidCanvas.setCanvas(canvas);
    }
    aItem.draw(aAndroidCanvas.scale(scale), null);
  }

  @Override
  public void move(float x, float y) {
    aItem.move(x, y);
  }

  @Override
  public void setPos(float left, float top) {
    aItem.setPos(left, top);
  }

}
