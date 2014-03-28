package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.Theme;
import android.graphics.Canvas;
import android.graphics.RectF;

/** An interface for a lightweight view. */
public interface LightView {

  public void setFocussed(boolean pFocussed);

  public boolean isFocussed();

  public void setSelected(boolean pSelected);

  public boolean isSelected();

  public void setTouched(boolean pTouched);

  public boolean isTouched();

  public void getBounds(RectF pTarget);

  public void draw(Canvas pCanvas, Theme<AndroidStrategy, AndroidPen, AndroidPath> pTheme, double pScale);

  public void move(float pX, float pY);

  public void setPos(float pLeft, float pTop);

}
