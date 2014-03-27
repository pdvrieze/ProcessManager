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
  
  public RelativeLightView(LightView pView, int pRelativePos) {
    aView = pView;
    aRelativePos = pRelativePos;
  }
  
  public int getRelativePos() {
    return aRelativePos;
  }

  @Override
  public void setFocussed(boolean pFocussed) {
    aView.setFocussed(pFocussed);
  }

  @Override
  public boolean isFocussed() {
    return aView.isFocussed();
  }

  @Override
  public void setSelected(boolean pSelected) {
    aView.setSelected(pSelected);
  }

  @Override
  public boolean isSelected() {
    return aView.isSelected();
  }

  @Override
  public void setTouched(boolean pB) {
    aView.setTouched(pB);
  }

  @Override
  public boolean isTouched() {
    return aView.isTouched();
  }

  @Override
  public void getBounds(RectF pTarget) {
    aView.getBounds(pTarget);
  }

  @Override
  public void draw(Canvas pCanvas, Theme<AndroidStrategy, AndroidPen, AndroidPath> pTheme, double pScale) {
    aView.draw(pCanvas, pTheme, pScale);
  }

  @Override
  public void move(float pX, float pY) {
    aView.move(pX, pY);
  }

  @Override
  public void setPos(float pLeft, float pTop) {
    aView.setPos(pLeft, pTop);
  }

}
