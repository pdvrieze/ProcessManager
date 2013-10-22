package nl.adaptivity.diagram;

import java.util.Collection;


public interface Diagram extends GroupDrawable {
  /**
   * Set the given items to be highlighted. This will unHighlight all other items.
   */
  void setHighlighted(Collection<Drawable> pItems);

  Collection<? extends Drawable> getHighlighted();
}
