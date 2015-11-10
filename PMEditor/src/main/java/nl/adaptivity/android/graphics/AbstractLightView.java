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
  public void setFocussed(boolean focussed) {
    setState(Drawable.STATE_FOCUSSED, focussed);
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
  public void setSelected(boolean selected) {
    setState(Drawable.STATE_SELECTED, selected);
    aState |= Drawable.STATE_SELECTED;
  }

  @Override
  public boolean isSelected() {
    return hasState(Drawable.STATE_SELECTED);
  }

  @Override
  public void setTouched(boolean touched) {
    setState(Drawable.STATE_TOUCHED, touched);
    aState |= Drawable.STATE_TOUCHED;
  }

  @Override
  public boolean isTouched() {
    return hasState(Drawable.STATE_TOUCHED);
  }

  @Override
  public void setActive(boolean active) {
    setState(Drawable.STATE_ACTIVE, active);
    aState |= Drawable.STATE_ACTIVE;
  }

  @Override
  public boolean isActive() {
    return hasState(Drawable.STATE_ACTIVE);
  }

}