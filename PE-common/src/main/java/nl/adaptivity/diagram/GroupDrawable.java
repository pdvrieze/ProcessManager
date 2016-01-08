package nl.adaptivity.diagram;

import java.util.Collection;


public interface GroupDrawable extends Drawable {

  Collection<? extends Drawable> getChildElements();
}
