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
