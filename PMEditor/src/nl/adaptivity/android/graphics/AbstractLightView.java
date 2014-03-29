package nl.adaptivity.android.graphics;


import nl.adaptivity.diagram.Drawable;
import nl.adaptivity.diagram.android.LightView;


public abstract class AbstractLightView implements LightView{
  private int aState = 0;

  public AbstractLightView() {
    super();
  }

  protected void setState(int var, boolean val) {
    if(val) {
      aState |= var;
    } else {
      aState &= ~var;
    }
  }

  @Override
  public void setFocussed(boolean pFocussed) {
    setState(Drawable.STATE_FOCUSSED, pFocussed);
    aState |= Drawable.STATE_FOCUSSED;
  }

  @Override
  public boolean isFocussed() {
    return hasState(Drawable.STATE_FOCUSSED);
  }

  protected boolean hasState(final int state) {
    return (aState&state)!=0;
  }

  @Override
  public void setSelected(boolean pSelected) {
    setState(Drawable.STATE_SELECTED, pSelected);
    aState |= Drawable.STATE_SELECTED;
  }

  @Override
  public boolean isSelected() {
    return hasState(Drawable.STATE_SELECTED);
  }

  @Override
  public void setTouched(boolean pTouched) {
    setState(Drawable.STATE_TOUCHED, pTouched);
    aState |= Drawable.STATE_TOUCHED;
  }

  @Override
  public boolean isTouched() {
    return hasState(Drawable.STATE_TOUCHED);
  }

  @Override
  public void setActive(boolean pActive) {
    setState(Drawable.STATE_ACTIVE, pActive);
    aState |= Drawable.STATE_ACTIVE;
  }

  @Override
  public boolean isActive() {
    return hasState(Drawable.STATE_ACTIVE);
  }

}