package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.Pen;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.graphics.Typeface;


public class AndroidPen implements Pen<AndroidPen> {

  private static final float FONT_MEASURE_FACTOR = 3f;
  private Paint mPaint;
  private double mStrokeWidth;
  private float mShadowRadius=-1f;
  private int mShadowColor;
  private float mShadowDx;
  private float mShadowDy;
  private double mFontSize=Double.NaN;
  private FontMetrics mFontMetrics;


  public AndroidPen(Paint paint) {
    mPaint = paint;
    mPaint.setStyle(Style.STROKE);
  }

  public Paint getPaint() {
    return mPaint;
  }

  @Override
  public AndroidPen setColor(int red, int green, int blue) {
    mPaint.setARGB(255, red, green, blue);
    return this;
  }

  @Override
  public AndroidPen setColor(int red, int green, int blue, int alpha) {
    mPaint.setARGB(alpha, red, green, blue);
    return this;
  }

  @Override
  public AndroidPen setStrokeWidth(double strokeWidth) {
    mStrokeWidth = strokeWidth;
    mPaint.setStrokeWidth((float) strokeWidth);
    return this;
  }
  
  @Override
  public double getStrokeWidth() {
    return mStrokeWidth;
  }

  public void setShadowLayer(float radius, int color) {
    mShadowRadius = radius;
    mShadowColor = color;
    mShadowDx = 0f;
    mShadowDy = 0f;
    mPaint.setShadowLayer(radius, mShadowDx, mShadowDy, color);
  }

  public AndroidPen scale(double scale) {
    mPaint.setStrokeWidth((float) (mStrokeWidth*scale));
    if (mShadowRadius>0f) {
      mPaint.setShadowLayer((float) (mShadowRadius*scale), (float) (mShadowDx*scale), (float) (mShadowDy*scale), mShadowColor);
    }
    if (!Double.isNaN(mFontSize)) {
      mPaint.setTextSize((float) (mFontSize*scale));
    }
    return this;
  }

  @Override
  public AndroidPen setFontSize(double fontSize) {
    mPaint.setTextAlign(Align.LEFT);
    mPaint.setTextSize((float) fontSize);
    mFontSize = fontSize;
    return this;
  }

  @Override
  public double getFontSize() {
    return mFontSize;
  }

  @Override
  public double measureTextWidth(String text, double foldWidth) {
    float ts = mPaint.getTextSize();
    mPaint.setTextSize(((float) mFontSize)*FONT_MEASURE_FACTOR);
    final float result = mPaint.measureText(text)/FONT_MEASURE_FACTOR;
    mPaint.setTextSize(ts);
    return result;
  }

  public void ensureFontMetrics() {
    if (mFontMetrics==null) {
      float ts = mPaint.getTextSize();
      mPaint.setTextSize((float) mFontSize);
      mFontMetrics=mPaint.getFontMetrics();
      mPaint.setTextSize(ts);
    }
  }

  @Override
  public double getTextMaxAscent() {
    ensureFontMetrics();
    return Math.abs(mFontMetrics.top);
  }

  public double getTextAscent() {
    ensureFontMetrics();
    return Math.abs(mFontMetrics.ascent);
  }

  @Override
  public double getTextMaxDescent() {
    ensureFontMetrics();
    return Math.abs(mFontMetrics.bottom);
  }

  public double getTextDescent() {
    ensureFontMetrics();
    return Math.abs(mFontMetrics.descent);
  }

  @Override
  public double getTextLeading() {
//    float ts = mPaint.getTextSize();
//    mPaint.setTextSize((float) mFontSize);
//    double result = mPaint.getFontSpacing() - mFontSize;
//    mPaint.setTextSize(ts);
//    return result;
    ensureFontMetrics();
    return Math.abs(mFontMetrics.top)+Math.abs(mFontMetrics.bottom)-Math.abs(mFontMetrics.ascent)-Math.abs(mFontMetrics.descent);
  }

  @Override
  public void setTextItalics(boolean italics) {
    final Typeface oldTypeface = mPaint.getTypeface();
    final int style;
    if (oldTypeface==null) {
      style = italics ? Typeface.ITALIC : Typeface.NORMAL;
    } else {
      style = (oldTypeface.getStyle() & ~ Typeface.ITALIC) | (italics ? Typeface.ITALIC : Typeface.NORMAL);
    }
    mPaint.setTypeface(Typeface.create(oldTypeface,style));
  }

}
