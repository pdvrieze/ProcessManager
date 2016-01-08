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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.diagram;


public interface Canvas<S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> {
  
  enum TextPos {
    MAXTOPLEFT, MAXTOP, MAXTOPRIGHT,
    ASCENTLEFT, ASCENT, ASCENTRIGHT,
    LEFT, MIDDLE, RIGHT,
    BASELINELEFT, BASELINEMIDDLE, BASELINERIGHT,
    DESCENTLEFT, DESCENT, DESCENTRIGHT,
    BOTTOMLEFT, BOTTOM, BOTTOMRIGHT
  }

  S getStrategy();

  Canvas<S, PEN_T, PATH_T> childCanvas(Rectangle area, double scale);

  /**
   * Draw a circle filled with the given color.
   *
   * @param x
   * @param y
   * @param radius
   * @param color
   */
  void drawFilledCircle(double x, double y, double radius, PEN_T color);

  void drawRect(Rectangle rect, PEN_T color);

  void drawFilledRect(Rectangle rect, PEN_T color);

  void drawCircle(double x, double y, double radius, PEN_T color);

  void drawRoundRect(Rectangle rect, double rx, double ry, PEN_T color);

  void drawFilledRoundRect(Rectangle rect, double rx, double ry, PEN_T color);

  /**
   * These are implemented in terms of drawPath, but don't allow for path caching.
   * @param points The points of the poly
   * @param color The color
   */
  @Deprecated
  void drawPoly(double[] points, PEN_T color);

  @Deprecated
  void drawFilledPoly(double[] points, PEN_T color);

  void drawPath(PATH_T path, PEN_T stroke, PEN_T fill);

  Theme<S, PEN_T, PATH_T> getTheme();

  /**
   * Draw the given text onto the canvas.
   * @param textPos The position of the text anchor.
   * @param left The left point for drawing the text.
   * @param baselineY The coordinate of the text baseline
   * @param text The text to draw.
   * @param foldWidth The width at which to fold the text.
   * @param pen The pen to use for it all.
   */
  void drawText(TextPos textPos, double left, double baselineY, String text, double foldWidth, PEN_T pen);

}
