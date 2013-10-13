package nl.adaptivity.diagram;


public interface Canvas<PENTYPE extends Pen, PATHTYPE extends DiagramPath> {

  Canvas<PENTYPE, PATHTYPE> childCanvas(Rectangle area, double pScale);

  PENTYPE newColor(int pR, int pG, int pB, int pA);

  PENTYPE newPen();

  /**
   * Draw a circle filled with the given color.
   *
   * @param pX
   * @param pY
   * @param pRadius
   * @param pColor
   */
  void drawFilledCircle(double pX, double pY, double pRadius, PENTYPE pColor);

  void drawRect(Rectangle pRect, PENTYPE pColor);

  void drawFilledRect(Rectangle pRect, PENTYPE pColor);

  void drawCircle(double pX, double pY, double pRadius, PENTYPE pColor);

  void drawRoundRect(Rectangle pRect, double pRx, double pRy, PENTYPE pColor);

  void drawFilledRoundRect(Rectangle pRect, double pRx, double pRy, PENTYPE pColor);

  /**
   * These are implemented in terms of drawPath, but don't allow for path caching.
   * @param pPoints The points of the poly
   * @param pColor The color
   */
  @Deprecated
  void drawPoly(double[] pPoints, PENTYPE pColor);

  @Deprecated
  void drawFilledPoly(double[] pPoints, PENTYPE pColor);

  void drawPath(PATHTYPE pPath, PENTYPE pColor);

  void drawFilledPath(PATHTYPE pPath, PENTYPE pColor);

  /**
   * Method to create a new path instance that can then be used for drawPath and
   * drawFilledPath.
   */
  PATHTYPE newPath();

}
