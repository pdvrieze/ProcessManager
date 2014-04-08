package nl.adaptivity.process.diagram.svg;

import nl.adaptivity.diagram.Pen;


public class SVGPen implements Pen<SVGPen>, Cloneable {

  private int aColor;
  private double aStrokeWidth;
  private double aFontSize;
  private boolean aItalics;
  private TextMeasurer aTextMeasurer;
  private TextMeasurer.MeasureInfo aTextMeasureInfo;

  public SVGPen(TextMeasurer pTextMeasurer) {
    aTextMeasurer = pTextMeasurer;
  }

  @Override
  public SVGPen setColor(int pRed, int pGreen, int pBlue) {
    aColor = (pRed&0xff)<<16 | (pGreen&0xff)<<8 | (pBlue&0xff);
    return this;
  }

  @Override
  public SVGPen setColor(int pRed, int pGreen, int pBlue, int pAlpha) {
    aColor = (pAlpha&0xff) << 24 | (pRed&0xff)<<16 | (pGreen&0xff)<<8 | (pBlue&0xff);
    return this;
  }

  @Override
  public SVGPen setStrokeWidth(double pStrokeWidth) {
    aStrokeWidth = pStrokeWidth;
    return this;
  }

  @Override
  public SVGPen setFontSize(double pFontSize) {
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
      aTextMeasurer.getTextMeasureInfo(this);
    }
    return aTextMeasurer.measureTextWidth(aTextMeasureInfo, pText, pFoldWidth);
  }

  @Override
  public double getTextMaxAscent() {
    return aTextMeasurer.getTextMaxAscent(aTextMeasureInfo);
  }

  @Override
  public double getTextMaxDescent() {
    return aTextMeasurer.getTextMaxDescent(aTextMeasureInfo);
  }

  @Override
  public double getTextLeading() {
    return aTextMeasurer.getTextLeading(aTextMeasureInfo);
  }

  @Override
  public void setTextItalics(boolean pItalics) {
    aItalics = pItalics;
  }

  @Override
  public SVGPen clone() {
    try {
      return (SVGPen) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new AssertionError("This should never throw", e);
    }
  }
  
}
