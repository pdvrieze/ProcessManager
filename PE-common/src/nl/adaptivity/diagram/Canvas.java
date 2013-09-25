package nl.adaptivity.diagram;

public interface Canvas {

  Canvas childCanvas(Rectangle area, double pScale);

  Color newColor(int pR, int pG, int pB, int pA);

  /**
   * Draw a circle filled with the given color.
   *
   * @param pX
   * @param pY
   * @param pRadius
   * @param pColor
   */
  void drawFilledCircle(double pX, double pY, double pRadius, Color pColor);

  void drawRect(Rectangle pRect, Color pColor);

  void drawFilledRect(Rectangle pRect, Color pColor);

  void drawCircle(double pX, double pY, double pRadius, Color pColor);

  void drawRoundRect(Rectangle pRect, double pRx, double pRy, Color pColor);

  void drawFilledRoundRect(Rectangle pRect, double pRx, double pRy, Color pColor);

  void drawPath(double[] pPoints, Color pColor);

  void drawFilledPath(double[] pPoints, Color pColor);

}
