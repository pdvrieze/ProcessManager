package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.Color;
import nl.adaptivity.diagram.Rectangle;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;


public class AndroidCanvas implements nl.adaptivity.diagram.Canvas {

  private class OffsetCanvas implements nl.adaptivity.diagram.Canvas {
    private double aXOffset;
    private double aYOffset;
    private double aScale;


    public OffsetCanvas(OffsetCanvas pBase, Rectangle pArea, double pScale) {
      aXOffset = pBase.aXOffset + pArea.left;
      aYOffset = pBase.aYOffset + pArea.top;
      aScale = pBase.aScale*pScale;
    }

    public OffsetCanvas(Rectangle pArea, double pScale) {
      aXOffset = pArea.left;
      aYOffset = pArea.top;
      aScale = pScale;
    }

    @Override
    public nl.adaptivity.diagram.Canvas childCanvas(Rectangle pArea, double pScale) {
      return new OffsetCanvas(this, pArea, pScale);
    }

    @Override
    public Color newColor(int pR, int pG, int pB, int pA) {
      final Color color = AndroidCanvas.this.newColor(pR, pG, pB, pA);
      ((AndroidColor) color).getPaint().setStrokeWidth((float) aScale);
      return color;
    }

    @Override
    public void drawCircle(double pX, double pY, double pRadius, Color pColor) {
      AndroidCanvas.this.drawCircle(pX*aScale+aXOffset, pY*aScale*aYOffset, pRadius*aScale, pColor);
    }

    @Override
    public void drawFilledCircle(double pX, double pY, double pRadius, Color pColor) {
      AndroidCanvas.this.drawFilledCircle(pX*aScale+aXOffset, pY*aScale*aYOffset, pRadius*aScale, pColor);
    }

    @Override
    public void drawRect(Rectangle pRect, Color pColor) {
      AndroidCanvas.this.drawRect(pRect.offsetScaled(aXOffset, aYOffset, aScale), pColor);
    }

    @Override
    public void drawFilledRect(Rectangle pRect, Color pColor) {
      AndroidCanvas.this.drawFilledRect(pRect.offsetScaled(aXOffset, aYOffset, aScale), pColor);
    }

    @Override
    public void drawPath(double[] pPoints, Color pColor) {
      AndroidCanvas.this.drawPath(transform(pPoints), pColor);
    }

    @Override
    public void drawFilledPath(double[] pPoints, Color pColor) {
      AndroidCanvas.this.drawFilledPath(transform(pPoints), pColor);
    }

    private double[] transform(double[] pPoints) {
      double[] result = new double[pPoints.length];
      final int len = pPoints.length-1;
      for(int i=0; i<len;++i) {
        result[i] = pPoints[i]*aScale+aXOffset;
        ++i;
        result[i] = pPoints[i]*aScale+aYOffset;
      }
      return result;
    }

    @Override
    public void drawRoundRect(Rectangle pRect, double pRx, double pRy, Color pColor) {
      AndroidCanvas.this.drawRoundRect(pRect.offsetScaled(aXOffset, aYOffset, aScale), pRx, pRy, pColor);
    }

    @Override
    public void drawFilledRoundRect(Rectangle pRect, double pRx, double pRy, Color pColor) {
      AndroidCanvas.this.drawFilledRoundRect(pRect.offsetScaled(aXOffset, aYOffset, aScale), pRx, pRy, pColor);
    }

  }

  android.graphics.Canvas aCanvas;

  public AndroidCanvas(android.graphics.Canvas pCanvas) {
    aCanvas = pCanvas;
  }

  @Override
  public nl.adaptivity.diagram.Canvas childCanvas(Rectangle pArea, double pScale) {
    return new OffsetCanvas(pArea, pScale);
  }

  @Override
  public Color newColor(int pR, int pG, int pB, int pA) {
    // TODO cache this some way
    Paint paint = new Paint();
    paint.setColor(android.graphics.Color.argb(pA, pR, pG, pB));

    // TODO make this configurable
    paint.setStrokeWidth(1f);

    return new AndroidColor(paint);
  }

  @Override
  public void drawFilledCircle(double pX, double pY, double pRadius, Color pColor) {
    Paint paint = ((AndroidColor) pColor).getPaint();
    Style oldStyle = paint.getStyle();
    paint.setStyle(Paint.Style.FILL);
    aCanvas.drawCircle((float) pX, (float) pY, (float) pRadius, paint);
    paint.setStyle(oldStyle);
  }

  @Override
  public void drawCircle(double pX, double pY, double pRadius, Color pColor) {
    aCanvas.drawCircle((float) pX, (float) pY, (float) pRadius, ((AndroidColor) pColor).getPaint());
  }

  @Override
  public void drawFilledRoundRect(Rectangle pRect, double pRx, double pRy, Color pColor) {
    Paint paint = ((AndroidColor) pColor).getPaint();
    Style oldStyle = paint.getStyle();
    paint.setStyle(Paint.Style.FILL);
    aCanvas.drawRoundRect(toRectF(pRect), (float)pRx, (float)pRy, ((AndroidColor)pColor).getPaint());
    paint.setStyle(oldStyle);
  }

  @Override
  public void drawRoundRect(Rectangle pRect, double pRx, double pRy, Color pColor) {
    aCanvas.drawRoundRect(toRectF(pRect), (float)pRx, (float)pRy, ((AndroidColor)pColor).getPaint());
  }

  @Override
  public void drawFilledRect(Rectangle pRect, Color pColor) {
    Paint paint = ((AndroidColor) pColor).getPaint();
    Style oldStyle = paint.getStyle();
    paint.setStyle(Paint.Style.FILL);
    aCanvas.drawRect(toRectF(pRect), ((AndroidColor)pColor).getPaint());
    paint.setStyle(oldStyle);
  }

  @Override
  public void drawRect(Rectangle pRect, Color pColor) {
    aCanvas.drawRect(toRectF(pRect), ((AndroidColor)pColor).getPaint());
  }

  @Override
  public void drawPath(double[] pPoints, Color pColor) {
    aCanvas.drawPath(toPath(pPoints), ((AndroidColor)pColor).getPaint());
  }

  @Override
  public void drawFilledPath(double[] pPoints, Color pColor) {
    Paint paint = ((AndroidColor) pColor).getPaint();
    Style oldStyle = paint.getStyle();
    paint.setStyle(Paint.Style.FILL);
    aCanvas.drawPath(toPath(pPoints), ((AndroidColor)pColor).getPaint());
    paint.setStyle(oldStyle);
  }

  private static Path toPath(double[] pPoints) {
    Path result = new Path();

    final int len = pPoints.length - 1;
    if (len>0) {
      result.moveTo((float)pPoints[0], (float)pPoints[1]);
      for(int i=2; i<len; ++i) {
        result.lineTo((float)pPoints[i], (float) pPoints[++i]);
      }
      result.close();
    }
    return result;
  }

  private static RectF toRectF(Rectangle pRect) {
    return new RectF(pRect.leftf(), pRect.topf(), pRect.rightf(), pRect.bottomf());
  }

}
