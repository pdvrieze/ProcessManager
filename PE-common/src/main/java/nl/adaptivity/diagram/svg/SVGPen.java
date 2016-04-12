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

package nl.adaptivity.diagram.svg;

import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.svg.TextMeasurer.MeasureInfo;


public class SVGPen<M extends MeasureInfo> implements Pen<SVGPen<M>>, Cloneable {

  private int mColor = 0xff000000;
  private double mStrokeWidth;
  private double mFontSize;
  private boolean mItalics;
  private TextMeasurer<M> mTextMeasurer;
  private M mTextMeasureInfo;

  public SVGPen(TextMeasurer<M> textMeasurer) {
    mTextMeasurer = textMeasurer;
  }

  @Override
  public SVGPen<M> setColor(int red, int green, int blue) {
    return setColor(red, green, blue, 0xff);
  }

  @Override
  public SVGPen<M> setColor(int red, int green, int blue, int alpha) {
    mColor = (alpha&0xff) << 24 | (red&0xff)<<16 | (green&0xff)<<8 | (blue&0xff);
    return this;
  }

  public int getColor() {
    return mColor;
  }

  @Override
  public SVGPen<M> setStrokeWidth(double strokeWidth) {
    mStrokeWidth = strokeWidth;
    return this;
  }

  public double getStrokeWidth() {
    return mStrokeWidth;
  }

  @Override
  public SVGPen<M> setFontSize(double fontSize) {
    mFontSize = fontSize;
    if (mTextMeasureInfo!=null) { mTextMeasureInfo.setFontSize(fontSize); }
    return this;
  }

  @Override
  public double getFontSize() {
    return mFontSize;
  }


  @Override
  public double measureTextWidth(String text, double foldWidth) {
    if (mTextMeasureInfo==null) {
      mTextMeasureInfo = mTextMeasurer.getTextMeasureInfo(this);
    }
    return mTextMeasurer.measureTextWidth(mTextMeasureInfo, text, foldWidth);
  }

  @Override
  public Rectangle measureTextSize(Rectangle dest, final double x, final double y, String text, double foldWidth) {
    if (mTextMeasureInfo==null) {
      mTextMeasureInfo = mTextMeasurer.getTextMeasureInfo(this);
    }
    mTextMeasurer.measureTextSize(dest, mTextMeasureInfo, text, foldWidth);
    dest.top+=y;
    dest.left+=x;
    return  dest;
  }

  @Override
  public double getTextMaxAscent() {
    if (mTextMeasureInfo==null) {
      mTextMeasureInfo = mTextMeasurer.getTextMeasureInfo(this);
    }
    return mTextMeasurer.getTextMaxAscent(mTextMeasureInfo);
  }

  @Override
  public double getTextAscent() {
    if (mTextMeasureInfo==null) {
      mTextMeasureInfo = mTextMeasurer.getTextMeasureInfo(this);
    }
    return mTextMeasurer.getTextAscent(mTextMeasureInfo);
  }

  @Override
  public double getTextDescent() {
    if (mTextMeasureInfo==null) {
      mTextMeasureInfo = mTextMeasurer.getTextMeasureInfo(this);
    }
    return mTextMeasurer.getTextDescent(mTextMeasureInfo);
  }

  @Override
  public double getTextMaxDescent() {
    if (mTextMeasureInfo==null) {
      mTextMeasureInfo = mTextMeasurer.getTextMeasureInfo(this);
    }
    return mTextMeasurer.getTextMaxDescent(mTextMeasureInfo);
  }

  @Override
  public double getTextLeading() {
    if (mTextMeasureInfo==null) {
      mTextMeasureInfo = mTextMeasurer.getTextMeasureInfo(this);
    }
    return mTextMeasurer.getTextLeading(mTextMeasureInfo);
  }

  @Override
  public void setTextItalics(boolean italics) {
    mItalics = italics;
  }

  public boolean isTextItalics() {
    return mItalics;
  }

  @SuppressWarnings("unchecked")
  @Override
  public SVGPen<M> clone() {
    try {
      return (SVGPen<M>) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new AssertionError("This should never throw", e);
    }
  }

}
