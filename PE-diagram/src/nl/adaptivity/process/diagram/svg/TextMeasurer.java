package nl.adaptivity.process.diagram.svg;

import nl.adaptivity.process.diagram.svg.TextMeasurer.MeasureInfo;
import org.jetbrains.annotations.NotNull;


public interface TextMeasurer <M extends MeasureInfo> {


  interface MeasureInfo {

    void setFontSize(double fontSize);

  }

  @NotNull M getTextMeasureInfo(@NotNull SVGPen<M> svgPen);

  double measureTextWidth(@NotNull M textMeasureInfo, String text, double foldWidth);

  double getTextMaxAscent(@NotNull M textMeasureInfo);

  double getTextAscent(@NotNull M textMeasureInfo);

  double getTextMaxDescent(@NotNull M textMeasureInfo);

  double getTextDescent(@NotNull M textMeasureInfo);

  double getTextLeading(@NotNull M textMeasureInfo);

}
