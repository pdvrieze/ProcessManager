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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.diagram.svg;

import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.Theme;
import nl.adaptivity.diagram.svg.TextMeasurer.MeasureInfo;
import nl.adaptivity.lang.Doubles;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlWriter;
import org.jetbrains.annotations.NotNull;

import javax.xml.XMLConstants;

import java.util.ArrayList;
import java.util.List;


public class SVGCanvas<M extends MeasureInfo> implements Canvas<SVGStrategy<M>, SVGPen<M>, SVGPath> {

  interface IPaintedElem {

    void serialize(XmlWriter out) throws XmlException;

    Rectangle getBounds(Rectangle dest);
  }

  private static abstract class PaintedElem<M extends MeasureInfo> implements IPaintedElem {
    final SVGPen<M> stroke;
    final SVGPen<M> fill;

    PaintedElem(final SVGPen<M> stroke, final SVGPen<M> fill) {
      this.stroke = stroke;
      this.fill = fill;
    }

    void serializeFill(final XmlWriter out) throws XmlException {
      serializeStyle(out, null, fill, null);
    }

    void serializeStroke(final XmlWriter out) throws XmlException {
      serializeStyle(out, stroke, null, null);
    }

    void serializeStrokeFill(final XmlWriter out) throws XmlException {
      serializeStyle(out, stroke, fill, null);
    }
  }

  private static abstract class BaseRect<M extends MeasureInfo> extends PaintedElem<M> {
    final Rectangle mBounds;

    BaseRect(final Rectangle bounds, final SVGPen<M> stroke, final SVGPen<M> fill) {
      super(stroke, fill);
      mBounds = bounds.clone();
    }

    void serializeRect(final XmlWriter out) throws XmlException {
      out.startTag(SVG_NAMESPACE, "rect", null);
      out.attribute(null, "x", null, Double.toString(mBounds.left));
      out.attribute(null, "y", null, Double.toString(mBounds.top));
      out.attribute(null, "width", null, Double.toString(mBounds.width));
      out.attribute(null, "height", null, Double.toString(mBounds.height));
    }

    @Override
    public Rectangle getBounds(final Rectangle dest) {
      final double strokeWidth = stroke.getStrokeWidth();
      double delta = strokeWidth / 2;
      dest.set(mBounds.left-delta, mBounds.top-delta, mBounds.width+strokeWidth, mBounds.height+strokeWidth);
      return dest;
    }
  }

  private static class Rect<M extends MeasureInfo> extends BaseRect<M> {

    public Rect(final Rectangle bounds, final SVGPen<M> stroke) {
      super(bounds, stroke, null);
    }

    @Override
    public void serialize(final XmlWriter out) throws XmlException {
      serializeRect(out);
      serializeStroke(out);
      out.endTag(SVG_NAMESPACE, "rect", null);
    }

  }

  private static class FilledRect<M extends MeasureInfo> extends BaseRect<M> {

    FilledRect(final Rectangle bounds, final SVGPen<M> fill) {
      super(bounds, null, fill);
    }

    FilledRect(final Rectangle bounds, final SVGPen<M> stroke, final SVGPen<M> fill) {
      super(bounds, stroke, fill);
    }

    @Override
    public void serialize(final XmlWriter out) throws XmlException {
      serializeRect(out);
      serializeStrokeFill(out);
      out.endTag(SVG_NAMESPACE, "rect", null);
    }

  }

  private static abstract class BaseRoundRect<M extends MeasureInfo> extends BaseRect<M> {
    final double mRx;
    final double mRy;

    BaseRoundRect(final Rectangle bounds, final double rx, final double ry, final SVGPen<M> stroke, final SVGPen<M> fill) {
      super(bounds, stroke, fill);
      mRx = rx;
      mRy = ry;
    }

    void serializeRoundRect(final XmlWriter out) throws XmlException {
      serializeRect(out);
      out.attribute(null, "rx", null, Double.toString(mRx));
      out.attribute(null, "ry", null, Double.toString(mRy));
    }
  }

  private static class RoundRect<M extends MeasureInfo> extends BaseRoundRect<M> {

    RoundRect(final Rectangle bounds, final double rx, final double ry, final SVGPen<M> stroke) {
      super(bounds, rx, ry, stroke, null);
    }

    @Override
    public void serialize(final XmlWriter out) throws XmlException {
      serializeRoundRect(out);
      serializeStroke(out);
      out.endTag(SVG_NAMESPACE, "rect", null);
    }

  }

  private static class FilledRoundRect<M extends MeasureInfo> extends BaseRoundRect<M> {

    FilledRoundRect(final Rectangle bounds, final double rx, final double ry, final SVGPen<M> stroke, final SVGPen<M> fill) {
      super(bounds, rx, ry, stroke, fill);
    }

    FilledRoundRect(final Rectangle bounds, final double rx, final double ry, final SVGPen<M> fill) {
      super(bounds, rx, ry, null, fill);
    }

    @Override
    public void serialize(final XmlWriter out) throws XmlException {
      serializeRoundRect(out);
      serializeFill(out);
      out.endTag(SVG_NAMESPACE, "rect", null);
    }

  }

  private static abstract class BaseCircle<M extends MeasureInfo> extends PaintedElem<M> {

    final double mX;
    final double mY;
    final double mRadius;

    public BaseCircle(final double x, final double y, final double radius, final SVGPen<M> stroke, final SVGPen<M> fill) {
      super(stroke, fill);
      mX = x;
      mY = y;
      mRadius = radius;
    }

    public void serializeCircle(final XmlWriter out) throws XmlException {
      out.startTag(SVG_NAMESPACE, "circle", null);
      out.attribute(null, "cx", null, Double.toString(mX));
      out.attribute(null, "cy", null, Double.toString(mY));
      out.attribute(null, "r", null, Double.toString(mRadius));
    }

    @Override
    public Rectangle getBounds(final Rectangle dest) {
      final double strokeWidth = stroke==null ? 0d :stroke.getStrokeWidth();
      double delta = strokeWidth / 2;
      dest.set(mX-mRadius-delta, mY-mRadius-delta, mRadius*2+strokeWidth, mRadius*2+strokeWidth);
      return dest;
    }

  }

  private static class Circle<M extends MeasureInfo> extends BaseCircle<M> {

    public Circle(final double x, final double y, final double radius, final SVGPen<M> color) {
      super(x, y, radius, color, null);
    }

    @Override
    public void serialize(final XmlWriter out) throws XmlException {
      serializeCircle(out);
      serializeStroke(out);
      out.endTag(SVG_NAMESPACE, "circle", null);
    }

  }

  private static class FilledCircle<M extends MeasureInfo> extends BaseCircle<M> {

    public FilledCircle(final double x, final double y, final double radius, final SVGPen<M> fill) {
      super(x, y, radius, null, fill);
    }

    public FilledCircle(final double x, final double y, final double radius, final SVGPen<M> stroke, final SVGPen<M> fill) {
      super(x, y, radius, stroke, fill);
    }

    @Override
    public void serialize(final XmlWriter out) throws XmlException {
      serializeCircle(out);
      serializeStrokeFill(out);
      out.endTag(SVG_NAMESPACE, "circle", null);
    }

  }

  private static class PaintedPath<M extends MeasureInfo> implements IPaintedElem {

    final SVGPath mPath;
    final SVGPen<M> mStroke;
    final SVGPen<M> mFill;

    public PaintedPath(final SVGPath path, final SVGPen<M> stroke, final SVGPen<M> fill) {
      mPath = path;
      mStroke = stroke;
      mFill = fill;
    }

    @Override
    public void serialize(final XmlWriter out) throws XmlException {
      out.startTag(SVG_NAMESPACE, "path", null);
      serializeStyle(out, mStroke, mFill, null);

      out.attribute(null, "d", null, mPath.toPathData());

      out.endTag(SVG_NAMESPACE, "path", null);
    }

    @Override
    public Rectangle getBounds(final Rectangle dest) {
      return mPath.getBounds(dest, mStroke);
    }
  }


  private static class DrawText<M extends MeasureInfo> extends PaintedElem<M> {

    final TextPos mTextPos;
    final double mX;
    final double mY;
    final String mText;
    @SuppressWarnings("unused")
    final double mFoldWidth;

    public DrawText(final TextPos textPos, final double x, final double y, final String text, final double foldWidth, final SVGPen<M> color) {
      super(null, color);
      mTextPos = textPos;
      mX = x;
      mY = y;
      mText = text;
      mFoldWidth = foldWidth;
    }

    @Override
    public void serialize(final XmlWriter out) throws XmlException {
      out.startTag(SVG_NAMESPACE, "text", null);
      out.attribute(null, "x", null, Double.toString(mX));
      out.attribute(null, "y", null, Double.toString(mY));
      serializeStyle(out, null, fill, mTextPos);
      out.startTag(SVG_NAMESPACE, "tspan", null);
      out.text(mText);
      out.endTag(SVG_NAMESPACE, "tspan", null);
      out.endTag(SVG_NAMESPACE, "text", null);
    }

    @Override
    public Rectangle getBounds(final Rectangle dest) {

      fill.measureTextSize(dest, mX, mY, mText, mFoldWidth);
      mTextPos.offset(dest, stroke);
      return dest;
    }
  }

  private static class SubCanvas<M extends MeasureInfo> extends SVGCanvas<M> implements IPaintedElem {

    final double mScale;
    final double mX;
    final double mY;

    SubCanvas(final SVGStrategy<M> strategy, final double offsetX, final double offsetY, final double scale) {
      super(strategy);
      mX = offsetX;
      mY = offsetY;
      mScale = scale;
    }

    @Override
    public void serialize(final XmlWriter out) throws XmlException {
      out.startTag(SVG_NAMESPACE, "g", null);
      if (mX==0 && mY==0) {
        if (mScale!=1d) {
          out.attribute(null, "transform", null, "scale(" + mScale + ")");
        }
      } else if (mScale==1d) {
        out.attribute(null, "transform", null, "translate("+mX+","+mY+")");
      } else {
        out.attribute(null, "transform", null, "matrix("+mScale+",0,0,"+mScale+","+mX*mScale+","+mY*mScale+")");
      }
      for (final IPaintedElem element:this.path) {
        element.serialize(out);
      }
      out.endTag(SVG_NAMESPACE, "g", null);
    }

  }

  private static final String SVG_NAMESPACE = "http://www.w3.org/2000/svg";

  private SVGStrategy<M> mStrategy;
  
  private static boolean sUseBaselineAlign = false;

  List<IPaintedElem> path = new ArrayList<>();

  private final Rectangle mBounds;

// Only for debug purposes
//  private SVGPen<M> mRedPen;
//  private SVGPen<M> mGreenPen;

  public SVGCanvas(final TextMeasurer<M> textMeasurer) {
    this(new SVGStrategy<>(textMeasurer));
  }

  public SVGCanvas(final SVGStrategy<M> strategy) {
    mBounds = new Rectangle(Double.NaN, Double.NaN, 0d, 0d);
    mStrategy = strategy;
// Only for debug purposes
//    mRedPen = mStrategy.newPen();
//    mRedPen.setColor(0xff, 0, 0);
//    mGreenPen = mStrategy.newPen();
//    mGreenPen.setColor(0, 0xff, 0);
  }

  public static void serializeStyle(final XmlWriter out, final SVGPen<?> stroke, final SVGPen<?> fill, final TextPos textPos) throws XmlException {
    final StringBuilder style = new StringBuilder();
    if (stroke!=null) {
      final int color = stroke.getColor();
      style.append("stroke: ").append(colorToSVGpaint(color)).append("; ");
      if (hasAlpha(color)) {
        style.append("stroke-opacity: ").append(colorToSVGOpacity(color)).append("; ");
      }
      style.append("stroke-width: ").append(Double.toString(stroke.getStrokeWidth())).append("; ");
    } else {
      style.append("stroke:none; ");
    }
    if (fill!=null) {
      final int color = fill.getColor();
      style.append("fill: ").append(colorToSVGpaint(color)).append("; ");
      if (hasAlpha(color)) {
        style.append("fill-opacity: ").append(colorToSVGOpacity(color)).append("; ");
      }
      if (textPos!=null) {
        style.append("font-family: Arial, Helvetica, sans; ");
        style.append("font-size: ").append(Double.toString(fill.getFontSize())).append("; ");
        if (fill.isTextItalics()) {
          style.append("font-style: italic; ");
        } else {
          style.append("font-style: normal; ");
        }
        style.append("text-anchor: ").append(toAnchor(textPos)).append("; ");
        if (sUseBaselineAlign) {
          style.append("alignment-baseline: ").append(toBaseline(textPos)).append("; ");
        }
      }
    } else {
      style.append("fill:none; ");
    }

    out.attribute(null, "style", null, style.toString());
  }

  private static String toBaseline(final TextPos textPos) {
    switch (textPos) {
      case BASELINELEFT:
      case BASELINEMIDDLE:
      case BASELINERIGHT:
        return "auto";
      case DESCENTLEFT:
      case DESCENT:
      case DESCENTRIGHT:
      case BOTTOM:
      case BOTTOMLEFT:
      case BOTTOMRIGHT:
        return "after-edge";
      case LEFT:
      case MIDDLE:
      case RIGHT:
        return "central";
      case ASCENT:
      case ASCENTLEFT:
      case ASCENTRIGHT:
      case MAXTOP:
      case MAXTOPLEFT:
      case MAXTOPRIGHT:
        return "before-edge";
    }
    throw new IllegalArgumentException();
  }

  private static String toAnchor(final TextPos textPos) {
    switch (textPos) {
      case MAXTOPLEFT:
      case ASCENTLEFT:
      case DESCENTLEFT:
      case LEFT:
      case BASELINELEFT:
      case BOTTOMLEFT:
        return "start";
      case MAXTOP:
      case ASCENT:
      case DESCENT:
      case MIDDLE:
      case BASELINEMIDDLE:
      case BOTTOM:
        return "middle";
      case MAXTOPRIGHT:
      case ASCENTRIGHT:
      case DESCENTRIGHT:
      case RIGHT:
      case BASELINERIGHT:
      case BOTTOMRIGHT:
        return "end";
    }
    throw new IllegalArgumentException();
  }

  private static boolean hasAlpha(final int color) {
    return (color >>>24) !=0xff;
  }

  private static String colorToSVGOpacity(final int color) {
    final int alpha = color >>> 24;
    return String.format("%5f", Double.valueOf(alpha/255d));
  }

  private static String colorToSVGpaint(final int color) {
    return String.format("#%06x", Integer.valueOf(color&0xffffff)); // Ignore alpha here
  }

  public void setBounds(final Rectangle bounds) {
    mBounds.set(bounds);
  }

  private void ensureBounds() {
    if (Doubles.isFinite(mBounds.top) &&
        Doubles.isFinite(mBounds.left) &&
        Doubles.isFinite(mBounds.height) &&
        Doubles.isFinite(mBounds.width)) {
      return;
    }
    if (path.size() > 0) {
      final Rectangle tmpBounds = new Rectangle(0d, 0d, 0d, 0d);
      mBounds.set(path.get(0).getBounds(tmpBounds));
      for (int i = 1; i < path.size(); i++) {
        mBounds.extendBounds(path.get(i).getBounds(tmpBounds));
      }
    } else {
      mBounds.set(0.0,0.0,0.0,0.0);
    }
  }

  public Rectangle getBounds(final Rectangle dest) {
    ensureBounds();
    dest.set(mBounds);
    return dest;
  }

  public Rectangle getBounds() {
    return getBounds(new Rectangle(0d, 0d, 0d, 0d));
  }

  @Override
  public SVGStrategy<M> getStrategy() {
    return mStrategy;
  }

  @Override
  public Canvas<SVGStrategy<M>, SVGPen<M>, SVGPath> childCanvas(final double offsetX, final double offsetY, final double scale) {
    final SubCanvas<M> result = new SubCanvas<>(mStrategy, offsetX, offsetY, scale);
    mBounds.top=Double.NaN;
    path.add(result);
    return result;
  }

  @Override
  public void drawFilledCircle(final double x, final double y, final double radius, final SVGPen<M> fill) {
    mBounds.top=Double.NaN;
    path.add(new FilledCircle<>(x, y, radius, fill.clone()));
  }

  @Override
  public void drawRect(final Rectangle rect, final SVGPen<M> stroke) {
    mBounds.top=Double.NaN;
    path.add(new Rect<>(rect, stroke));
  }

  @Override
  public void drawFilledRect(final Rectangle rect, final SVGPen<M> fill) {
    mBounds.top=Double.NaN;
    path.add(new FilledRect<>(rect, fill));
  }

  @Override
  public void drawRect(@NotNull final Rectangle rect, @NotNull final SVGPen<M> stroke, @NotNull final SVGPen<M> fill) {
    mBounds.top=Double.NaN;
    path.add(new FilledRect<>(rect, stroke, fill));
  }

  @Override
  public void drawCircle(final double x, final double y, final double radius, final SVGPen<M> stroke) {
    mBounds.top=Double.NaN;
    path.add(new Circle<>(x, y, radius, stroke));
  }

  @Override
  public void drawCircle(final double x,
                         final double y,
                         final double radius,
                         @NotNull final SVGPen<M> stroke,
                         @NotNull final SVGPen<M> fill)
  {
    mBounds.top=Double.NaN;
    path.add(new FilledCircle<>(x, y, radius, stroke, fill));
  }

  @Override
  public void drawRoundRect(final Rectangle rect, final double rx, final double ry, final SVGPen<M> stroke) {
    mBounds.top=Double.NaN;
    path.add(new RoundRect<>(rect, rx, ry, stroke));

  }

  @Override
  public void drawFilledRoundRect(final Rectangle rect, final double rx, final double ry, final SVGPen<M> fill) {
    mBounds.top=Double.NaN;
    path.add(new FilledRoundRect<>(rect, rx, ry, fill));
  }

  @Override
  public void drawRoundRect(@NotNull final Rectangle rect,
                            final double rx,
                            final double ry,
                            @NotNull final SVGPen<M> stroke,
                            @NotNull final SVGPen<M> fill)
  {
    mBounds.top=Double.NaN;
    path.add(new FilledRoundRect<>(rect, rx, ry, stroke, fill));
  }

  @Override
  public void drawPoly(final double[] points, final SVGPen<M> stroke) {
    drawPoly(points, stroke, null);
  }

  public void drawPoly(final double[] points, final SVGPen<M> stroke, final SVGPen<M> fill) {
    if (points.length>1) {
      final SVGPath path = pointsToPath(points);
      drawPath(path, stroke, fill);
    }
  }

  @Override
  public void drawFilledPoly(final double[] points, final SVGPen<M> fill) {
    drawPoly(points, null, fill);
  }

  private static SVGPath pointsToPath(final double[] points) {
    final SVGPath path = new SVGPath();
    path.moveTo(points[0], points[1]);
    for(int i=2; i<points.length; i+=2) {
      path.lineTo(points[i], points[i+1]);
    }
    return path;
  }

  @Override
  public void drawPath(final SVGPath path, final SVGPen<M> stroke, final SVGPen<M> fill) {
    mBounds.top=Double.NaN;
    this.path.add(new PaintedPath<>(path, stroke, fill));
  }

  @Override
  public Theme<SVGStrategy<M>, SVGPen<M>, SVGPath> getTheme() {
    return new SVGTheme<>(mStrategy);
  }

  @Override
  public void drawText(final TextPos textPos, final double x, final double y, final String text, final double foldWidth,
                       final SVGPen<M> pen) {
    final double adjustedY;
    if (sUseBaselineAlign) {
      adjustedY = y;
    } else {
      adjustedY = adjustToBaseline(textPos, y, pen);
    }
    mBounds.top=Double.NaN;
    path.add(new DrawText<>(textPos, x, adjustedY, text, foldWidth, pen.clone()));
// Only for debug purposes
//    mPath.add(new FilledCircle<>(pX, pY, 1d, mGreenPen));
//    mPath.add(new FilledCircle<>(pX, y, 1d, mRedPen));
  }

  private double adjustToBaseline(final nl.adaptivity.diagram.Canvas.TextPos textPos, final double y, final SVGPen<M> pen) {
    switch (textPos) {
    case BASELINELEFT:
    case BASELINEMIDDLE:
    case BASELINERIGHT:
      return y;
    case BOTTOM:
    case BOTTOMLEFT:
    case BOTTOMRIGHT:
      return y-pen.getTextMaxDescent();
    case DESCENT:
    case DESCENTLEFT:
    case DESCENTRIGHT:
      return y-pen.getTextDescent();
    case LEFT:
    case MIDDLE:
    case RIGHT: 
      return (y+0.5*(pen.getTextAscent()-pen.getTextDescent()));
    case ASCENT:
    case ASCENTLEFT:
    case ASCENTRIGHT:
      return y +pen.getTextAscent();
    case MAXTOP:
    case MAXTOPLEFT:
    case MAXTOPRIGHT:
      return y +pen.getTextMaxAscent();
    }
    throw new IllegalArgumentException();
  }

  public void serialize(final XmlWriter out) throws XmlException {
    ensureBounds();
    out.startTag(SVG_NAMESPACE, "svg", null);
    out.namespaceAttr(XMLConstants.DEFAULT_NS_PREFIX, SVG_NAMESPACE);
    out.attribute(null, "version", null, "1.1");
    out.attribute(null, "width", null, Double.toString(mBounds.width));
    out.attribute(null, "height", null, Double.toString(mBounds.height));
    final boolean closeGroup;
    // As svg outer element only supports width and height, when the topleft corner is not at the
    // origin then wrap the content into a group that translates appropriately.
    if (mBounds.left!=0.0d || mBounds.top!=0.0d) {
      closeGroup = true;
      out.startTag(SVG_NAMESPACE, "g", null);
      out.attribute(null, "transform", null, "translate("+ -mBounds.left+","+ -mBounds.top+")");
    } else {
      closeGroup = false;
    }


    for (final IPaintedElem element: path) {
      try {element.serialize(out);} catch (XmlException e) {
        throw new RuntimeException(e);
      }
    }
    if (closeGroup) out.endTag(SVG_NAMESPACE, "g", null);
    out.endTag(SVG_NAMESPACE, "svg", null);
  }
}
