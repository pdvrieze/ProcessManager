package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.DiagramPath;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;


public class AndroidCanvas implements nl.adaptivity.diagram.Canvas {

  private class OffsetCanvas implements nl.adaptivity.diagram.Canvas {
    /** The offset of the canvas. This is in scaled coordinates. */
    private double aXOffset;
    private double aYOffset;
    private double aScale;


    public OffsetCanvas(OffsetCanvas pBase, Rectangle pArea, double pScale) {
      aXOffset = (pBase.aXOffset - pArea.left)*pScale;
      aYOffset = (pBase.aYOffset - pArea.top)*pScale;
      aScale = pBase.aScale*pScale;
    }

    public OffsetCanvas(Rectangle pArea, double pScale) {
      aXOffset = -pArea.left* pScale;
      aYOffset = -pArea.top* pScale;
      aScale = pScale;
    }

    @Override
    public nl.adaptivity.diagram.Canvas childCanvas(Rectangle pArea, double pScale) {
      return new OffsetCanvas(this, pArea, pScale);
    }

    @Override
    public Pen newColor(int pR, int pG, int pB, int pA) {
      return AndroidCanvas.this.newColor(pR, pG, pB, pA);
    }

    @Override
    public AndroidPen newPen() {
      return AndroidCanvas.this.newPen();
    }

    @Override
    public AndroidPath newPath() {
      return AndroidCanvas.this.newPath();
    }

    private Pen scalePen(Pen pPen) {
      return ((AndroidPen) pPen).scale(aScale);
    }

    @Override
    public void drawCircle(double pX, double pY, double pRadius, Pen pPen) {
      AndroidCanvas.this.drawCircle((pX-aXOffset)*aScale, (pY - aYOffset) * aScale, pRadius*aScale, scalePen(pPen));
    }

    @Override
    public void drawFilledCircle(double pX, double pY, double pRadius, Pen pPen) {
      AndroidCanvas.this.drawFilledCircle((pX - aXOffset) * aScale, (pY - aYOffset) * aScale, pRadius*aScale, pPen);
    }

    @Override
    public void drawRect(Rectangle pRect, Pen pPen) {
      AndroidCanvas.this.drawRect(pRect.offsetScaled(-aXOffset, -aYOffset, aScale), scalePen(pPen));
    }

    @Override
    public void drawFilledRect(Rectangle pRect, Pen pPen) {
      AndroidCanvas.this.drawFilledRect(pRect.offsetScaled(-aXOffset, -aYOffset, aScale), pPen);
    }

    @Override
    public void drawPoly(double[] pPoints, Pen pPen) {
      AndroidCanvas.this.drawPoly(transform(pPoints), scalePen(pPen));
    }

    @Override
    public void drawFilledPoly(double[] pPoints, Pen pPen) {
      AndroidCanvas.this.drawFilledPoly(transform(pPoints), pPen);
    }

    private double[] transform(double[] pPoints) {
      double[] result = new double[pPoints.length];
      final int len = pPoints.length-1;
      for(int i=0; i<len;++i) {
        result[i] = (pPoints[i]-aXOffset)*aScale;
        ++i;
        result[i] = (pPoints[i]-aYOffset)*aScale;
      }
      return result;
    }

    @Override
    public void drawPath(DiagramPath pPath, Pen pColor) {
//      Matrix matrix = new Matrix();
//      matrix.setScale((float)aScale, (float)aScale);
//      matrix.postTranslate((float)aXOffset, (float) aYOffset);
//      ((AndroidPath) pPath).getPath().transform(matrix);
      AndroidCanvas.this.drawPath(pPath, pColor);
//      ((AndroidPath) pPath).getPath().transform(matrix);
    }

    @Override
    public void drawFilledPath(DiagramPath pPath, Pen pColor) {
      // TODO Auto-generated method stub
      //
      throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void drawRoundRect(Rectangle pRect, double pRx, double pRy, Pen pPen) {
      AndroidCanvas.this.drawRoundRect(pRect.offsetScaled(-aXOffset, -aYOffset, aScale), pRx*aScale, pRy*aScale, scalePen(pPen));
    }

    @Override
    public void drawFilledRoundRect(Rectangle pRect, double pRx, double pRy, Pen pPen) {
      AndroidCanvas.this.drawFilledRoundRect(pRect.offsetScaled(-aXOffset, -aYOffset, aScale), pRx*aScale, pRy*aScale, pPen);
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
  public AndroidPen newColor(int pR, int pG, int pB, int pA) {
    // TODO cache this some way
    return newPen().setColor(pR, pG, pB, pA);
  }

  @Override
  public AndroidPen newPen() {
    Paint paint = new Paint();
    paint.setAntiAlias(true);
    return new AndroidPen(paint);
  }

  public AndroidPath newPath() {
    return new AndroidPath();
  }

  @Override
  public void drawFilledCircle(double pX, double pY, double pRadius, Pen pPen) {
    Paint paint = ((AndroidPen) pPen).getPaint();
    Style oldStyle = paint.getStyle();
    paint.setStyle(Paint.Style.FILL);
    aCanvas.drawCircle((float) pX, (float) pY, (float) pRadius, paint);
    paint.setStyle(oldStyle);
  }

  @Override
  public void drawCircle(double pX, double pY, double pRadius, Pen pPen) {
    aCanvas.drawCircle((float) pX, (float) pY, (float) pRadius, ((AndroidPen) pPen).getPaint());
  }

  @Override
  public void drawFilledRoundRect(Rectangle pRect, double pRx, double pRy, Pen pPen) {
    Paint paint = ((AndroidPen) pPen).getPaint();
    Style oldStyle = paint.getStyle();
    paint.setStyle(Paint.Style.FILL);
    aCanvas.drawRoundRect(toRectF(pRect), (float)pRx, (float)pRy, ((AndroidPen)pPen).getPaint());
    paint.setStyle(oldStyle);
  }

  @Override
  public void drawRoundRect(Rectangle pRect, double pRx, double pRy, Pen pPen) {
    aCanvas.drawRoundRect(toRectF(pRect), (float)pRx, (float)pRy, ((AndroidPen)pPen).getPaint());
  }

  @Override
  public void drawFilledRect(Rectangle pRect, Pen pPen) {
    Paint paint = ((AndroidPen) pPen).getPaint();
    Style oldStyle = paint.getStyle();
    paint.setStyle(Paint.Style.FILL);
    aCanvas.drawRect(toRectF(pRect), ((AndroidPen)pPen).getPaint());
    paint.setStyle(oldStyle);
  }

  @Override
  public void drawRect(Rectangle pRect, Pen pPen) {
    aCanvas.drawRect(toRectF(pRect), ((AndroidPen)pPen).getPaint());
  }

  @Override
  public void drawPoly(double[] pPoints, Pen pPen) {
    aCanvas.drawPath(toPath(pPoints), ((AndroidPen)pPen).getPaint());
  }

  @Override
  public void drawFilledPoly(double[] pPoints, Pen pPen) {
    Paint paint = ((AndroidPen) pPen).getPaint();
    Style oldStyle = paint.getStyle();
    paint.setStyle(Paint.Style.FILL);
    aCanvas.drawPath(toPath(pPoints), ((AndroidPen)pPen).getPaint());
    paint.setStyle(oldStyle);
  }

  @Override
  public void drawPath(DiagramPath pPath, Pen pColor) {
    aCanvas.drawPath(((AndroidPath)pPath).getPath(), ((AndroidPen)pColor).getPaint());
  }

  @Override
  public void drawFilledPath(DiagramPath pPath, Pen pPen) {
    Paint paint = ((AndroidPen) pPen).getPaint();
    Style oldStyle = paint.getStyle();
    paint.setStyle(Paint.Style.FILL);
    aCanvas.drawPath(((AndroidPath)pPath).getPath(), ((AndroidPen)pPen).getPaint());
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
