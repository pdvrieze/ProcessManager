package nl.adaptivity.diagram;


public interface Canvas<S extends DrawingStrategy<S>> {

  S getStrategy();

  Canvas<S> childCanvas(Rectangle area, double pScale);

  Pen<S> newColor(int pR, int pG, int pB, int pA);

  Pen<S> newPen();

  /**
   * Draw a circle filled with the given color.
   *
   * @param pX
   * @param pY
   * @param pRadius
   * @param pColor
   */
  void drawFilledCircle(double pX, double pY, double pRadius, Pen<S> pColor);

  void drawRect(Rectangle pRect, Pen<S> pColor);

  void drawFilledRect(Rectangle pRect, Pen<S> pColor);

  void drawCircle(double pX, double pY, double pRadius, Pen<S> pColor);

  void drawRoundRect(Rectangle pRect, double pRx, double pRy, Pen<S> pColor);

  void drawFilledRoundRect(Rectangle pRect, double pRx, double pRy, Pen<S> pColor);

  /**
   * These are implemented in terms of drawPath, but don't allow for path caching.
   * @param pPoints The points of the poly
   * @param pColor The color
   */
  @Deprecated
  void drawPoly(double[] pPoints, Pen<S> pColor);

  @Deprecated
  void drawFilledPoly(double[] pPoints, Pen<S> pColor);

  void drawPath(DiagramPath<S> pPath, Pen<S> pColor);

  void drawFilledPath(DiagramPath<S> pPath, Pen<S> pColor);

  /**
   * Method to create a new path instance that can then be used for drawPath and
   * drawFilledPath.
   */
  DiagramPath<S> newPath();

}
