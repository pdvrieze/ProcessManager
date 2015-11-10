package nl.adaptivity.android.graphics;

import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Typeface;
import nl.adaptivity.android.graphics.AndroidTextMeasurer.AndroidMeasureInfo;
import nl.adaptivity.process.diagram.svg.SVGPen;
import nl.adaptivity.process.diagram.svg.TextMeasurer;


public class AndroidTextMeasurer implements TextMeasurer<AndroidTextMeasurer.AndroidMeasureInfo> {


  public static class AndroidMeasureInfo implements TextMeasurer.MeasureInfo {

    final Paint aPaint;
    final FontMetrics aFontMetrics = new FontMetrics();

    public AndroidMeasureInfo(Paint paint) {
      aPaint = paint;
      aPaint.getFontMetrics(aFontMetrics);
    }

    @Override
    public void setFontSize(double fontSize) {
      aPaint.setTextSize((float) fontSize*FONT_MEASURE_FACTOR);
      aPaint.getFontMetrics(aFontMetrics);
    }

  }

  private static final float FONT_MEASURE_FACTOR = 1f;

  @Override
  public AndroidMeasureInfo getTextMeasureInfo(SVGPen<AndroidMeasureInfo> svgPen) {
    Paint paint = new Paint();
    paint.setTextSize((float) svgPen.getFontSize()*FONT_MEASURE_FACTOR);
    if (svgPen.isTextItalics()) {
      paint.setTypeface(Typeface.create(paint.getTypeface(), Typeface.ITALIC));
    } else {
      paint.setTypeface(Typeface.create(paint.getTypeface(), Typeface.NORMAL));
    }
    return new AndroidMeasureInfo(paint);
  }

  @Override
  public double measureTextWidth(AndroidMeasureInfo textMeasureInfo, String text, double foldWidth) {
    return textMeasureInfo.aPaint.measureText(text)/FONT_MEASURE_FACTOR;
  }

  @Override
  public double getTextMaxAscent(AndroidMeasureInfo textMeasureInfo) {
    return Math.abs(textMeasureInfo.aFontMetrics.top)/FONT_MEASURE_FACTOR;
  }

  @Override
  public double getTextAscent(AndroidMeasureInfo textMeasureInfo) {
    return Math.abs(textMeasureInfo.aFontMetrics.ascent)/FONT_MEASURE_FACTOR;
  }

  @Override
  public double getTextMaxDescent(AndroidMeasureInfo textMeasureInfo) {
    return Math.abs(textMeasureInfo.aFontMetrics.bottom)/FONT_MEASURE_FACTOR;
  }

  @Override
  public double getTextDescent(AndroidMeasureInfo textMeasureInfo) {
    return Math.abs(textMeasureInfo.aFontMetrics.descent)/FONT_MEASURE_FACTOR;
  }

  @Override
  public double getTextLeading(AndroidMeasureInfo textMeasureInfo) {
    return (Math.abs(textMeasureInfo.aFontMetrics.top)+Math.abs(textMeasureInfo.aFontMetrics.bottom)-Math.abs(textMeasureInfo.aFontMetrics.ascent)-Math.abs(textMeasureInfo.aFontMetrics.descent))/FONT_MEASURE_FACTOR;
  }

}
