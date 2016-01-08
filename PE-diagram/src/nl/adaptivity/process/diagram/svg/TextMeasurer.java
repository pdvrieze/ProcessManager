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

package nl.adaptivity.process.diagram.svg;

import nl.adaptivity.process.diagram.svg.TextMeasurer.MeasureInfo;
import org.jetbrains.annotations.NotNull;


public interface TextMeasurer <M extends MeasureInfo> {


  interface MeasureInfo {

    void setFontSize(double fontSize);

  }

  @NotNull M getTextMeasureInfo(@NotNull SVGPen<M> svgPen);

  double measureTextWidth(@NotNull M textMeasureInfo, String text, double foldWidth);

  double getTextMaxAscent(@NotNull M textMeasureInfo);

  double getTextAscent(@NotNull M textMeasureInfo);

  double getTextMaxDescent(@NotNull M textMeasureInfo);

  double getTextDescent(@NotNull M textMeasureInfo);

  double getTextLeading(@NotNull M textMeasureInfo);

}
