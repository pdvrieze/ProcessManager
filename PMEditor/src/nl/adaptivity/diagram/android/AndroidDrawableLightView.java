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

  public AndroidDrawableLightView(Drawable pDrawable, double pScale) {
    aDrawable = pDrawable;
    // Initialise bounds
    aDrawable.setBounds(0, 0, aDrawable.getIntrinsicWidth(), aDrawable.getIntrinsicHeight());
    aScale = pScale;
  }

  @Override
  public void setFocussed(boolean pFocussed) {
    setState(android.R.attr.state_focused, pFocussed);
  }

  @Override
  public boolean isFocussed() {
    return hasState(android.R.attr.state_focused);
  }

  @Override
  public void setSelected(boolean pSelected) {
    setState(android.R.attr.state_selected, pSelected);
  }

  @Override
  public boolean isSelected() {
    return hasState(android.R.attr.state_focused);
  }

  @Override
  public void setTouched(boolean pTouched) {
    setState(android.R.attr.state_pressed, pTouched);
  }

  @Override
  public boolean isTouched() {
    return hasState(android.R.attr.state_pressed);
  }

  private void setState(final int pStateResource, boolean pDesiredState) {
    final int[] oldState = aDrawable.getState();
    final int statePos = getStatePos(oldState, pStateResource);
    if (pDesiredState) {
      if (statePos<0) {
        int newState[] = new int[oldState.length+1];
        System.arraycopy(oldState, 0, newState, 0, oldState.length);
        newState[oldState.length]=pStateResource;
        aDrawable.setState(newState);
      }
    } else {
      if (statePos>=0) {
        int newState[] = new int[oldState.length-1];
        System.arraycopy(oldState, 0, newState, 0, statePos);
        System.arraycopy(oldState, statePos+1, newState, statePos, newState.length-statePos);
      }
    }
  }

  private static int getStatePos(final int[] pStates, final int pStateResource) {
    for(int pos:pStates) {
      if (pos== pStateResource) {
        return pos;
      }
    }
    return -1;
  }
  
  private boolean hasState(final int pStateResource) {
    return getStatePos(aDrawable.getState(), pStateResource)>=0;
  }
  
  @Override
  public void getBounds(RectF pTarget) {
    pTarget.top = aTop;
    pTarget.left = aLeft;
    pTarget.right = aLeft+ (float) (aDrawable.getIntrinsicWidth()/aScale);
    pTarget.bottom = aTop+ (float) (aDrawable.getIntrinsicHeight()/aScale);
  }
  
  @Override
  public void setPos(float pLeft, float pTop) {
    aLeft = pLeft;
    aTop = pTop;
  }

  @Override
  public void draw(Canvas pCanvas, Theme<AndroidStrategy, AndroidPen, AndroidPath> pTheme, double pScale) {
    aScale = pScale;
    aDrawable.draw(pCanvas);
  }

  @Override
  public void move(float pX, float pY) {
    aTop = aTop+pY;
    aLeft = aLeft+pX;
  }

}
