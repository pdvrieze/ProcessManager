package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.Pen;
import android.graphics.Paint;
import android.graphics.Paint.Style;


public class AndroidPen implements Pen<AndroidPen> {

  private Paint aPaint;
  private double aStrokeWidth;
  private float aShadowRadius=-1f;
  private int aShadowColor;
  private float aShadowDx;
  private float aShadowDy;
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
    return this;
  }

}
