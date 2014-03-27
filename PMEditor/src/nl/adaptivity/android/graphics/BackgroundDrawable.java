package nl.adaptivity.android.graphics;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;

/**
 * Drawable that takes two initial drawables. One background, and one content.
 * @author Paul de Vrieze
 *
 */
public class BackgroundDrawable extends Drawable {

  private Drawable aBackground;
  private Drawable aForeground;
  private Rect aTmpRect = new Rect();

  public BackgroundDrawable(Context context, int backgroundRes, int foregroundRes) {
    Resources resources = context.getResources();
    aBackground = resources.getDrawable(backgroundRes);
    aForeground = resources.getDrawable(foregroundRes);
  }

  public BackgroundDrawable(Drawable background, Drawable foreground) {
    aBackground = background;
    aForeground = foreground;
  }

  @Override
  public void draw(Canvas canvas) {
    aBackground.draw(canvas);
    aForeground.draw(canvas);
  }

  @Override
  public void setAlpha(int alpha) {
    aBackground.setAlpha(alpha);
    aForeground.setAlpha(alpha);
  }

  @Override
  public void setColorFilter(ColorFilter cf) {
    aBackground.setColorFilter(cf);
    aForeground.setColorFilter(cf);
  }

  @Override
  public int getOpacity() {
    return resolveOpacity(aBackground.getOpacity(), aBackground.getOpacity());
  }

  @Override
  public boolean isStateful() {
    return aBackground.isStateful()||aForeground.isStateful();
  }

  @Override
  public boolean setState(int[] pStateSet) {
    if (super.setState(pStateSet)) {
      aBackground.setState(pStateSet);
      aForeground.setState(pStateSet);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public Region getTransparentRegion() {
    return aForeground.getTransparentRegion();
  }

  @Override
  public int getIntrinsicWidth() {
    final int fgIntrinsicWidth = aForeground.getIntrinsicWidth();
    if (fgIntrinsicWidth<0) { return -1; }
    Rect padding = aTmpRect ;
    aBackground.getPadding(padding);
    return fgIntrinsicWidth+padding.left+padding.right;
  }

  @Override
  public int getIntrinsicHeight() {
    final int fgIntrinsicHeight = aForeground.getIntrinsicHeight();
    if (fgIntrinsicHeight<0) { return -1; }
    Rect padding = aTmpRect ;
    aBackground.getPadding(padding);
    return aForeground.getIntrinsicHeight()+padding.top+padding.bottom;
  }

  @Override
  public int getMinimumWidth() {
    Rect padding = aTmpRect ;
    aBackground.getPadding(padding);
    return aForeground.getMinimumWidth()+padding.left+padding.right;
  }

  @Override
  public int getMinimumHeight() {
    Rect padding = aTmpRect ;
    aBackground.getPadding(padding);
    return aForeground.getMinimumHeight()+padding.top+padding.bottom;
  }

  @Override
  public void setBounds(int pLeft, int pTop, int pRight, int pBottom) {
    aBackground.setBounds(pLeft, pTop, pRight, pBottom);
    aBackground.getPadding(aTmpRect);
    aForeground.setBounds(pLeft+aTmpRect.left, pTop+aTmpRect.top, pRight-aTmpRect.right, pBottom-aTmpRect.bottom);
    super.setBounds(pLeft, pTop, pRight, pBottom);
  }

  @Override
  public void setBounds(Rect bounds) {
    // This is what the parent does as well.
    setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
  }

}
