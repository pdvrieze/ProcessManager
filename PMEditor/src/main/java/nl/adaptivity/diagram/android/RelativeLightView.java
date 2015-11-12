package nl.adaptivity.diagram.android;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.support.annotation.IntDef;
import nl.adaptivity.diagram.Theme;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


public class RelativeLightView implements LightView {

  @IntDef({DEFAULT, HGRAVITY, LEFT, RIGHT, VGRAVITY, TOP, BOTTOM})
  @Retention(RetentionPolicy.SOURCE)
  public @interface LVGravity{}

  public static final int HGRAVITY=1;
  public static final int LEFT=2;
  public static final int RIGHT=4;
  public static final int HMASK=HGRAVITY|LEFT|RIGHT;
  public static final int VGRAVITY=8;
  public static final int TOP=16;
  public static final int BOTTOM=32;
  public static final int VMASK=VGRAVITY|TOP|BOTTOM;
  public static final int DEFAULT=HGRAVITY|VGRAVITY;

  private final int mRelativePos;

  private final LightView mView;

  public RelativeLightView(LightView view, @LVGravity int relativePos) {
    mView = view;
    mRelativePos = ((relativePos & HMASK)==0 ? HGRAVITY : relativePos) & ((relativePos & VMASK) == 0 ? VGRAVITY : relativePos);
  }

  @LVGravity
  public int getRelativePos() {
    return mRelativePos;
  }

  @Override
  public void setFocussed(boolean focussed) {
    mView.setFocussed(focussed);
  }

  @Override
  public boolean isFocussed() {
    return mView.isFocussed();
  }

  @Override
  public void setSelected(boolean selected) {
    mView.setSelected(selected);
  }

  @Override
  public boolean isSelected() {
    return mView.isSelected();
  }

  @Override
  public void setTouched(boolean b) {
    mView.setTouched(b);
  }

  @Override
  public boolean isTouched() {
    return mView.isTouched();
  }

  @Override
  public void setActive(boolean active) {
    mView.setActive(active);
  }

  @Override
  public boolean isActive() {
    return mView.isActive();
  }

  @Override
  public void getBounds(RectF target) {
    mView.getBounds(target);
  }

  @Override
  public void draw(Canvas canvas, Theme<AndroidStrategy, AndroidPen, AndroidPath> theme, double scale) {
    mView.draw(canvas, theme, scale);
  }

  @Override
  public void move(float x, float y) {
    mView.move(x, y);
  }

  @Override
  public void setPos(float left, float top) {
    mView.setPos(left, top);
  }

}
