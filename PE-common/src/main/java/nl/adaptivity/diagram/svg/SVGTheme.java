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

import nl.adaptivity.diagram.Theme;
import nl.adaptivity.diagram.ThemeItem;
import nl.adaptivity.process.diagram.svg.TextMeasurer.MeasureInfo;


public class SVGTheme<M extends MeasureInfo> implements Theme<SVGStrategy<M>, SVGPen<M>, SVGPath> {

  private SVGStrategy<M> mStrategy;

  public SVGTheme(SVGStrategy<M> strategy) {
    mStrategy = strategy;
  }

  @Override
  public SVGPen<M> getPen(ThemeItem item, int state) {
    int itemState = item.getEffectiveState(state);

    return item.createPen(mStrategy, itemState);
  }

}
