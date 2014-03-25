package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.Drawable;
import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.Theme;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;


public class LWDrawableView implements LightView{

  private Drawable aItem;
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
  public boolean getFocussed() {
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
  public boolean getSelected() {
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
  public boolean getTouched() {
    return (aItem.getState()&Drawable.STATE_TOUCHED)!=0;
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
  public <S extends DrawingStrategy<S, AndroidPen, AndroidPath>> void draw(Canvas pCanvas, Theme<S, AndroidPen, AndroidPath> pTheme, double pScale) {
    if (aAndroidCanvas==null) {
      aAndroidCanvas=new AndroidCanvas(pCanvas, pTheme);
    } else {
      aAndroidCanvas.setCanvas(pCanvas);
    }
//    if ((aItem.getState()&Drawable.STATE_SELECTED)!=0) {
//      Rectangle bounds = aItem.getBounds();
//      Bitmap bitmap = Bitmap.createBitmap((int) Math.ceil(bounds.width*pScale), (int) Math.ceil(bounds.height*pScale), Bitmap.Config.ARGB_8888);
//      AndroidCanvas bitmapCanvas = new AndroidCanvas(new Canvas(bitmap), pTheme);
//      aItem.draw(bitmapCanvas.scale(pScale), null);
//      AndroidPen pen = pTheme.getPen(AndroidExtraThemeItem.BLUR, aItem.getState());
//      aAndroidCanvas.drawBitmap(0f, 0f, bitmap, pen);
//    } else {
    aItem.draw(aAndroidCanvas.scale(pScale), null);
//    }
  }

}
