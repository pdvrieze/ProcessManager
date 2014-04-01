package nl.adaptivity.diagram;


public interface Pen<PEN_T extends Pen<PEN_T>> {
  PEN_T setColor(int red, int green, int blue);
  PEN_T setColor(int red, int green, int blue, int alpha);
  PEN_T setStrokeWidth(double strokeWidth);
  PEN_T setFontSize(double fontSize);
  double getFontSize();

  /**
   * Measure the size of the given text. This is the width and height that will
   * be used when actually drawing the text.
   *
   * @param text The text to measure
   * @param foldWidth The width at which to fold the text
   * @param pen The pen that would be be used.
   * @return The width of the text with the given pen
   */
  public double measureTextWidth(String text, double foldWidth);

  public double getTextMaxAscent();

  public double getTextMaxDescent();

  /**
   * The space recommended to separate two lines (beyond ascent and descent.
   * @return The leading
   */
  public double getTextLeading();

  public void setTextItalics(boolean italics);

}
