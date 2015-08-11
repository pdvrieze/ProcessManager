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


  public AndroidPen(Paint pPaint) {
    aPaint = pPaint;
    aPaint.setStyle(Style.STROKE);
  }

  public Paint getPaint() {
    return aPaint;
  }

  @Override
  public AndroidPen setColor(int pRed, int pGreen, int pBlue) {
    aPaint.setARGB(255, pRed, pGreen, pBlue);
    return this;
  }

  @Override
  public AndroidPen setColor(int pRed, int pGreen, int pBlue, int pAlpha) {
    aPaint.setARGB(pAlpha, pRed, pGreen, pBlue);
    return this;
  }

  @Override
  public AndroidPen setStrokeWidth(double pStrokeWidth) {
    aStrokeWidth = pStrokeWidth;
    aPaint.setStrokeWidth((float) pStrokeWidth);
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

  public AndroidPen scale(double pScale) {
    aPaint.setStrokeWidth((float) (aStrokeWidth*pScale));
    if (aShadowRadius>0f) {
      aPaint.setShadowLayer((float) (aShadowRadius*pScale), (float) (aShadowDx*pScale), (float) (aShadowDy*pScale), aShadowColor);
    }
    if (!Double.isNaN(aFontSize)) {
      aPaint.setTextSize((float) (aFontSize*pScale));
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
  public double measureTextWidth(String pText, double pFoldWidth) {
    float ts = aPaint.getTextSize();
    aPaint.setTextSize(((float) aFontSize)*FONT_MEASURE_FACTOR);
    final float result = aPaint.measureText(pText)/FONT_MEASURE_FACTOR;
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
  public void setTextItalics(boolean pItalics) {
    final Typeface oldTypeface = aPaint.getTypeface();
    final int style;
    if (oldTypeface==null) {
      style = pItalics ? Typeface.ITALIC : Typeface.NORMAL;
    } else {
      style = (oldTypeface.getStyle() & ~ Typeface.ITALIC) | (pItalics ? Typeface.ITALIC : Typeface.NORMAL);
    }
    aPaint.setTypeface(Typeface.create(oldTypeface,style));
  }

}
