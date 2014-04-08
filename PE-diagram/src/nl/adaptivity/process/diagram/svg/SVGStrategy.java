package nl.adaptivity.process.diagram.svg;

import nl.adaptivity.diagram.DrawingStrategy;


public class SVGStrategy implements DrawingStrategy<SVGStrategy, SVGPen, SVGPath> {

  private TextMeasurer aTextMeasurer;

  public SVGStrategy(TextMeasurer pTextMeasurer) {
    aTextMeasurer = pTextMeasurer;
  }
  
  @Override
  public SVGPen newPen() {
    return new SVGPen(aTextMeasurer);
  }

  @Override
  public SVGPath newPath() {
    return new SVGPath();
  }

}
