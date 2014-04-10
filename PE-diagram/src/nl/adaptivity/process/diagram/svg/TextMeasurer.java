package nl.adaptivity.process.diagram.svg;

import nl.adaptivity.process.diagram.svg.TextMeasurer.MeasureInfo;



public interface TextMeasurer <M extends MeasureInfo> {


  public interface MeasureInfo {

    void setFontSize(double pFontSize);

  }

  M getTextMeasureInfo(SVGPen<M> pSvgPen);

  double measureTextWidth(M pTextMeasureInfo, String pText, double pFoldWidth);

  double getTextMaxAscent(M pTextMeasureInfo);

  double getTextMaxDescent(M pTextMeasureInfo);

  double getTextLeading(M pTextMeasureInfo);

}
