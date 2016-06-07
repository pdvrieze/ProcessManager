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

import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.svg.JVMTextMeasurer.JvmMeasureInfo;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;


public class JVMTextMeasurer implements TextMeasurer<JvmMeasureInfo> {

  private static final String SAMPLE_LETTERS="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

  public static class JvmMeasureInfo implements TextMeasurer.MeasureInfo {

    Font mFont;
    final FontRenderContext mFontRenderContext;
    private double mLeading = Double.NaN;
    private double mMaxAscent = Double.NaN;
    private double mMaxDescent = Double.NaN;
    private double mAscent = Double.NaN;
    private double mDescent = Double.NaN;

    public JvmMeasureInfo(final Font font) {
      mFont = font;
      mFontRenderContext = new FontRenderContext(null, true, true);
    }

    private void calcMetrics() {
      if (Double.isNaN(mMaxAscent)) {
        Rectangle2D maxBounds = mFont.getMaxCharBounds(mFontRenderContext);
        mMaxAscent = maxBounds.getMinY();
        mMaxDescent = maxBounds.getMaxY();
      }
      if (Double.isNaN(mLeading)) {
        LineMetrics linemetrics = mFont.getLineMetrics(SAMPLE_LETTERS, mFontRenderContext);
        mLeading = linemetrics.getLeading();
        mAscent = linemetrics.getAscent();
        mDescent = linemetrics.getDescent();
      }
    }

    public double getTextLeading() {
      calcMetrics();
      return mLeading;
    }

    public double getTextDescent() {
      calcMetrics();
      return mDescent;
    }

    public double getTextMaxDescent() {
      calcMetrics();
      return mMaxDescent;
    }

    public double getTextAscent() {
      calcMetrics();
      return mAscent;
    }

    public double getTextMaxAscent() {
      calcMetrics();
      return mMaxAscent;
    }

    @Override
    public void setFontSize(final double fontSize) {
      mFont = mFont.deriveFont((float) fontSize);
      mLeading = Double.NaN;
      mMaxAscent = Double.NaN;
      mMaxDescent = Double.NaN;
      mAscent = Double.NaN;
      mDescent = Double.NaN;
    }

  }

  private static final float FONT_MEASURE_FACTOR = 1f;
  private static final String FONTNAME = "SansSerif";

  @Override
  public JvmMeasureInfo getTextMeasureInfo(final SVGPen<JvmMeasureInfo> svgPen) {
    int style = (svgPen.isTextItalics() ? Font.ITALIC : 0) |
                (svgPen.isTextBold() ? Font.BOLD : 0);

    Font font = new Font(FONTNAME, style, 10).deriveFont((float)Math.ceil(svgPen.getFontSize()*FONT_MEASURE_FACTOR));

    return new JvmMeasureInfo(font);
  }

  @Override
  public double measureTextWidth(final JvmMeasureInfo textMeasureInfo, final String text, final double foldWidth) {
    final Rectangle2D bounds = textMeasureInfo.mFont.getStringBounds(text, textMeasureInfo.mFontRenderContext);

    return bounds.getWidth();
  }

  @Override
  public Rectangle measureTextSize(final Rectangle dest, final JvmMeasureInfo textMeasureInfo, final String text, final double foldWidth) {
    final Rectangle2D bounds = textMeasureInfo.mFont.getStringBounds(text, textMeasureInfo.mFontRenderContext);

    dest.left = 0;
    dest.top = bounds.getMinY();
    dest.width = bounds.getWidth();
    dest.height = bounds.getHeight();
    return dest;
  }

  @Override
  public double getTextMaxAscent(final JvmMeasureInfo textMeasureInfo) {
    return textMeasureInfo.getTextMaxAscent();
  }

  @Override
  public double getTextAscent(final JvmMeasureInfo textMeasureInfo) {
    return textMeasureInfo.getTextAscent();
  }

  @Override
  public double getTextMaxDescent(final JvmMeasureInfo textMeasureInfo) {
    return textMeasureInfo.getTextMaxDescent();
  }

  @Override
  public double getTextDescent(final JvmMeasureInfo textMeasureInfo) {
    return textMeasureInfo.getTextDescent();
  }

  @Override
  public double getTextLeading(final JvmMeasureInfo textMeasureInfo) {
    return textMeasureInfo.getTextLeading();
  }

}
