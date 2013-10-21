package nl.adaptivity.diagram;

import java.util.Collection;


public interface GroupDrawable extends Drawable {

  public Collection<? extends Drawable> getChildElements();
}
