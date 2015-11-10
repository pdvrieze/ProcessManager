package nl.adaptivity.process.diagram.svg;

import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.process.diagram.svg.TextMeasurer.MeasureInfo;


public class SVGStrategy<M extends MeasureInfo> implements DrawingStrategy<SVGStrategy<M>, SVGPen<M>, SVGPath> {

  private TextMeasurer<M> aTextMeasurer;

  public SVGStrategy(TextMeasurer<M> textMeasurer) {
    aTextMeasurer = textMeasurer;
  }

  @Override
  public SVGPen<M> newPen() {
    return new SVGPen<>(aTextMeasurer);
  }

  @Override
  public SVGPath newPath() {
    return new SVGPath();
  }

}
