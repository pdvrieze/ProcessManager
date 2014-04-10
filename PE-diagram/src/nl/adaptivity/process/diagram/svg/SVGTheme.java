package nl.adaptivity.process.diagram.svg;

import nl.adaptivity.diagram.Theme;
import nl.adaptivity.diagram.ThemeItem;


public class SVGTheme implements Theme<SVGStrategy, SVGPen, SVGPath> {

  private SVGStrategy aStrategy;

  public SVGTheme(SVGStrategy pStrategy) {
    aStrategy = pStrategy;
  }

  @Override
  public SVGPen getPen(ThemeItem pItem, int pState) {
    int itemState = pItem.getEffectiveState(pState);

    return pItem.createPen(aStrategy, itemState);
  }

}
