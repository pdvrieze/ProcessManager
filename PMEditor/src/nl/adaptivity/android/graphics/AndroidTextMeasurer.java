package nl.adaptivity.android.graphics;

import nl.adaptivity.android.graphics.AndroidTextMeasurer.AndroidMeasureInfo;
import nl.adaptivity.process.diagram.svg.SVGPen;
import nl.adaptivity.process.diagram.svg.TextMeasurer;
import nl.adaptivity.process.diagram.svg.TextMeasurer.MeasureInfo;
import android.graphics.Paint;
import android.graphics.Typeface;


public class AndroidTextMeasurer implements TextMeasurer<AndroidMeasureInfo> {


  public static class AndroidMeasureInfo implements MeasureInfo {

    final Paint aPaint;

    public AndroidMeasureInfo(Paint pPaint) {
      aPaint = pPaint;
    }

    @Override
    public void setFontSize(double pFontSize) {
      aPaint.setTextSize((float) pFontSize*FONT_MEASURE_FACTOR);
    }

  }

  private static final float FONT_MEASURE_FACTOR = 4f;

  @Override
  public AndroidMeasureInfo getTextMeasureInfo(SVGPen<AndroidMeasureInfo> pSvgPen) {
    Paint paint = new Paint();
    paint.setTextSize((float) pSvgPen.getFontSize()*FONT_MEASURE_FACTOR);
    if (pSvgPen.isTextItalics()) {
      paint.setTypeface(Typeface.create(paint.getTypeface(), Typeface.ITALIC));
    } else {
      paint.setTypeface(Typeface.create(paint.getTypeface(), Typeface.NORMAL));
    }
    return new AndroidMeasureInfo(paint);
  }

  @Override
  public double measureTextWidth(AndroidMeasureInfo pTextMeasureInfo, String pText, double pFoldWidth) {
    return pTextMeasureInfo.aPaint.measureText(pText)/FONT_MEASURE_FACTOR;
  }

  @Override
  public double getTextMaxAscent(AndroidMeasureInfo pTextMeasureInfo) {
    return pTextMeasureInfo.aPaint.ascent();
  }

  @Override
  public double getTextMaxDescent(AndroidMeasureInfo pTextMeasureInfo) {
    return pTextMeasureInfo.aPaint.descent();
  }

  @Override
  public double getTextLeading(AndroidMeasureInfo pTextMeasureInfo) {
    return pTextMeasureInfo.aPaint.getFontSpacing();
  }

}
