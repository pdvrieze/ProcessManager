package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.Pen;
import android.graphics.Paint;


public class AndroidPen implements Pen {

  private Paint aPaint;

  public AndroidPen(Paint pPaint) {
    aPaint = pPaint;
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
    aPaint.setStrokeWidth((float) pStrokeWidth);
    return this;
  }

}
