package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.Theme;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;


public class AndroidCanvas implements IAndroidCanvas {


  private class OffsetCanvas implements IAndroidCanvas {
    /** The offset of the canvas. This is in scaled coordinates. */
    private double aXOffset;
    private double aYOffset;
    private double aScale;


    public OffsetCanvas(OffsetCanvas pBase, Rectangle pArea, double pScale) {
      aXOffset = (pBase.aXOffset - pArea.left)*pScale;
      aYOffset = (pBase.aYOffset - pArea.top)*pScale;
      aScale = pBase.aScale*pScale;
    }

    public OffsetCanvas(OffsetCanvas pBase, double pScale) {
      aXOffset = (pBase.aXOffset)*pScale;
      aYOffset = (pBase.aYOffset)*pScale;
      aScale = pBase.aScale*pScale;
    }

    public OffsetCanvas(Rectangle pArea, double pScale) {
      aXOffset = -pArea.left* pScale;
      aYOffset = -pArea.top* pScale;
      aScale = pScale;
    }

    public OffsetCanvas(double pScale) {
      aXOffset = 0;
      aYOffset = 0;
      aScale = pScale;
    }

    @Override
    public IAndroidCanvas childCanvas(Rectangle pArea, double pScale) {
      return new OffsetCanvas(this, pArea, pScale);
    }

    @Override
    public IAndroidCanvas scale(double pScale) {
      return new OffsetCanvas(this, pScale);
    }


    private AndroidPen scalePen(AndroidPen pPen) {
      return pPen.scale(aScale);
    }

    @Override
    public void drawCircle(double pX, double pY, double pRadius, AndroidPen pPen) {
      AndroidCanvas.this.drawCircle((pX-aXOffset)*aScale, (pY - aYOffset) * aScale, pRadius*aScale, scalePen(pPen));
    }

    @Override
    public void drawBitmap(double pLeft, double pTop, Bitmap pBitmap, AndroidPen pPen) {
      AndroidCanvas.this.drawBitmap((pLeft-aXOffset)*aScale, (pTop-aYOffset)*aScale, pBitmap, scalePen(pPen));
    }

    @Override
    public void drawFilledCircle(double pX, double pY, double pRadius, AndroidPen pPen) {
      AndroidCanvas.this.drawFilledCircle((pX - aXOffset) * aScale, (pY - aYOffset) * aScale, pRadius*aScale, scalePen(pPen));
    }

    @Override
    public void drawRect(Rectangle pRect, AndroidPen pPen) {
      AndroidCanvas.this.drawRect(pRect.offsetScaled(-aXOffset, -aYOffset, aScale), scalePen(pPen));
    }

    @Override
    public void drawFilledRect(Rectangle pRect, AndroidPen pPen) {
      AndroidCanvas.this.drawFilledRect(pRect.offsetScaled(-aXOffset, -aYOffset, aScale), scalePen(pPen));
    }

    @Override
    public void drawPoly(double[] pPoints, AndroidPen pPen) {
      AndroidCanvas.this.drawPoly(transform(pPoints), scalePen(pPen));
    }

    @Override
    public void drawFilledPoly(double[] pPoints, AndroidPen pPen) {
      AndroidCanvas.this.drawFilledPoly(transform(pPoints), scalePen(pPen));
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
    public void drawPath(AndroidPath pPath, AndroidPen pStroke, AndroidPen pFill) {
      Path transformedPath = transformPath(pPath);
      if (pFill!=null) {
        AndroidCanvas.this.drawFilledPath(transformedPath, pFill.getPaint());
      }
      if (pStroke!=null) {
        AndroidCanvas.this.drawPath(transformedPath, scalePen(pStroke).getPaint());
      }
    }

    private Path transformPath(AndroidPath pPath) {
      Path transformedPath = new Path(pPath.getPath());
      Matrix matrix = new Matrix();
      matrix.setScale((float)aScale, (float)aScale);
      matrix.preTranslate((float)-aXOffset, (float) -aYOffset);
      transformedPath.transform(matrix);
      return transformedPath;
    }

    @Override
    public void drawRoundRect(Rectangle pRect, double pRx, double pRy, AndroidPen pPen) {
      AndroidCanvas.this.drawRoundRect(pRect.offsetScaled(-aXOffset, -aYOffset, aScale), pRx*aScale, pRy*aScale, scalePen(pPen));
    }

    @Override
    public void drawFilledRoundRect(Rectangle pRect, double pRx, double pRy, AndroidPen pPen) {
      AndroidCanvas.this.drawFilledRoundRect(pRect.offsetScaled(-aXOffset, -aYOffset, aScale), pRx*aScale, pRy*aScale, scalePen(pPen));
    }

    @Override
    public AndroidStrategy getStrategy() {
      return AndroidStrategy.INSTANCE;
    }

    @Override
    public Theme<AndroidStrategy, AndroidPen, AndroidPath> getTheme() {
      return AndroidCanvas.this.getTheme();
    }

  }

  private android.graphics.Canvas aCanvas;
  private Theme<AndroidStrategy, AndroidPen, AndroidPath> aTheme;

  public AndroidCanvas(android.graphics.Canvas pCanvas, Theme<AndroidStrategy, AndroidPen, AndroidPath> pTheme) {
    aCanvas = pCanvas;
    aTheme = pTheme;
  }

  @Override
  public IAndroidCanvas childCanvas(Rectangle pArea, double pScale) {
    return new OffsetCanvas(pArea, pScale);
  }

  @Override
  public void drawFilledCircle(double pX, double pY, double pRadius, AndroidPen pPen) {
    Paint paint = pPen.getPaint();
    Style oldStyle = paint.getStyle();
    paint.setStyle(Paint.Style.FILL);
    aCanvas.drawCircle((float) pX, (float) pY, (float) pRadius, paint);
    paint.setStyle(oldStyle);
  }

  @Override
  public void drawCircle(double pX, double pY, double pRadius, AndroidPen pPen) {
    aCanvas.drawCircle((float) pX, (float) pY, (float) pRadius, pPen.getPaint());
  }

  @Override
  public void drawFilledRoundRect(Rectangle pRect, double pRx, double pRy, AndroidPen pPen) {
    Paint paint = pPen.getPaint();
    Style oldStyle = paint.getStyle();
    paint.setStyle(Paint.Style.FILL);
    aCanvas.drawRoundRect(toRectF(pRect), (float)pRx, (float)pRy, pPen.getPaint());
    paint.setStyle(oldStyle);
  }

  @Override
  public void drawRoundRect(Rectangle pRect, double pRx, double pRy, AndroidPen pPen) {
    aCanvas.drawRoundRect(toRectF(pRect), (float)pRx, (float)pRy, pPen.getPaint());
  }

  @Override
  public void drawFilledRect(Rectangle pRect, AndroidPen pPen) {
    Paint paint = pPen.getPaint();
    Style oldStyle = paint.getStyle();
    paint.setStyle(Paint.Style.FILL);
    aCanvas.drawRect(toRectF(pRect), pPen.getPaint());
    paint.setStyle(oldStyle);
  }

  @Override
  public void drawRect(Rectangle pRect, AndroidPen pPen) {
    aCanvas.drawRect(toRectF(pRect), pPen.getPaint());
  }

  @Override
  public void drawPoly(double[] pPoints, AndroidPen pPen) {
    aCanvas.drawPath(toPath(pPoints), pPen.getPaint());
  }

  @Override
  public void drawFilledPoly(double[] pPoints, AndroidPen pPen) {
    Paint paint = pPen.getPaint();
    Style oldStyle = paint.getStyle();
    paint.setStyle(Paint.Style.FILL);
    aCanvas.drawPath(toPath(pPoints), pPen.getPaint());
    paint.setStyle(oldStyle);
  }

  @Override
  public void drawPath(AndroidPath pPath, AndroidPen pStroke, AndroidPen pFill) {
    if (pFill!=null)
      drawFilledPath(pPath.getPath(), pFill.getPaint());
    if (pStroke!=null)
      drawPath(pPath.getPath(), pStroke.getPaint());
  }

  void drawPath(Path path, Paint paint) {
    aCanvas.drawPath(path, paint);
  }

  private void drawFilledPath(Path path, Paint paint) {
    Style oldStyle = paint.getStyle();
    paint.setStyle(Paint.Style.FILL);
    aCanvas.drawPath(path, paint);
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

  @Override
  public AndroidStrategy getStrategy() {
    return AndroidStrategy.INSTANCE;
  }

  @Override
  public Theme<AndroidStrategy, AndroidPen, AndroidPath> getTheme() {
    if (aTheme==null) { aTheme = new AndroidTheme(getStrategy()); }
    return aTheme;
  }

  public void setCanvas(Canvas pCanvas) {
    aCanvas = pCanvas;
  }

  @Override
  public IAndroidCanvas scale(double pScale) {
    return new OffsetCanvas(pScale);
  }

  @Override
  public void drawBitmap(double pLeft, double pTop, Bitmap pBitmap, AndroidPen pPen) {
    aCanvas.drawBitmap(pBitmap, (float) pLeft, (float) pTop, pPen.getPaint());
  }

}
