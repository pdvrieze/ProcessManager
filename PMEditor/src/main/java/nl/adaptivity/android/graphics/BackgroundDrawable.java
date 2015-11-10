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

  private final Drawable aBackground;
  private final Drawable aForeground;
  private final Rect aTmpRect = new Rect();

  public BackgroundDrawable(Context context, int backgroundRes, int foregroundRes) {
    Resources resources = context.getResources();
    Theme theme = context.getTheme();
    aBackground = ResourcesCompat.getDrawable(resources, backgroundRes, theme);
    aForeground = ResourcesCompat.getDrawable(resources, foregroundRes, theme);
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
  protected boolean onStateChange(int[] stateSet) {
    boolean result = aBackground.setState(stateSet);
    if (result) {
      Rect bounds = getBounds();
      aBackground.getCurrent().getPadding(aTmpRect);
      aForeground.setBounds(bounds.left+aTmpRect.left, bounds.top+aTmpRect.top, bounds.right-aTmpRect.right, bounds.bottom-aTmpRect.bottom);
    }
    result|=aForeground.setState(stateSet);
    return result;
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
    aBackground.getCurrent().getPadding(padding);
    return fgIntrinsicWidth+padding.left+padding.right;
  }

  @Override
  public int getIntrinsicHeight() {
    final int fgIntrinsicHeight = aForeground.getIntrinsicHeight();
    if (fgIntrinsicHeight<0) { return -1; }
    Rect padding = aTmpRect ;
    aBackground.getCurrent().getPadding(padding);
    return aForeground.getIntrinsicHeight()+padding.top+padding.bottom;
  }

  @Override
  public int getMinimumWidth() {
    Rect padding = aTmpRect ;
    aBackground.getCurrent().getPadding(padding);
    return aForeground.getMinimumWidth()+padding.left+padding.right;
  }

  @Override
  public int getMinimumHeight() {
    Rect padding = aTmpRect ;
    aBackground.getCurrent().getPadding(padding);
    return aForeground.getMinimumHeight()+padding.top+padding.bottom;
  }

  @Override
  public void setBounds(int left, int top, int right, int bottom) {
    aBackground.setBounds(left, top, right, bottom);
    aBackground.getCurrent().getPadding(aTmpRect);
    aForeground.setBounds(left+aTmpRect.left, top+aTmpRect.top, right-aTmpRect.right, bottom-aTmpRect.bottom);
    super.setBounds(left, top, right, bottom);
  }

  @Override
  public void setBounds(Rect bounds) {
    // This is what the parent does as well.
    setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
  }

}
