package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.Theme;
import android.graphics.Canvas;
import android.graphics.RectF;

/** An interface for a lightweight view. */
public interface LightView {
  
  public void setFocussed(boolean pFocussed);
  
  public boolean getFocussed();
  
  public void setSelected(boolean pSelected);
  
  public boolean getSelected();

  public void setTouched(boolean pB);
  
  public boolean getTouched();
  
  public void getBounds(RectF pTarget);
  
  public void draw(Canvas pCanvas, Theme<AndroidStrategy, AndroidPen, AndroidPath> pTheme, double pScale);

  public void move(double pX, double pY);
  
}
