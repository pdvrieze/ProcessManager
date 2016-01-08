package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.Theme;
import android.graphics.Canvas;
import android.graphics.RectF;

/** An interface for a lightweight view. */
public interface LightView {

  public void setFocussed(boolean focussed);

  public boolean isFocussed();

  public void setSelected(boolean selected);

  public boolean isSelected();

  public void setTouched(boolean touched);

  public boolean isTouched();

  public void getBounds(RectF target);

  public void draw(Canvas canvas, Theme<AndroidStrategy, AndroidPen, AndroidPath> theme, double scale);

  public void move(float x, float y);

  public void setPos(float left, float top);

  public void setActive(boolean active);

  public boolean isActive();

}
