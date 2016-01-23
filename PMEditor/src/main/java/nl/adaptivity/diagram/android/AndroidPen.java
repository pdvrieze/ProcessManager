/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.diagram.android;

import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import nl.adaptivity.diagram.Pen;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import nl.adaptivity.diagram.Rectangle;


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
  public AndroidPen setColor(@IntRange(from=0, to=255) int red,@IntRange(from=0, to=255)  int green, @IntRange(from=0, to=255) int blue) {
    mPaint.setARGB(255, red, green, blue);
    return this;
  }

  @Override
  public AndroidPen setColor(@IntRange(from=0, to=255) int red,@IntRange(from=0, to=255)  int green,@IntRange(from=0, to=255)  int blue, @IntRange(from=0, to=255) int alpha) {
    mPaint.setARGB(alpha, red, green, blue);
    return this;
  }

  @Override
  public AndroidPen setStrokeWidth(@FloatRange(from=0, to=Float.MAX_VALUE,fromInclusive = false) double strokeWidth) {
    mStrokeWidth = strokeWidth;
    mPaint.setStrokeWidth((float) strokeWidth);
    return this;
  }
  
  @Override
  public double getStrokeWidth() {
    return mStrokeWidth;
  }

  public void setShadowLayer(@FloatRange(from=0, to=Float.MAX_VALUE,fromInclusive = false) float radius, @ColorInt int color) {
    mShadowRadius = radius;
    mShadowColor = color;
    mShadowDx = 0f;
    mShadowDy = 0f;
    mPaint.setShadowLayer(radius, mShadowDx, mShadowDy, color);
  }

  public AndroidPen scale(@FloatRange(from=0, to=Float.MAX_VALUE,fromInclusive = false) double scale) {
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
  public AndroidPen setFontSize(@FloatRange(from=0, to=Float.MAX_VALUE,fromInclusive = false) double fontSize) {
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
  public double measureTextWidth(String text, @FloatRange(from=0, to=Float.MAX_VALUE,fromInclusive = false) double foldWidth) {
    float ts = mPaint.getTextSize();
    mPaint.setTextSize(((float) mFontSize)*FONT_MEASURE_FACTOR);
    final float result = mPaint.measureText(text)/FONT_MEASURE_FACTOR;
    mPaint.setTextSize(ts);
    return result;
  }

  @Override
  public Rectangle measureTextSize(final Rectangle dest, final double x, final double y, final String text, final double foldWidth) {
    float ts = mPaint.getTextSize();
    mPaint.setTextSize(((float) mFontSize)*FONT_MEASURE_FACTOR);
    ensureFontMetrics();
    double left=x;
    double width = mPaint.measureText(text)/FONT_MEASURE_FACTOR;
    double top=y+mFontMetrics.top-mFontMetrics.leading/2;
    double height=mFontMetrics.leading+mFontMetrics.top+mFontMetrics.bottom;
    dest.set(left,top, width, height);
    return dest;
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
