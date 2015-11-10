package nl.adaptivity.process.diagram.svg;

import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.process.diagram.svg.TextMeasurer.MeasureInfo;


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
