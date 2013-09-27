package nl.adaptivity.diagram;

public interface Canvas {

  Canvas childCanvas(Rectangle area, double pScale);

  Pen newColor(int pR, int pG, int pB, int pA);

  Pen newPen();

  /**
   * Draw a circle filled with the given color.
   *
   * @param pX
   * @param pY
   * @param pRadius
   * @param pColor
   */
  void drawFilledCircle(double pX, double pY, double pRadius, Pen pColor);

  void drawRect(Rectangle pRect, Pen pColor);

  void drawFilledRect(Rectangle pRect, Pen pColor);

  void drawCircle(double pX, double pY, double pRadius, Pen pColor);

  void drawRoundRect(Rectangle pRect, double pRx, double pRy, Pen pColor);

  void drawFilledRoundRect(Rectangle pRect, double pRx, double pRy, Pen pColor);

  void drawPath(double[] pPoints, Pen pColor);

  void drawFilledPath(double[] pPoints, Pen pColor);

}
