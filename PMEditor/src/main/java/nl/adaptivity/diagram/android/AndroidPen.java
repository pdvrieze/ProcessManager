package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.Pen;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.graphics.Typeface;


public class AndroidPen implements Pen<AndroidPen> {

  private static final float FONT_MEASURE_FACTOR = 3f;
  private Paint aPaint;
  private double aStrokeWidth;
  private float aShadowRadius=-1f;
  private int aShadowColor;
  private float aShadowDx;
  private float aShadowDy;
  private double aFontSize=Double.NaN;
  private FontMetrics aFontMetrics;


  public AndroidPen(Paint paint) {
    aPaint = paint;
    aPaint.setStyle(Style.STROKE);
  }

  public Paint getPaint() {
    return aPaint;
  }

  @Override
  public AndroidPen setColor(int red, int green, int blue) {
    aPaint.setARGB(255, red, green, blue);
    return this;
  }

  @Override
  public AndroidPen setColor(int red, int green, int blue, int alpha) {
    aPaint.setARGB(alpha, red, green, blue);
    return this;
  }

  @Override
  public AndroidPen setStrokeWidth(double strokeWidth) {
    aStrokeWidth = strokeWidth;
    aPaint.setStrokeWidth((float) strokeWidth);
    return this;
  }
  
  @Override
  public double getStrokeWidth() {
    return aStrokeWidth;
  }

  public void setShadowLayer(float radius, int color) {
    aShadowRadius = radius;
    aShadowColor = color;
    aShadowDx = 0f;
    aShadowDy = 0f;
    aPaint.setShadowLayer(radius, aShadowDx, aShadowDy, color);
  }

  public AndroidPen scale(double scale) {
    aPaint.setStrokeWidth((float) (aStrokeWidth*scale));
    if (aShadowRadius>0f) {
      aPaint.setShadowLayer((float) (aShadowRadius*scale), (float) (aShadowDx*scale), (float) (aShadowDy*scale), aShadowColor);
    }
    if (!Double.isNaN(aFontSize)) {
      aPaint.setTextSize((float) (aFontSize*scale));
    }
    return this;
  }

  @Override
  public AndroidPen setFontSize(double fontSize) {
    aPaint.setTextAlign(Align.LEFT);
    aPaint.setTextSize((float) fontSize);
    aFontSize = fontSize;
    return this;
  }

  @Override
  public double getFontSize() {
    return aFontSize;
  }

  @Override
  public double measureTextWidth(String text, double foldWidth) {
    float ts = aPaint.getTextSize();
    aPaint.setTextSize(((float) aFontSize)*FONT_MEASURE_FACTOR);
    final float result = aPaint.measureText(text)/FONT_MEASURE_FACTOR;
    aPaint.setTextSize(ts);
    return result;
  }

  public void ensureFontMetrics() {
    if (aFontMetrics==null) {
      float ts = aPaint.getTextSize();
      aPaint.setTextSize((float) aFontSize);
      aFontMetrics=aPaint.getFontMetrics();
      aPaint.setTextSize(ts);
    }
  }

  @Override
  public double getTextMaxAscent() {
    ensureFontMetrics();
    return Math.abs(aFontMetrics.top);
  }

  public double getTextAscent() {
    ensureFontMetrics();
    return Math.abs(aFontMetrics.ascent);
  }

  @Override
  public double getTextMaxDescent() {
    ensureFontMetrics();
    return Math.abs(aFontMetrics.bottom);
  }

  public double getTextDescent() {
    ensureFontMetrics();
    return Math.abs(aFontMetrics.descent);
  }

  @Override
  public double getTextLeading() {
//    float ts = aPaint.getTextSize();
//    aPaint.setTextSize((float) aFontSize);
//    double result = aPaint.getFontSpacing() - aFontSize;
//    aPaint.setTextSize(ts);
//    return result;
    ensureFontMetrics();
    return Math.abs(aFontMetrics.top)+Math.abs(aFontMetrics.bottom)-Math.abs(aFontMetrics.ascent)-Math.abs(aFontMetrics.descent);
  }

  @Override
  public void setTextItalics(boolean italics) {
    final Typeface oldTypeface = aPaint.getTypeface();
    final int style;
    if (oldTypeface==null) {
      style = italics ? Typeface.ITALIC : Typeface.NORMAL;
    } else {
      style = (oldTypeface.getStyle() & ~ Typeface.ITALIC) | (italics ? Typeface.ITALIC : Typeface.NORMAL);
    }
    aPaint.setTypeface(Typeface.create(oldTypeface,style));
  }

}
