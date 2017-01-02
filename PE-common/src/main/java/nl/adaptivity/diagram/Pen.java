/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.diagram;


public interface Pen<PEN_T extends Pen<PEN_T>> {
  PEN_T setColor(int red, int green, int blue);
  PEN_T setColor(int red, int green, int blue, int alpha);

  PEN_T setStrokeWidth(double strokeWidth);
  double getStrokeWidth();
  
  PEN_T setFontSize(double fontSize);
  double getFontSize();

  /**
   * Measure the full bounding rectangle for the text.
   */
  Rectangle measureTextSize(Rectangle dest, final double x, final double y, final String text, double foldWidth);

  /**
   * Measure the size of the given text. This is the width and height that will
   * be used when actually drawing the text.
   *
   * @param text The text to measure
   * @param foldWidth The width at which to fold the text
   * @return The width of the text with the given pen
   */
  double measureTextWidth(String text, double foldWidth);

  double getTextMaxAscent();

  double getTextAscent();

  double getTextMaxDescent();

  double getTextDescent();

  /**
   * The space recommended to separate two lines (beyond ascent and descent.
   * @return The leading
   */
  double getTextLeading();

  boolean isTextItalics();
  
  void setTextItalics(boolean italics);

  boolean isTextBold();
  
  void setTextBold(boolean bold);

}
