/*
 * Copyright (c) 2017.
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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.diagram.android;

import android.graphics.*;
import android.graphics.Paint.Style;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.Theme;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


public class AndroidCanvas implements IAndroidCanvas {

    private android.graphics.Canvas                         mCanvas;
    private Theme<AndroidStrategy, AndroidPen, AndroidPath> mTheme;

    public AndroidCanvas(final android.graphics.Canvas canvas,
                         final Theme<AndroidStrategy, AndroidPen, AndroidPath> theme) {
        mCanvas = canvas;
        mTheme = theme;
//Only for debug purposes
//    mRedPaint.setColor(Color.rgb(255, 0, 0)); mRedPaint.setStyle(Style.FILL);
//    mGreenPaint.setColor(Color.rgb(0, 255, 0)); mGreenPaint.setStyle(Style.FILL);
    }

//Only for debug purposes
//  private Paint mRedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//  private Paint mGreenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public void setCanvas(final Canvas canvas) {
        mCanvas = canvas;
    }

    @SuppressWarnings("UnclearExpression")
    private class OffsetCanvas implements IAndroidCanvas {

        /** The offset of the canvas. This is in scaled coordinates. */
        private final double mXOffset;
        private final double mYOffset;
        private final double mScale;


        public OffsetCanvas(final OffsetCanvas base, final double offsetX, final double offsetY, final double scale) {
            mXOffset = (base.mXOffset - offsetX) * scale;
            mYOffset = (base.mYOffset - offsetY) * scale;
            mScale = base.mScale * scale;
        }

        public OffsetCanvas(final OffsetCanvas base, final double scale) {
            mXOffset = (base.mXOffset) * scale;
            mYOffset = (base.mYOffset) * scale;
            mScale = base.mScale * scale;
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

        @NonNull
        @Override
        public IAndroidCanvas scale(final double scale) {
            return new OffsetCanvas(this, scale);
        }

        @NonNull
        @Override
        public AndroidStrategy getStrategy() {
            return AndroidStrategy.INSTANCE;
        }        @NonNull
        @Override
        public IAndroidCanvas childCanvas(final double offsetX, final double offsetY, final double scale) {
            return new OffsetCanvas(this, offsetX, offsetY, scale);
        }



        @NonNull
        @Override
        public IAndroidCanvas translate(final double dx, final double dy) {
            return new OffsetCanvas(mXOffset - dx, mYOffset - dy, mScale);
        }

        @Nullable
        @Contract("null -> null; !null -> !null")
        private AndroidPen scalePen(@Nullable final AndroidPen pen) {
            return pen == null ? null : pen.scale(mScale);
        }

        @Override
        public void drawCircle(final double x, final double y, final double radius, @NonNull final AndroidPen stroke) {
            AndroidCanvas.this.drawCircle(transformX(x), transformY(y), radius * mScale, scalePen(stroke));
        }

        @Override
        public void drawFilledCircle(final double x,
                                     final double y,
                                     final double radius,
                                     @NonNull final AndroidPen fill) {
            AndroidCanvas.this.drawFilledCircle(transformX(x), transformY(y), radius * mScale, fill);
        }

        @Override
        public void drawCircle(final double x, final double y, final double radius, @Nullable final AndroidPen stroke,
                               @Nullable final AndroidPen fill) {
            if (fill != null) {
                AndroidCanvas.this.drawFilledCircle(transformX(x), transformY(y), radius * mScale, fill);
            }
            if (stroke != null) {
                AndroidCanvas.this.drawCircle(transformX(x), transformY(y), radius * mScale, scalePen(stroke));
            }
        }

        @Override
        public void drawBitmap(final double left,
                               final double top,
                               final Bitmap bitmap,
                               @NonNull final AndroidPen pen) {
            AndroidCanvas.this.drawBitmap(transformX(left), transformY(top), bitmap, scalePen(pen));
        }

        @Override
        public void drawRect(@NonNull final Rectangle rect, @NonNull final AndroidPen stroke) {
            AndroidCanvas.this.drawRect(rect.offsetScaled(-mXOffset, -mYOffset, mScale), scalePen(stroke));
        }

        @Override
        public void drawFilledRect(@NonNull final Rectangle rect, @NonNull final AndroidPen fill) {
            AndroidCanvas.this.drawFilledRect(rect.offsetScaled(-mXOffset, -mYOffset, mScale), scalePen(fill));
        }

        @Override
        public void drawRect(@NonNull final Rectangle rect,
                             @Nullable final AndroidPen stroke,
                             @Nullable final AndroidPen fill) {
            if (fill != null) {
                AndroidCanvas.this.drawFilledRect(rect.offsetScaled(-mXOffset, -mYOffset, mScale), fill);
            }

            if (stroke != null) {
                AndroidCanvas.this.drawRect(rect.offsetScaled(-mXOffset, -mYOffset, mScale), scalePen(stroke));
            }
        }

        @Override
        public void drawPoly(@NotNull final double[] points,
                             @org.jetbrains.annotations.Nullable final AndroidPen stroke,
                             @org.jetbrains.annotations.Nullable final AndroidPen fill) {
            AndroidCanvas.this.drawPoly(transform(points), scalePen(stroke), fill);
        }

        @NonNull
        private double[] transform(final double[] points) {
            final double[] result = new double[points.length];
            final int      len    = points.length - 1;
            for (int i = 0; i < len; ++i) {
                result[i] = transformX(points[i]);
                ++i;
                result[i] = transformY(points[i]);
            }
            return result;
        }

        public double transformX(final double x) {
            return (x - mXOffset) * mScale;
        }

        public double transformY(final double y) {
            return (y - mYOffset) * mScale;
        }

        @Override
        public void drawPath(@NonNull final AndroidPath path,
                             @Nullable final AndroidPen stroke,
                             @Nullable final AndroidPen fill) {
            final Path transformedPath = transformPath(path);
            if (fill != null) {
                AndroidCanvas.this.drawFilledPath(transformedPath, fill.getPaint());
            }
            if (stroke != null) {
                AndroidCanvas.this.drawPath(transformedPath, scalePen(stroke).getPaint());
            }
        }

        @NonNull
        private Path transformPath(final AndroidPath path) {
            final Path   transformedPath = new Path(path.getPath());
            final Matrix matrix          = new Matrix();
            matrix.setScale((float) mScale, (float) mScale);
            matrix.preTranslate((float) -mXOffset, (float) -mYOffset);
            transformedPath.transform(matrix);
            return transformedPath;
        }

        @Override
        public void drawRoundRect(@NonNull final Rectangle rect,
                                  final double rx,
                                  final double ry,
                                  @NonNull final AndroidPen stroke) {
            AndroidCanvas.this.drawRoundRect(rect.offsetScaled(-mXOffset, -mYOffset, mScale), rx * mScale, ry * mScale,
                                             scalePen(
                                                 stroke));
        }

        @Override
        public void drawFilledRoundRect(@NonNull final Rectangle rect,
                                        final double rx,
                                        final double ry,
                                        @NonNull final AndroidPen fill) {
            AndroidCanvas.this.drawFilledRoundRect(rect.offsetScaled(-mXOffset, -mYOffset, mScale), rx * mScale,
                                                   ry * mScale, scalePen(
                    fill));
        }

        @Override
        public void drawRoundRect(@NonNull final Rectangle rect,
                                  final double rx,
                                  final double ry,
                                  @Nullable final AndroidPen stroke,
                                  @Nullable final AndroidPen fill) {
            if (fill != null) {
                AndroidCanvas.this.drawFilledRoundRect(rect.offsetScaled(-mXOffset, -mYOffset, mScale), rx * mScale,
                                                       ry * mScale, scalePen(fill));
            }
            if (stroke != null) {
                AndroidCanvas.this.drawRoundRect(rect.offsetScaled(-mXOffset, -mYOffset, mScale), rx * mScale,
                                                 ry * mScale, scalePen(stroke));
            }
        }


        @NonNull
        @Override
        public Theme<AndroidStrategy, AndroidPen, AndroidPath> getTheme() {
            return AndroidCanvas.this.getTheme();
        }

        @Override
        public void drawText(@NonNull final TextPos textPos,
                             final double left,
                             final double baselineY,
                             @NonNull final String text,
                             final double foldWidth,
                             @NonNull final AndroidPen pen) {
            AndroidCanvas.this.drawText(textPos, transformX(left), transformY(baselineY), text, foldWidth * mScale,
                                        scalePen(pen), mScale);
        }
    }

    @NonNull
    @Override
    public IAndroidCanvas childCanvas(final double offsetX, final double offsetY, final double scale) {
        return new OffsetCanvas(offsetX, offsetY, scale);
    }

    @Override
    public void drawFilledCircle(final double x, final double y, final double radius, @NonNull final AndroidPen fill) {
        final Paint paint    = fill.getPaint();
        final Style oldStyle = paint.getStyle();
        paint.setStyle(Paint.Style.FILL);
        mCanvas.drawCircle((float) x, (float) y, (float) radius, paint);
        paint.setStyle(oldStyle);
    }

    @Override
    public void drawCircle(final double x, final double y, final double radius, @NonNull final AndroidPen stroke) {
        mCanvas.drawCircle((float) x, (float) y, (float) radius, stroke.getPaint());
    }

    @Override
    public void drawCircle(final double x,
                           final double y,
                           final double radius,
                           @Nullable final AndroidPen stroke,
                           @Nullable final AndroidPen fill) {
        if (fill != null) { drawFilledCircle(x, y, radius, fill); }
        if (stroke != null) { drawCircle(x, y, radius, stroke); }
    }

    @Override
    public void drawFilledRoundRect(@NonNull final Rectangle rect,
                                    final double rx,
                                    final double ry,
                                    @NonNull final AndroidPen fill) {
        final Paint paint    = fill.getPaint();
        final Style oldStyle = paint.getStyle();
        paint.setStyle(Paint.Style.FILL);
        mCanvas.drawRoundRect(toRectF(rect), (float) rx, (float) ry, fill.getPaint());
        paint.setStyle(oldStyle);
    }

    @Override
    public void drawRoundRect(@NonNull final Rectangle rect,
                              final double rx,
                              final double ry,
                              @NonNull final AndroidPen stroke) {
        mCanvas.drawRoundRect(toRectF(rect), (float) rx, (float) ry, stroke.getPaint());
    }

    @Override
    public void drawRoundRect(@NonNull final Rectangle rect,
                              final double rx,
                              final double ry,
                              @Nullable final AndroidPen stroke,
                              @Nullable final AndroidPen fill) {
        if (fill != null) { drawFilledRoundRect(rect, rx, ry, fill); }
        if (stroke != null) { drawRoundRect(rect, rx, ry, stroke); }
    }

    @Override
    public void drawFilledRect(@NonNull final Rectangle rect, @NonNull final AndroidPen fill) {
        final Paint paint    = fill.getPaint();
        final Style oldStyle = paint.getStyle();
        paint.setStyle(Paint.Style.FILL);
        mCanvas.drawRect(toRectF(rect), fill.getPaint());
        paint.setStyle(oldStyle);
    }

    @Override
    public void drawRect(@NonNull final Rectangle rect, @NonNull final AndroidPen stroke) {
        mCanvas.drawRect(toRectF(rect), stroke.getPaint());
    }

    @Override
    public void drawRect(@NonNull final Rectangle rect,
                         @Nullable final AndroidPen stroke,
                         @Nullable final AndroidPen fill) {
        if (fill != null) { drawFilledRect(rect, fill); }
        if (stroke != null) { drawRect(rect, stroke); }
    }

    @Override
    public void drawPoly(@NotNull final double[] points,
                         @org.jetbrains.annotations.Nullable final AndroidPen stroke,
                         @org.jetbrains.annotations.Nullable final AndroidPen fill) {

        final Path androidPath = toPath(points);
        if (fill != null) {
            final Paint fillPaint = fill.getPaint();
            final Style oldStyle  = fillPaint.getStyle();
            fillPaint.setStyle(Paint.Style.FILL);
            mCanvas.drawPath(androidPath, fill.getPaint());
            fillPaint.setStyle(oldStyle);
        }
        if (stroke != null) {
            mCanvas.drawPath(androidPath, stroke.getPaint());
        }
    }

    @Override
    public void drawPath(@NonNull final AndroidPath path,
                         @NonNull final AndroidPen stroke,
                         @org.jetbrains.annotations.Nullable final AndroidPen fill) {
        if (fill != null) { drawFilledPath(path.getPath(), fill.getPaint()); }
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

    @NonNull
    private static Path toPath(final double[] points) {
        final Path result = new Path();

        final int len = points.length - 1;
        if (len > 0) {
            result.moveTo((float) points[0], (float) points[1]);
            for (int i = 2; i < len; ++i) {
                result.lineTo((float) points[i], (float) points[++i]);
            }
            result.close();
        }
        return result;
    }

    private static RectF toRectF(final Rectangle rect) {
        return new RectF(rect.leftf(), rect.topf(), rect.rightf(), rect.bottomf());
    }

    @NonNull
    @Override
    public AndroidStrategy getStrategy() {
        return AndroidStrategy.INSTANCE;
    }

    @NonNull
    @Override
    public Theme<AndroidStrategy, AndroidPen, AndroidPath> getTheme() {
        if (mTheme == null) { mTheme = new AndroidTheme(getStrategy()); }
        return mTheme;
    }


    @NonNull
    @Override
    public IAndroidCanvas scale(final double scale) {
        return new OffsetCanvas(scale);
    }

    @NonNull
    @Override
    public IAndroidCanvas translate(final double dx, final double dy) {
        if (dx == 0d && dy == 0d) { return this; }
        return new OffsetCanvas(dx, dy, 1);
    }

    @Override
    public void drawBitmap(final double left, final double top, final Bitmap bitmap, @NonNull final AndroidPen pen) {
        mCanvas.drawBitmap(bitmap, (float) left, (float) top, pen.getPaint());
    }

    @Override
    public void drawText(@NonNull final TextPos textPos,
                         final double left,
                         final double baselineY,
                         @NonNull final String text,
                         final double foldWidth,
                         @NonNull final AndroidPen pen) {
        drawText(textPos, left, baselineY, text, foldWidth, pen, 1);
    }

    private void drawText(@NonNull final TextPos textPos,
                          final double x,
                          final double y,
                          final String text,
                          final double foldWidth,
                          final AndroidPen pen,
                          final double scale) {
        final Paint paint = pen.getPaint();
        paint.setStyle(Style.FILL);
        final float left     = getLeft(textPos, x, text, foldWidth, pen, scale);
        final float baseline = getBaseLine(textPos, y, pen, scale);
        mCanvas.drawText(text, left, baseline, paint);
//Only for debug purposes
//    mCanvas.drawCircle(left, baseline, 3f, mRedPaint);
//    mCanvas.drawCircle((float)pX, (float)pY, 3f, mGreenPaint);
    }

    private static float getBaseLine(final TextPos textPos,
                                     final double y,
                                     @NonNull final Pen<?> pen,
                                     final double scale) {
        switch (textPos) {
            case MAXTOPLEFT:
            case MAXTOP:
            case MAXTOPRIGHT:
                return (float) (y + (pen.getTextMaxAscent() * scale));
            case ASCENTLEFT:
            case ASCENT:
            case ASCENTRIGHT:
                return (float) (y + (pen.getTextAscent() * scale));
            case LEFT:
            case MIDDLE:
            case RIGHT:
                return (float) (y + (((0.5 * pen.getTextAscent()) - (0.5 * pen.getTextDescent())) * scale));
            case BASELINEMIDDLE:
            case BASELINERIGHT:
            case BASELINELEFT:
                return (float) y;
            case BOTTOMLEFT:
            case BOTTOMRIGHT:
            case BOTTOM:
                return (float) (y - (pen.getTextMaxDescent() * scale));
            case DESCENTLEFT:
            case DESCENTRIGHT:
            case DESCENT:
                return (float) (y - (pen.getTextDescent() * scale));
        }
        throw new IllegalArgumentException(textPos.toString());
    }

    private static float getLeft(final TextPos textPos,
                                 final double x,
                                 final String text,
                                 final double foldWidth,
                                 @NonNull final AndroidPen pen,
                                 final double scale) {
        switch (textPos) {
            case BASELINELEFT:
            case BOTTOMLEFT:
            case LEFT:
            case MAXTOPLEFT:
            case ASCENTLEFT:
                return (float) x;
            case ASCENT:
            case DESCENT:
            case BASELINEMIDDLE:
            case MAXTOP:
            case MIDDLE:
            case BOTTOM:
                return (float) (x - ((pen.measureTextWidth(text, foldWidth) * scale) / 2));
            case MAXTOPRIGHT:
            case ASCENTRIGHT:
            case DESCENTLEFT:
            case DESCENTRIGHT:
            case RIGHT:
            case BASELINERIGHT:
            case BOTTOMRIGHT:
                return (float) (x - ((pen.measureTextWidth(text, foldWidth) * scale)));
        }
        throw new IllegalArgumentException(textPos.toString());
    }
}
