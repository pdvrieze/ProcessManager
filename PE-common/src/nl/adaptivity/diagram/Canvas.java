package nl.adaptivity.diagram;


public interface Canvas<S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> {

  S getStrategy();

  Canvas<S, PEN_T, PATH_T> childCanvas(Rectangle area, double pScale);

  /**
   * Draw a circle filled with the given color.
   *
   * @param pX
   * @param pY
   * @param pRadius
   * @param pColor
   */
  void drawFilledCircle(double pX, double pY, double pRadius, PEN_T pColor);

  void drawRect(Rectangle pRect, PEN_T pColor);

  void drawFilledRect(Rectangle pRect, PEN_T pColor);

  void drawCircle(double pX, double pY, double pRadius, PEN_T pColor);

  void drawRoundRect(Rectangle pRect, double pRx, double pRy, PEN_T pColor);

  void drawFilledRoundRect(Rectangle pRect, double pRx, double pRy, PEN_T pColor);

  /**
   * These are implemented in terms of drawPath, but don't allow for path caching.
   * @param pPoints The points of the poly
   * @param pColor The color
   */
  @Deprecated
  void drawPoly(double[] pPoints, PEN_T pColor);

  @Deprecated
  void drawFilledPoly(double[] pPoints, PEN_T pColor);

  void drawPath(PATH_T pPath, PEN_T pColor);

  void drawFilledPath(PATH_T pPath, PEN_T pColor);

}
