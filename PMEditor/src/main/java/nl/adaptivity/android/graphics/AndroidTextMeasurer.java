package nl.adaptivity.android.graphics;

import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Typeface;
import nl.adaptivity.android.graphics.AndroidTextMeasurer.AndroidMeasureInfo;
import nl.adaptivity.process.diagram.svg.SVGPen;
import nl.adaptivity.process.diagram.svg.TextMeasurer;


public class AndroidTextMeasurer implements TextMeasurer<AndroidTextMeasurer.AndroidMeasureInfo> {


  public static class AndroidMeasureInfo implements TextMeasurer.MeasureInfo {

    final Paint mPaint;
    final FontMetrics mFontMetrics = new FontMetrics();

    public AndroidMeasureInfo(Paint paint) {
      mPaint = paint;
      mPaint.getFontMetrics(mFontMetrics);
    }

    @Override
    public void setFontSize(double fontSize) {
      mPaint.setTextSize((float) fontSize*FONT_MEASURE_FACTOR);
      mPaint.getFontMetrics(mFontMetrics);
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
    return textMeasureInfo.mPaint.measureText(text)/FONT_MEASURE_FACTOR;
  }

  @Override
  public double getTextMaxAscent(AndroidMeasureInfo textMeasureInfo) {
    return Math.abs(textMeasureInfo.mFontMetrics.top)/FONT_MEASURE_FACTOR;
  }

  @Override
  public double getTextAscent(AndroidMeasureInfo textMeasureInfo) {
    return Math.abs(textMeasureInfo.mFontMetrics.ascent)/FONT_MEASURE_FACTOR;
  }

  @Override
  public double getTextMaxDescent(AndroidMeasureInfo textMeasureInfo) {
    return Math.abs(textMeasureInfo.mFontMetrics.bottom)/FONT_MEASURE_FACTOR;
  }

  @Override
  public double getTextDescent(AndroidMeasureInfo textMeasureInfo) {
    return Math.abs(textMeasureInfo.mFontMetrics.descent)/FONT_MEASURE_FACTOR;
  }

  @Override
  public double getTextLeading(AndroidMeasureInfo textMeasureInfo) {
    return (Math.abs(textMeasureInfo.mFontMetrics.top)+Math.abs(textMeasureInfo.mFontMetrics.bottom)-Math.abs(textMeasureInfo.mFontMetrics.ascent)-Math.abs(textMeasureInfo.mFontMetrics.descent))/FONT_MEASURE_FACTOR;
  }

}
