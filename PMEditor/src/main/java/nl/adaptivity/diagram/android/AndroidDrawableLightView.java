package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.Theme;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;


public class AndroidDrawableLightView implements LightView {

  private Drawable aDrawable;
  private double aScale;
  private float aLeft = 0;
  private float aTop = 0;

  public AndroidDrawableLightView(Drawable drawable, double scale) {
    aDrawable = drawable;
    // Initialise bounds
    aDrawable.setBounds(0, 0, aDrawable.getIntrinsicWidth(), aDrawable.getIntrinsicHeight());
    aScale = scale;
  }

  @Override
  public void setFocussed(boolean focussed) {
    setState(android.R.attr.state_focused, focussed);
  }

  @Override
  public boolean isFocussed() {
    return hasState(android.R.attr.state_focused);
  }

  @Override
  public void setSelected(boolean selected) {
    setState(android.R.attr.state_selected, selected);
  }

  @Override
  public boolean isSelected() {
    return hasState(android.R.attr.state_focused);
  }

  @Override
  public void setTouched(boolean touched) {
    setState(android.R.attr.state_pressed, touched);
  }

  @Override
  public boolean isTouched() {
    return hasState(android.R.attr.state_pressed);
  }

  @Override
  public void setActive(boolean active) {
    setState(android.R.attr.state_active, active);
  }

  @Override
  public boolean isActive() {
    return hasState(android.R.attr.state_active);
  }

  private void setState(final int stateResource, boolean desiredState) {
    final int[] oldState = aDrawable.getState();
    final int statePos = getStatePos(oldState, stateResource);
    if (desiredState) {
      if (statePos<0) {
        int newState[] = new int[oldState.length+1];
        System.arraycopy(oldState, 0, newState, 0, oldState.length);
        newState[oldState.length]=stateResource;
        aDrawable.setState(newState);
      }
    } else {
      if (statePos>=0) {
        int newState[] = new int[oldState.length-1];
        System.arraycopy(oldState, 0, newState, 0, statePos);
        System.arraycopy(oldState, statePos+1, newState, statePos, newState.length-statePos);
        aDrawable.setState(newState);
      }
    }
  }

  private static int getStatePos(final int[] states, final int stateResource) {
    final int len = states.length;
    for(int pos=0; pos<len; ++pos) {
      if (states[pos]== stateResource) {
        return pos;
      }
    }
    return -1;
  }

  private boolean hasState(final int stateResource) {
    return getStatePos(aDrawable.getState(), stateResource)>=0;
  }

  @Override
  public void getBounds(RectF target) {
    target.top = aTop;
    target.left = aLeft;
    target.right = aLeft+ (float) (aDrawable.getIntrinsicWidth()/aScale);
    target.bottom = aTop+ (float) (aDrawable.getIntrinsicHeight()/aScale);
  }

  @Override
  public void setPos(float left, float top) {
    aLeft = left;
    aTop = top;
  }

  @Override
  public void draw(Canvas canvas, Theme<AndroidStrategy, AndroidPen, AndroidPath> theme, double scale) {
    aScale = scale;
    aDrawable.draw(canvas);
  }

  @Override
  public void move(float x, float y) {
    aTop = aTop+y;
    aLeft = aLeft+x;
  }

}
