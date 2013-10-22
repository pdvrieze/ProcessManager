package nl.adaptivity.diagram;


public interface Theme<S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> {

  /**
   * Get a pen that corresponds to the current state for the current theme element.
   * @param pItem The selector for the theme item.
   * @param pState The state of the object.
   *
   * @return The pen.
   */
  PEN_T getPen(ThemeItem pItem, int pState);

}
