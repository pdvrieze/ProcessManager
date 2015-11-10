package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.Theme;
import android.graphics.Canvas;
import android.graphics.RectF;


public class RelativeLightView implements LightView {

  public static final int HGRAVITY=0;
  public static final int LEFT=1;
  public static final int RIGHT=2;
  public static final int HMASK=HGRAVITY|LEFT|RIGHT;
  public static final int VGRAVITY=0;
  public static final int TOP=4;
  public static final int BOTTOM=8;
  public static final int VMASK=VGRAVITY|TOP|BOTTOM;
  public static final int GRAVITY=HGRAVITY|VGRAVITY;

  private final int aRelativePos;

  private final LightView aView;

  public RelativeLightView(LightView view, int relativePos) {
    aView = view;
    aRelativePos = relativePos;
  }

  public int getRelativePos() {
    return aRelativePos;
  }

  @Override
  public void setFocussed(boolean focussed) {
    aView.setFocussed(focussed);
  }

  @Override
  public boolean isFocussed() {
    return aView.isFocussed();
  }

  @Override
  public void setSelected(boolean selected) {
    aView.setSelected(selected);
  }

  @Override
  public boolean isSelected() {
    return aView.isSelected();
  }

  @Override
  public void setTouched(boolean b) {
    aView.setTouched(b);
  }

  @Override
  public boolean isTouched() {
    return aView.isTouched();
  }

  @Override
  public void setActive(boolean active) {
    aView.setActive(active);
  }

  @Override
  public boolean isActive() {
    return aView.isActive();
  }

  @Override
  public void getBounds(RectF target) {
    aView.getBounds(target);
  }

  @Override
  public void draw(Canvas canvas, Theme<AndroidStrategy, AndroidPen, AndroidPath> theme, double scale) {
    aView.draw(canvas, theme, scale);
  }

  @Override
  public void move(float x, float y) {
    aView.move(x, y);
  }

  @Override
  public void setPos(float left, float top) {
    aView.setPos(left, top);
  }

}
