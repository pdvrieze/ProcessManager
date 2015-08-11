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

  public LWDrawableView(Drawable pItem) {
    aItem = pItem;
  }

  @Override
  public void setFocussed(boolean pFocussed) {
    if (pFocussed) {
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
  public void setSelected(boolean pSelected) {
    if (pSelected) {
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
  public void setTouched(boolean pTouched) {
    if (pTouched) {
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
  public void setActive(boolean pActive) {
    if (pActive) {
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
  public void getBounds(RectF pRect) {
    Rectangle bounds = aItem.getBounds();
    pRect.set(bounds.leftf(), bounds.topf(), bounds.rightf(), bounds.bottomf());
  }


  /**
   * Craw this drawable onto an android canvas. The canvas has an ofset
   * preapplied so the top left of the drawing is 0,0.
   */
  @Override
  public  void draw(Canvas pCanvas, Theme<AndroidStrategy, AndroidPen, AndroidPath> pTheme, double pScale) {
    if (aAndroidCanvas==null) {
      aAndroidCanvas=new AndroidCanvas(pCanvas, pTheme);
    } else {
      aAndroidCanvas.setCanvas(pCanvas);
    }
    aItem.draw(aAndroidCanvas.scale(pScale), null);
  }

  @Override
  public void move(float pX, float pY) {
    aItem.move(pX, pY);
  }

  @Override
  public void setPos(float pLeft, float pTop) {
    aItem.setPos(pLeft, pTop);
  }

}
