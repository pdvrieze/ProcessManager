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

  public SVGPen(TextMeasurer<M> pTextMeasurer) {
    aTextMeasurer = pTextMeasurer;
  }

  @Override
  public SVGPen<M> setColor(int pRed, int pGreen, int pBlue) {
    return setColor(pRed, pGreen, pBlue, 0xff);
  }

  @Override
  public SVGPen<M> setColor(int pRed, int pGreen, int pBlue, int pAlpha) {
    aColor = (pAlpha&0xff) << 24 | (pRed&0xff)<<16 | (pGreen&0xff)<<8 | (pBlue&0xff);
    return this;
  }

  public int getColor() {
    return aColor;
  }

  @Override
  public SVGPen<M> setStrokeWidth(double pStrokeWidth) {
    aStrokeWidth = pStrokeWidth;
    return this;
  }

  public double getStrokeWidth() {
    return aStrokeWidth;
  }

  @Override
  public SVGPen<M> setFontSize(double pFontSize) {
    aFontSize = pFontSize;
    if (aTextMeasureInfo!=null) { aTextMeasureInfo.setFontSize(pFontSize); }
    return this;
  }

  @Override
  public double getFontSize() {
    return aFontSize;
  }


  @Override
  public double measureTextWidth(String pText, double pFoldWidth) {
    if (aTextMeasureInfo==null) {
      aTextMeasureInfo = aTextMeasurer.getTextMeasureInfo(this);
    }
    return aTextMeasurer.measureTextWidth(aTextMeasureInfo, pText, pFoldWidth);
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
  public void setTextItalics(boolean pItalics) {
    aItalics = pItalics;
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
