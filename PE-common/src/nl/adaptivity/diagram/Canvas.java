package nl.adaptivity.diagram;


public interface Canvas<S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> {
  
  public enum TextPos {
    TOPLEFT, TOP, TOPRIGHT,
    LEFT, MIDDLE, RIGHT,
    BASELINELEFT, BASELINEMIDDLE, BASELINERIGHT,
    BOTTOMLEFT, BOTTOM, BOTTOMRIGHT;
  }

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

  void drawPath(PATH_T pPath, PEN_T pStroke, PEN_T pFill);

  public Theme<S, PEN_T, PATH_T> getTheme();

  /**
   * Draw the given text onto the canvas.
   * @param TextPos The position of the text anchor.
   * @param left The left point for drawing the text.
   * @param baselineY The coordinate of the text baseline
   * @param text The text to draw.
   * @param foldWidth The width at which to fold the text.
   * @param pen The pen to use for it all.
   */
  public void drawText(TextPos pTextPos, double left, double baselineY, String text, double foldWidth, PEN_T pen);

}
