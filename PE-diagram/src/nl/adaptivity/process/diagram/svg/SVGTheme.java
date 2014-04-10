package nl.adaptivity.process.diagram.svg;

import nl.adaptivity.diagram.Theme;
import nl.adaptivity.diagram.ThemeItem;
import nl.adaptivity.process.diagram.svg.TextMeasurer.MeasureInfo;


public class SVGTheme<M extends MeasureInfo> implements Theme<SVGStrategy<M>, SVGPen<M>, SVGPath> {

  private SVGStrategy<M> aStrategy;

  public SVGTheme(SVGStrategy<M> pStrategy) {
    aStrategy = pStrategy;
  }

  @Override
  public SVGPen<M> getPen(ThemeItem pItem, int pState) {
    int itemState = pItem.getEffectiveState(pState);

    return pItem.createPen(aStrategy, itemState);
  }

}
