package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.DrawingStrategy;
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
  
  public void getBounds(RectF pTmpRectF);
  
  public <S extends DrawingStrategy<S, AndroidPen, AndroidPath>> void draw(Canvas pCanvas, Theme<S, AndroidPen, AndroidPath> pTheme, double pScale);
  
}
