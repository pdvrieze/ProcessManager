package nl.adaptivity.diagram;


public interface ThemeItem {
  /**
   * Get the number for this theme item. Multiple items should never return the same
   * number. Conciseness will help though.
   * @return The item ordinal.
   */
  int getItemNo();

  /**
   * Get the state that needs to be used for drawing the item at the given state. This allows
   * for optimization in caching.
   * @param The state needed.
   * @return The effective state.
   */
  int getEffectiveState(int state);

  <PEN_T extends Pen<PEN_T>> PEN_T createPen(DrawingStrategy<?, PEN_T, ?> strategy, int state);
}
