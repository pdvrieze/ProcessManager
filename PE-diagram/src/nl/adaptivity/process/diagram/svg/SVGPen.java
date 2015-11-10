package nl.adaptivity.process.diagram.svg;

import nl.adaptivity.diagram.Pen;
import nl.adaptivity.process.diagram.svg.TextMeasurer.MeasureInfo;


public class SVGPen<M extends MeasureInfo> implements Pen<SVGPen<M>>, Cloneable {

  private int aColor = 0xff000000;
  private double aStrokeWidth;
  private double aFontSize;
  private boolean aItalics;
  private TextMeasurer<M> aTextMeasurer;
  private M aTextMeasureInfo;

  public SVGPen(TextMeasurer<M> textMeasurer) {
    aTextMeasurer = textMeasurer;
  }

  @Override
  public SVGPen<M> setColor(int red, int green, int blue) {
    return setColor(red, green, blue, 0xff);
  }

  @Override
  public SVGPen<M> setColor(int red, int green, int blue, int alpha) {
    aColor = (alpha&0xff) << 24 | (red&0xff)<<16 | (green&0xff)<<8 | (blue&0xff);
    return this;
  }

  public int getColor() {
    return aColor;
  }

  @Override
  public SVGPen<M> setStrokeWidth(double strokeWidth) {
    aStrokeWidth = strokeWidth;
    return this;
  }

  public double getStrokeWidth() {
    return aStrokeWidth;
  }

  @Override
  public SVGPen<M> setFontSize(double fontSize) {
    aFontSize = fontSize;
    if (aTextMeasureInfo!=null) { aTextMeasureInfo.setFontSize(fontSize); }
    return this;
  }

  @Override
  public double getFontSize() {
    return aFontSize;
  }


  @Override
  public double measureTextWidth(String text, double foldWidth) {
    if (aTextMeasureInfo==null) {
      aTextMeasureInfo = aTextMeasurer.getTextMeasureInfo(this);
    }
    return aTextMeasurer.measureTextWidth(aTextMeasureInfo, text, foldWidth);
  }

  @Override
  public double getTextMaxAscent() {
    if (aTextMeasureInfo==null) {
      aTextMeasureInfo = aTextMeasurer.getTextMeasureInfo(this);
    }
    return aTextMeasurer.getTextMaxAscent(aTextMeasureInfo);
  }

  @Override
  public double getTextAscent() {
    if (aTextMeasureInfo==null) {
      aTextMeasureInfo = aTextMeasurer.getTextMeasureInfo(this);
    }
    return aTextMeasurer.getTextAscent(aTextMeasureInfo);
  }

  @Override
  public double getTextDescent() {
    if (aTextMeasureInfo==null) {
      aTextMeasureInfo = aTextMeasurer.getTextMeasureInfo(this);
    }
    return aTextMeasurer.getTextDescent(aTextMeasureInfo);
  }

  @Override
  public double getTextMaxDescent() {
    if (aTextMeasureInfo==null) {
      aTextMeasureInfo = aTextMeasurer.getTextMeasureInfo(this);
    }
    return aTextMeasurer.getTextMaxDescent(aTextMeasureInfo);
  }

  @Override
  public double getTextLeading() {
    if (aTextMeasureInfo==null) {
      aTextMeasureInfo = aTextMeasurer.getTextMeasureInfo(this);
    }
    return aTextMeasurer.getTextLeading(aTextMeasureInfo);
  }

  @Override
  public void setTextItalics(boolean italics) {
    aItalics = italics;
  }

  public boolean isTextItalics() {
    return aItalics;
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
