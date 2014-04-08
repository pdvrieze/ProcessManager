package nl.adaptivity.process.diagram.svg;

import nl.adaptivity.process.diagram.svg.TextMeasurer.MeasureInfo;


public interface TextMeasurer {

  
  public interface MeasureInfo {

    void setFontSize(double pFontSize);

  }

  void getTextMeasureInfo(SVGPen pSvgPen);

  double measureTextWidth(MeasureInfo pTextMeasureInfo, String pText, double pFoldWidth);

  double getTextMaxAscent(MeasureInfo pTextMeasureInfo);

  double getTextMaxDescent(MeasureInfo pTextMeasureInfo);

  double getTextLeading(MeasureInfo pTextMeasureInfo);

}
