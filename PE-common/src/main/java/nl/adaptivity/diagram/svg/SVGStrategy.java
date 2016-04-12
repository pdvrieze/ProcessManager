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

package nl.adaptivity.diagram.svg;

import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.diagram.svg.TextMeasurer.MeasureInfo;


public class SVGStrategy<M extends MeasureInfo> implements DrawingStrategy<SVGStrategy<M>, SVGPen<M>, SVGPath> {

  private TextMeasurer<M> mTextMeasurer;

  public SVGStrategy(TextMeasurer<M> textMeasurer) {
    mTextMeasurer = textMeasurer;
  }

  @Override
  public SVGPen<M> newPen() {
    return new SVGPen<>(mTextMeasurer);
  }

  @Override
  public SVGPath newPath() {
    return new SVGPath();
  }

}
