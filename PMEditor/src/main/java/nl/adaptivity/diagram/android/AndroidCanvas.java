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

package nl.adaptivity.diagram.android;

import android.graphics.*;
import android.graphics.Paint.Style;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.Theme;


public class AndroidCanvas implements IAndroidCanvas {

  private class OffsetCanvas implements IAndroidCanvas {
    /** The offset of the canvas. This is in scaled coordinates. */
    private double mXOffset;
    private double mYOffset;
    private double mScale;


    public OffsetCanvas(OffsetCanvas base, double offsetX, double offsetY, double scale) {
      mXOffset = (base.mXOffset - offsetX)*scale;
      mYOffset = (base.mYOffset - offsetY)*scale;
      mScale = base.mScale*scale;
    }

    public OffsetCanvas(OffsetCanvas base, double scale) {
      mXOffset = (base.mXOffset)*scale;
      mYOffset = (base.mYOffset)*scale;
      mScale = base.mScale*scale;
    }

    public OffsetCanvas(double scale) {
      mXOffset = 0;
      mYOffset = 0;
      mScale = scale;
    }

    private OffsetCanvas(double xOffset, double yOffset, double scale) {
      mXOffset = -xOffset;
      mYOffset = -yOffset;
      mScale = scale;
    }

    @Override
    public IAndroidCanvas childCanvas(final double offsetX, final double offsetY, double scale) {
      return new OffsetCanvas(this, offsetX, offsetY, scale);
    }

    @Override
    public IAndroidCanvas scale(double scale) {
      return new OffsetCanvas(this, scale);
    }

    @Override
    public IAndroidCanvas translate(final double x, final double y) {
      return new OffsetCanvas(mXOffset-x, mYOffset-y, mScale);
    }

    private AndroidPen scalePen(AndroidPen pen) {
      return pen.scale(mScale);
    }

    @Override
    public void drawCircle(double x, double y, double radius, AndroidPen pen) {
      AndroidCanvas.this.drawCircle(transformX(x), transformY(y), radius*mScale, scalePen(pen));
    }

    @Override
    public void drawBitmap(double left, double top, Bitmap bitmap, AndroidPen pen) {
      AndroidCanvas.this.drawBitmap(transformX(left), transformY(top), bitmap, scalePen(pen));
    }

    @Override
    public void drawFilledCircle(double x, double y, double radius, AndroidPen pen) {
      AndroidCanvas.this.drawFilledCircle(transformX(x), transformY(y), radius*mScale, scalePen(pen));
    }

    @Override
    public void drawRect(Rectangle rect, AndroidPen pen) {
      AndroidCanvas.this.drawRect(rect.offsetScaled(-mXOffset, -mYOffset, mScale), scalePen(pen));
    }

    @Override
    public void drawFilledRect(Rectangle rect, AndroidPen pen) {
      AndroidCanvas.this.drawFilledRect(rect.offsetScaled(-mXOffset, -mYOffset, mScale), scalePen(pen));
    }

    @Override
    public void drawPoly(double[] points, AndroidPen pen) {
      AndroidCanvas.this.drawPoly(transform(points), scalePen(pen));
    }

    @Override
    public void drawFilledPoly(double[] points, AndroidPen pen) {
      AndroidCanvas.this.drawFilledPoly(transform(points), scalePen(pen));
    }

    private double[] transform(double[] points) {
      double[] result = new double[points.length];
      final int len = points.length-1;
      for(int i=0; i<len;++i) {
        result[i] = transformX(points[i]);
        ++i;
        result[i] = transformY(points[i]);
      }
      return result;
    }

    public double transformX(double x) {
      return (x-mXOffset)*mScale;
    }

    public double transformY(double y) {
      return (y-mYOffset)*mScale;
    }

    @Override
    public void drawPath(AndroidPath path, AndroidPen stroke, AndroidPen fill) {
      Path transformedPath = transformPath(path);
      if (fill!=null) {
        AndroidCanvas.this.drawFilledPath(transformedPath, fill.getPaint());
      }
      if (stroke!=null) {
        AndroidCanvas.this.drawPath(transformedPath, scalePen(stroke).getPaint());
      }
    }

    private Path transformPath(AndroidPath path) {
      Path transformedPath = new Path(path.getPath());
      Matrix matrix = new Matrix();
      matrix.setScale((float)mScale, (float)mScale);
      matrix.preTranslate((float)-mXOffset, (float) -mYOffset);
      transformedPath.transform(matrix);
      return transformedPath;
    }

    @Override
    public void drawRoundRect(Rectangle rect, double rx, double ry, AndroidPen pen) {
      AndroidCanvas.this.drawRoundRect(rect.offsetScaled(-mXOffset, -mYOffset, mScale), rx*mScale, ry*mScale, scalePen(pen));
    }

    @Override
    public void drawFilledRoundRect(Rectangle rect, double rx, double ry, AndroidPen pen) {
      AndroidCanvas.this.drawFilledRoundRect(rect.offsetScaled(-mXOffset, -mYOffset, mScale), rx*mScale, ry*mScale, scalePen(pen));
    }

    @Override
    public AndroidStrategy getStrategy() {
      return AndroidStrategy.INSTANCE;
    }

    @Override
    public Theme<AndroidStrategy, AndroidPen, AndroidPath> getTheme() {
      return AndroidCanvas.this.getTheme();
    }

    @Override
    public void drawText(TextPos textPos, double left, double bottom, String text, double foldWidth, AndroidPen pen) {
      AndroidCanvas.this.drawText(textPos, transformX(left), transformY(bottom), text, foldWidth*mScale, scalePen(pen), mScale);
    }

  }

  private android.graphics.Canvas mCanvas;
  private Theme<AndroidStrategy, AndroidPen, AndroidPath> mTheme;
  
//Only for debug purposes
//  private Paint mRedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//  private Paint mGreenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

  public AndroidCanvas(android.graphics.Canvas canvas, Theme<AndroidStrategy, AndroidPen, AndroidPath> theme) {
    mCanvas = canvas;
    mTheme = theme;
//Only for debug purposes
//    mRedPaint.setColor(Color.rgb(255, 0, 0)); mRedPaint.setStyle(Style.FILL);
//    mGreenPaint.setColor(Color.rgb(0, 255, 0)); mGreenPaint.setStyle(Style.FILL);
  }

  @Override
  public IAndroidCanvas childCanvas(final double offsetX, final double offsetY, double scale) {
    return new OffsetCanvas(offsetX, offsetY, scale);
  }

  @Override
  public void drawFilledCircle(double x, double y, double radius, AndroidPen pen) {
    Paint paint = pen.getPaint();
    Style oldStyle = paint.getStyle();
    paint.setStyle(Paint.Style.FILL);
    mCanvas.drawCircle((float) x, (float) y, (float) radius, paint);
    paint.setStyle(oldStyle);
  }

  @Override
  public void drawCircle(double x, double y, double radius, AndroidPen pen) {
    mCanvas.drawCircle((float) x, (float) y, (float) radius, pen.getPaint());
  }

  @Override
  public void drawFilledRoundRect(Rectangle rect, double rx, double ry, AndroidPen pen) {
    Paint paint = pen.getPaint();
    Style oldStyle = paint.getStyle();
    paint.setStyle(Paint.Style.FILL);
    mCanvas.drawRoundRect(toRectF(rect), (float)rx, (float)ry, pen.getPaint());
    paint.setStyle(oldStyle);
  }

  @Override
  public void drawRoundRect(Rectangle rect, double rx, double ry, AndroidPen pen) {
    mCanvas.drawRoundRect(toRectF(rect), (float)rx, (float)ry, pen.getPaint());
  }

  @Override
  public void drawFilledRect(Rectangle rect, AndroidPen pen) {
    Paint paint = pen.getPaint();
    Style oldStyle = paint.getStyle();
    paint.setStyle(Paint.Style.FILL);
    mCanvas.drawRect(toRectF(rect), pen.getPaint());
    paint.setStyle(oldStyle);
  }

  @Override
  public void drawRect(Rectangle rect, AndroidPen pen) {
    mCanvas.drawRect(toRectF(rect), pen.getPaint());
  }

  @Override
  public void drawPoly(double[] points, AndroidPen pen) {
    mCanvas.drawPath(toPath(points), pen.getPaint());
  }

  @Override
  public void drawFilledPoly(double[] points, AndroidPen pen) {
    Paint paint = pen.getPaint();
    Style oldStyle = paint.getStyle();
    paint.setStyle(Paint.Style.FILL);
    mCanvas.drawPath(toPath(points), pen.getPaint());
    paint.setStyle(oldStyle);
  }

  @Override
  public void drawPath(AndroidPath path, AndroidPen stroke, AndroidPen fill) {
    if (fill!=null)
      drawFilledPath(path.getPath(), fill.getPaint());
    if (stroke!=null)
      drawPath(path.getPath(), stroke.getPaint());
  }

  void drawPath(Path path, Paint paint) {
    mCanvas.drawPath(path, paint);
  }

  private void drawFilledPath(Path path, Paint paint) {
    Style oldStyle = paint.getStyle();
    paint.setStyle(Paint.Style.FILL);
    mCanvas.drawPath(path, paint);
    paint.setStyle(oldStyle);
  }

  private static Path toPath(double[] points) {
    Path result = new Path();

    final int len = points.length - 1;
    if (len>0) {
      result.moveTo((float)points[0], (float)points[1]);
      for(int i=2; i<len; ++i) {
        result.lineTo((float)points[i], (float) points[++i]);
      }
      result.close();
    }
    return result;
  }

  private static RectF toRectF(Rectangle rect) {
    return new RectF(rect.leftf(), rect.topf(), rect.rightf(), rect.bottomf());
  }

  @Override
  public AndroidStrategy getStrategy() {
    return AndroidStrategy.INSTANCE;
  }

  @Override
  public Theme<AndroidStrategy, AndroidPen, AndroidPath> getTheme() {
    if (mTheme==null) { mTheme = new AndroidTheme(getStrategy()); }
    return mTheme;
  }

  public void setCanvas(Canvas canvas) {
    mCanvas = canvas;
  }

  @Override
  public IAndroidCanvas scale(double scale) {
    return new OffsetCanvas(scale);
  }

  @Override
  public IAndroidCanvas translate(final double left, final double right) {
    return new OffsetCanvas(left, right, 1);
  }

  @Override
  public void drawBitmap(double left, double top, Bitmap bitmap, AndroidPen pen) {
    mCanvas.drawBitmap(bitmap, (float) left, (float) top, pen.getPaint());
  }

  @Override
  public void drawText(TextPos textPos, double x, double y, String text, double foldWidth, AndroidPen pen) {
    drawText(textPos, x, y, text, foldWidth, pen, 1);
  }
  
  private void drawText(TextPos textPos, double x, double y, String text, double foldWidth, AndroidPen pen, double scale) {
    final Paint paint = pen.getPaint();
    paint.setStyle(Style.FILL);
    float left = getLeft(textPos, x, text, foldWidth, pen, scale);
    float baseline = getBaseLine(textPos, y, pen, scale);
    mCanvas.drawText(text, left, baseline, paint);
//Only for debug purposes
//    mCanvas.drawCircle(left, baseline, 3f, mRedPaint);
//    mCanvas.drawCircle((float)pX, (float)pY, 3f, mGreenPaint);
  }

  private static float getBaseLine(TextPos textPos, double y, AndroidPen pen, double scale) {
    switch (textPos) {
    case MAXTOPLEFT:
    case MAXTOP:
    case MAXTOPRIGHT:
      return (float) (y+(pen.getTextMaxAscent()*scale));
    case ASCENTLEFT:
    case ASCENT:
    case ASCENTRIGHT:
      return (float) (y+(pen.getTextAscent()*scale));
    case LEFT:
    case MIDDLE:
    case RIGHT:
      return (float) (y+(0.5*pen.getTextAscent()-0.5*pen.getTextDescent())*scale);
    case BASELINEMIDDLE:
    case BASELINERIGHT:
    case BASELINELEFT:
      return (float) y;
    case BOTTOMLEFT:
    case BOTTOMRIGHT:
    case BOTTOM:
      return (float) (y-(pen.getTextMaxDescent()*scale));
    case DESCENTLEFT:
    case DESCENTRIGHT:
    case DESCENT:
      return (float) (y-(pen.getTextDescent()*scale));
    }
    throw new IllegalArgumentException(textPos.toString());
  }

  private static float getLeft(TextPos textPos, double x, String text, double foldWidth, AndroidPen pen, double scale) {
    switch (textPos) {
    case BASELINELEFT:
    case BOTTOMLEFT:
    case LEFT:
    case MAXTOPLEFT:
    case ASCENTLEFT:
      return (float) x;
    case MAXTOP:
    case ASCENT:
    case DESCENT:
    case BASELINEMIDDLE:
    case MIDDLE:
    case BOTTOM:
      return (float) (x - ((pen.measureTextWidth(text, foldWidth)*scale)/2));
    case MAXTOPRIGHT:
    case ASCENTRIGHT:
    case DESCENTRIGHT:
    case RIGHT:
    case BASELINERIGHT:
    case BOTTOMRIGHT:
      return (float) (x - ((pen.measureTextWidth(text, foldWidth)*scale)));
    }
    throw new IllegalArgumentException(textPos.toString());
  }


}
