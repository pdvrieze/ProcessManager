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


    public OffsetCanvas(final OffsetCanvas base, final double offsetX, final double offsetY, final double scale) {
      mXOffset = (base.mXOffset - offsetX)*scale;
      mYOffset = (base.mYOffset - offsetY)*scale;
      mScale = base.mScale*scale;
    }

    public OffsetCanvas(final OffsetCanvas base, final double scale) {
      mXOffset = (base.mXOffset)*scale;
      mYOffset = (base.mYOffset)*scale;
      mScale = base.mScale*scale;
    }

    public OffsetCanvas(final double scale) {
      mXOffset = 0;
      mYOffset = 0;
      mScale = scale;
    }

    private OffsetCanvas(final double xOffset, final double yOffset, final double scale) {
      mXOffset = -xOffset;
      mYOffset = -yOffset;
      mScale = scale;
    }

    @Override
    public IAndroidCanvas childCanvas(final double offsetX, final double offsetY, final double scale) {
      return new OffsetCanvas(this, offsetX, offsetY, scale);
    }

    @Override
    public IAndroidCanvas scale(final double scale) {
      return new OffsetCanvas(this, scale);
    }

    @Override
    public IAndroidCanvas translate(final double x, final double y) {
      return new OffsetCanvas(mXOffset-x, mYOffset-y, mScale);
    }

    private AndroidPen scalePen(final AndroidPen pen) {
      return pen.scale(mScale);
    }

    @Override
    public void drawCircle(final double x, final double y, final double radius, final AndroidPen pen) {
      AndroidCanvas.this.drawCircle(transformX(x), transformY(y), radius*mScale, scalePen(pen));
    }

    @Override
    public void drawBitmap(final double left, final double top, final Bitmap bitmap, final AndroidPen pen) {
      AndroidCanvas.this.drawBitmap(transformX(left), transformY(top), bitmap, scalePen(pen));
    }

    @Override
    public void drawFilledCircle(final double x, final double y, final double radius, final AndroidPen pen) {
      AndroidCanvas.this.drawFilledCircle(transformX(x), transformY(y), radius*mScale, scalePen(pen));
    }

    @Override
    public void drawRect(final Rectangle rect, final AndroidPen pen) {
      AndroidCanvas.this.drawRect(rect.offsetScaled(-mXOffset, -mYOffset, mScale), scalePen(pen));
    }

    @Override
    public void drawFilledRect(final Rectangle rect, final AndroidPen pen) {
      AndroidCanvas.this.drawFilledRect(rect.offsetScaled(-mXOffset, -mYOffset, mScale), scalePen(pen));
    }

    @Override
    public void drawPoly(final double[] points, final AndroidPen pen) {
      AndroidCanvas.this.drawPoly(transform(points), scalePen(pen));
    }

    @Override
    public void drawFilledPoly(final double[] points, final AndroidPen pen) {
      AndroidCanvas.this.drawFilledPoly(transform(points), scalePen(pen));
    }

    private double[] transform(final double[] points) {
      final double[] result = new double[points.length];
      final int      len    = points.length-1;
      for(int i=0; i<len;++i) {
        result[i] = transformX(points[i]);
        ++i;
        result[i] = transformY(points[i]);
      }
      return result;
    }

    public double transformX(final double x) {
      return (x-mXOffset)*mScale;
    }

    public double transformY(final double y) {
      return (y-mYOffset)*mScale;
    }

    @Override
    public void drawPath(final AndroidPath path, final AndroidPen stroke, final AndroidPen fill) {
      final Path transformedPath = transformPath(path);
      if (fill!=null) {
        AndroidCanvas.this.drawFilledPath(transformedPath, fill.getPaint());
      }
      if (stroke!=null) {
        AndroidCanvas.this.drawPath(transformedPath, scalePen(stroke).getPaint());
      }
    }

    private Path transformPath(final AndroidPath path) {
      final Path   transformedPath = new Path(path.getPath());
      final Matrix matrix          = new Matrix();
      matrix.setScale((float)mScale, (float)mScale);
      matrix.preTranslate((float)-mXOffset, (float) -mYOffset);
      transformedPath.transform(matrix);
      return transformedPath;
    }

    @Override
    public void drawRoundRect(final Rectangle rect, final double rx, final double ry, final AndroidPen pen) {
      AndroidCanvas.this.drawRoundRect(rect.offsetScaled(-mXOffset, -mYOffset, mScale), rx*mScale, ry*mScale, scalePen(pen));
    }

    @Override
    public void drawFilledRoundRect(final Rectangle rect, final double rx, final double ry, final AndroidPen pen) {
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
    public void drawText(final TextPos textPos, final double left, final double bottom, final String text, final double foldWidth, final AndroidPen pen) {
      AndroidCanvas.this.drawText(textPos, transformX(left), transformY(bottom), text, foldWidth*mScale, scalePen(pen), mScale);
    }

  }

  private android.graphics.Canvas mCanvas;
  private Theme<AndroidStrategy, AndroidPen, AndroidPath> mTheme;
  
//Only for debug purposes
//  private Paint mRedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//  private Paint mGreenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

  public AndroidCanvas(final android.graphics.Canvas canvas, final Theme<AndroidStrategy, AndroidPen, AndroidPath> theme) {
    mCanvas = canvas;
    mTheme = theme;
//Only for debug purposes
//    mRedPaint.setColor(Color.rgb(255, 0, 0)); mRedPaint.setStyle(Style.FILL);
//    mGreenPaint.setColor(Color.rgb(0, 255, 0)); mGreenPaint.setStyle(Style.FILL);
  }

  @Override
  public IAndroidCanvas childCanvas(final double offsetX, final double offsetY, final double scale) {
    return new OffsetCanvas(offsetX, offsetY, scale);
  }

  @Override
  public void drawFilledCircle(final double x, final double y, final double radius, final AndroidPen pen) {
    final Paint paint    = pen.getPaint();
    final Style oldStyle = paint.getStyle();
    paint.setStyle(Paint.Style.FILL);
    mCanvas.drawCircle((float) x, (float) y, (float) radius, paint);
    paint.setStyle(oldStyle);
  }

  @Override
  public void drawCircle(final double x, final double y, final double radius, final AndroidPen pen) {
    mCanvas.drawCircle((float) x, (float) y, (float) radius, pen.getPaint());
  }

  @Override
  public void drawFilledRoundRect(final Rectangle rect, final double rx, final double ry, final AndroidPen pen) {
    final Paint paint    = pen.getPaint();
    final Style oldStyle = paint.getStyle();
    paint.setStyle(Paint.Style.FILL);
    mCanvas.drawRoundRect(toRectF(rect), (float)rx, (float)ry, pen.getPaint());
    paint.setStyle(oldStyle);
  }

  @Override
  public void drawRoundRect(final Rectangle rect, final double rx, final double ry, final AndroidPen pen) {
    mCanvas.drawRoundRect(toRectF(rect), (float)rx, (float)ry, pen.getPaint());
  }

  @Override
  public void drawFilledRect(final Rectangle rect, final AndroidPen pen) {
    final Paint paint    = pen.getPaint();
    final Style oldStyle = paint.getStyle();
    paint.setStyle(Paint.Style.FILL);
    mCanvas.drawRect(toRectF(rect), pen.getPaint());
    paint.setStyle(oldStyle);
  }

  @Override
  public void drawRect(final Rectangle rect, final AndroidPen pen) {
    mCanvas.drawRect(toRectF(rect), pen.getPaint());
  }

  @Override
  public void drawPoly(final double[] points, final AndroidPen pen) {
    mCanvas.drawPath(toPath(points), pen.getPaint());
  }

  @Override
  public void drawFilledPoly(final double[] points, final AndroidPen pen) {
    final Paint paint    = pen.getPaint();
    final Style oldStyle = paint.getStyle();
    paint.setStyle(Paint.Style.FILL);
    mCanvas.drawPath(toPath(points), pen.getPaint());
    paint.setStyle(oldStyle);
  }

  @Override
  public void drawPath(final AndroidPath path, final AndroidPen stroke, final AndroidPen fill) {
    if (fill!=null)
      drawFilledPath(path.getPath(), fill.getPaint());
    if (stroke!=null)
      drawPath(path.getPath(), stroke.getPaint());
  }

  void drawPath(final Path path, final Paint paint) {
    mCanvas.drawPath(path, paint);
  }

  private void drawFilledPath(final Path path, final Paint paint) {
    final Style oldStyle = paint.getStyle();
    paint.setStyle(Paint.Style.FILL);
    mCanvas.drawPath(path, paint);
    paint.setStyle(oldStyle);
  }

  private static Path toPath(final double[] points) {
    final Path result = new Path();

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

  private static RectF toRectF(final Rectangle rect) {
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

  public void setCanvas(final Canvas canvas) {
    mCanvas = canvas;
  }

  @Override
  public IAndroidCanvas scale(final double scale) {
    return new OffsetCanvas(scale);
  }

  @Override
  public IAndroidCanvas translate(final double left, final double right) {
    return new OffsetCanvas(left, right, 1);
  }

  @Override
  public void drawBitmap(final double left, final double top, final Bitmap bitmap, final AndroidPen pen) {
    mCanvas.drawBitmap(bitmap, (float) left, (float) top, pen.getPaint());
  }

  @Override
  public void drawText(final TextPos textPos, final double x, final double y, final String text, final double foldWidth, final AndroidPen pen) {
    drawText(textPos, x, y, text, foldWidth, pen, 1);
  }
  
  private void drawText(final TextPos textPos, final double x, final double y, final String text, final double foldWidth, final AndroidPen pen, final double scale) {
    final Paint paint = pen.getPaint();
    paint.setStyle(Style.FILL);
    final float left     = getLeft(textPos, x, text, foldWidth, pen, scale);
    final float baseline = getBaseLine(textPos, y, pen, scale);
    mCanvas.drawText(text, left, baseline, paint);
//Only for debug purposes
//    mCanvas.drawCircle(left, baseline, 3f, mRedPaint);
//    mCanvas.drawCircle((float)pX, (float)pY, 3f, mGreenPaint);
  }

  private static float getBaseLine(final TextPos textPos, final double y, final AndroidPen pen, final double scale) {
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

  private static float getLeft(final TextPos textPos, final double x, final String text, final double foldWidth, final AndroidPen pen, final double scale) {
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
