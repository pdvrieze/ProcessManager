package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.Pen;
import android.graphics.Paint;
import android.graphics.Paint.Style;


public class AndroidPen implements Pen {

  private Paint aPaint;
  private double aStrokeWidth;

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

  public Pen scale(double pScale) {
    aPaint.setStrokeWidth((float) (aStrokeWidth*pScale));
    return this;
  }

}
