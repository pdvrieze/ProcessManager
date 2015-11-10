package nl.adaptivity.process.diagram.svg;

import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.Theme;
import nl.adaptivity.process.clientProcessModel.SerializerAdapter;
import nl.adaptivity.process.diagram.svg.TextMeasurer.MeasureInfo;

import javax.xml.XMLConstants;

import java.util.ArrayList;
import java.util.List;


public class SVGCanvas<M extends MeasureInfo> implements Canvas<SVGStrategy<M>, SVGPen<M>, SVGPath> {

  interface IPaintedElem {

    void serialize(SerializerAdapter out);

  }

  private static abstract class PaintedElem<M extends MeasureInfo> implements IPaintedElem {
    final SVGPen<M> mColor;

    PaintedElem(SVGPen<M> color) {
      mColor = color;
    }

    void serializeFill(SerializerAdapter out) {
      serializeStyle(out, null, mColor, null);
    }

    void serializeStroke(SerializerAdapter out) {
      serializeStyle(out, mColor, null, null);
    }

  }

  private static abstract class BaseRect<M extends MeasureInfo> extends PaintedElem<M> {
    final Rectangle mBounds;

    BaseRect(Rectangle bounds, SVGPen<M> color) {
      super(color);
      mBounds = bounds;
    }

    void serializeRect(SerializerAdapter out) {
      out.startTag(SVG_NAMESPACE, "rect", true);
      out.addAttribute(null, "x", Double.toString(mBounds.left));
      out.addAttribute(null, "y", Double.toString(mBounds.top));
      out.addAttribute(null, "width", Double.toString(mBounds.width));
      out.addAttribute(null, "height", Double.toString(mBounds.height));
    }
  }

  private static class Rect<M extends MeasureInfo> extends BaseRect<M> {

    public Rect(Rectangle bounds, SVGPen<M> color) {
      super(bounds, color);
    }

    @Override
    public void serialize(SerializerAdapter out) {
      serializeRect(out);
      serializeStroke(out);
      out.endTag(SVG_NAMESPACE, "rect", true);
    }

  }

  private static class FilledRect<M extends MeasureInfo> extends BaseRect<M> {

    FilledRect(Rectangle bounds, SVGPen<M> color) {
      super(bounds, color);
    }

    @Override
    public void serialize(SerializerAdapter out) {
      serializeRect(out);
      serializeFill(out);
      out.endTag(SVG_NAMESPACE, "rect", true);
    }

  }

  private static abstract class BaseRoundRect<M extends MeasureInfo> extends BaseRect<M> {
    final double mRx;
    final double mRy;

    BaseRoundRect(Rectangle bounds, double rx, double ry, SVGPen<M> color) {
      super(bounds, color);
      mRx = rx;
      mRy = ry;
    }

    void serializeRoundRect(SerializerAdapter out) {
      serializeRect(out);
      out.addAttribute(null, "rx", Double.toString(mRx));
      out.addAttribute(null, "ry", Double.toString(mRy));
    }
  }

  private static class RoundRect<M extends MeasureInfo> extends BaseRoundRect<M> {

    RoundRect(Rectangle bounds, double rx, double ry, SVGPen<M> color) {
      super(bounds, rx, ry, color);
    }

    @Override
    public void serialize(SerializerAdapter out) {
      serializeRoundRect(out);
      serializeStroke(out);
      out.endTag(SVG_NAMESPACE, "rect", true);
    }

  }

  private static class FilledRoundRect<M extends MeasureInfo> extends BaseRoundRect<M> {

    FilledRoundRect(Rectangle bounds, double rx, double ry, SVGPen<M> color) {
      super(bounds, rx, ry, color);
    }

    @Override
    public void serialize(SerializerAdapter out) {
      serializeRoundRect(out);
      serializeFill(out);
      out.endTag(SVG_NAMESPACE, "rect", true);
    }

  }

  private static abstract class BaseCircle<M extends MeasureInfo> extends PaintedElem<M> {

    final double mX;
    final double mY;
    final double mRadius;

    public BaseCircle(double x, double y, double radius, SVGPen<M> color) {
      super(color);
      mX = x;
      mY = y;
      mRadius = radius;
    }

    public void serializeCircle(SerializerAdapter out) {
      out.startTag(SVG_NAMESPACE, "circle", true);
      out.addAttribute(null, "cx", Double.toString(mX));
      out.addAttribute(null, "cy", Double.toString(mY));
      out.addAttribute(null, "r", Double.toString(mRadius));
    }

  }

  private static class Circle<M extends MeasureInfo> extends BaseCircle<M> {

    public Circle(double x, double y, double radius, SVGPen<M> color) {
      super(x, y, radius, color);
    }

    @Override
    public void serialize(SerializerAdapter out) {
      serializeCircle(out);
      serializeStroke(out);
      out.endTag(SVG_NAMESPACE, "circle", true);
    }

  }

  private static class FilledCircle<M extends MeasureInfo> extends BaseCircle<M> {

    public FilledCircle(double x, double y, double radius, SVGPen<M> color) {
      super(x, y, radius, color);
    }

    @Override
    public void serialize(SerializerAdapter out) {
      serializeCircle(out);
      serializeFill(out);
      out.endTag(SVG_NAMESPACE, "circle", true);
    }

  }

  private static class PaintedPath<M extends MeasureInfo> implements IPaintedElem {

    final SVGPath mPath;
    final SVGPen<M> mStroke;
    final SVGPen<M> mFill;

    public PaintedPath(SVGPath path, SVGPen<M> stroke, SVGPen<M> fill) {
      mPath = path;
      mStroke = stroke;
      mFill = fill;
    }

    @Override
    public void serialize(SerializerAdapter out) {
      out.startTag(SVG_NAMESPACE, "path", true);
      serializeStyle(out, mStroke, mFill, null);

      out.addAttribute(null, "d", mPath.toPathData());

      out.endTag(SVG_NAMESPACE, "path", true);
    }

  }


  private static class DrawText<M extends MeasureInfo> extends PaintedElem<M> {

    final TextPos mTextPos;
    final double mX;
    final double mY;
    final String mText;
    @SuppressWarnings("unused")
    final double mFoldWidth;

    public DrawText(TextPos textPos, double x, double y, String text, double foldWidth, SVGPen<M> color) {
      super(color);
      mTextPos = textPos;
      mX = x;
      mY = y;
      mText = text;
      mFoldWidth = foldWidth;
    }

    @Override
    public void serialize(SerializerAdapter out) {
      out.startTag(SVG_NAMESPACE, "text", true);
      out.addAttribute(null, "x", Double.toString(mX));
      out.addAttribute(null, "y", Double.toString(mY));
      serializeStyle(out, null, mColor, mTextPos);
      out.startTag(SVG_NAMESPACE, "tspan", false);
      out.text(mText);
      out.endTag(SVG_NAMESPACE, "tspan", true);
      out.endTag(SVG_NAMESPACE, "text", true);
    }

  }

  private static class SubCanvas<M extends MeasureInfo> extends SVGCanvas<M> implements IPaintedElem {

    final double mScale;
    final double mX;
    final double mY;

    SubCanvas(SVGStrategy<M> strategy, Rectangle area, double scale) {
      super(strategy);
      mX = area.left;
      mY = area.top;
      mScale = scale;
    }

    @Override
    public void serialize(SerializerAdapter out) {
      out.startTag(SVG_NAMESPACE, "g", true);
      if (mX==0 && mY==0) {
        out.addAttribute(null, "transform", "scale("+mScale+")");
      } else {
        out.addAttribute(null, "transform", "matrix("+mScale+",0,0,"+mScale+","+mX*mScale+","+mY*mScale+")");
      }
      for (IPaintedElem element:this.mPath) {
        element.serialize(out);
      }
      out.endTag(SVG_NAMESPACE, "g", true);
    }

  }

  private static final String SVG_NAMESPACE = "http://www.w3.org/2000/svg";

  private SVGStrategy<M> mStrategy;
  
  private static boolean sUseBaselineAlign = false;

  List<IPaintedElem> mPath = new ArrayList<>();

  private Rectangle mBounds;

// Only for debug purposes
//  private SVGPen<M> mRedPen;
//  private SVGPen<M> mGreenPen;

  public SVGCanvas(TextMeasurer<M> textMeasurer) {
    this(new SVGStrategy<>(textMeasurer));
  }

  public SVGCanvas(SVGStrategy<M> strategy) {
    mStrategy = strategy;
// Only for debug purposes
//    mRedPen = mStrategy.newPen();
//    mRedPen.setColor(0xff, 0, 0);
//    mGreenPen = mStrategy.newPen();
//    mGreenPen.setColor(0, 0xff, 0);
  }

  public static void serializeStyle(SerializerAdapter out, SVGPen<?> stroke, SVGPen<?> fill, TextPos textPos) {
    StringBuilder style = new StringBuilder();
    if (stroke!=null) {
      final int color = stroke.getColor();
      style.append("stroke: ").append(colorToSVGpaint(color)).append("; ");
      if (hasAlpha(color)) {
        style.append("stroke-opacity: ").append(colorToSVGOpacity(color)).append("; ");
      }
      style.append("stroke-width: ").append(Double.toString(stroke.getStrokeWidth())).append("; ");
    } else {
      style.append("stroke:none;");
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

    out.addAttribute(null, "style", style.toString());
  }

  private static String toBaseline(TextPos textPos) {
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

  private static String toAnchor(TextPos textPos) {
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

  private static boolean hasAlpha(int color) {
    return (color >>>24) !=0xff;
  }

  private static String colorToSVGOpacity(int color) {
    int alpha = color >>>24;
    return String.format("%5f", Double.valueOf(alpha/255d));
  }

  private static String colorToSVGpaint(int color) {
    return String.format("#%06x", Integer.valueOf(color&0xffffff)); // Ignore alpha here
  }

  public void setBounds(Rectangle bounds) {
    mBounds = bounds;
  }

  @Override
  public SVGStrategy<M> getStrategy() {
    return mStrategy;
  }

  @Override
  public Canvas<SVGStrategy<M>, SVGPen<M>, SVGPath> childCanvas(Rectangle area, double scale) {
    final SubCanvas<M> result = new SubCanvas<>(mStrategy, area, scale);
    mPath.add(result);
    return result;
  }

  @Override
  public void drawFilledCircle(double x, double y, double radius, SVGPen<M> color) {
    mPath.add(new FilledCircle<>(x, y, radius, color.clone()));
  }

  @Override
  public void drawRect(Rectangle rect, SVGPen<M> color) {
    mPath.add(new Rect<>(rect, color));
  }

  @Override
  public void drawFilledRect(Rectangle rect, SVGPen<M> color) {
    mPath.add(new FilledRect<>(rect, color));
  }

  @Override
  public void drawCircle(double x, double y, double radius, SVGPen<M> color) {
    mPath.add(new Circle<>(x, y, radius, color));
  }

  @Override
  public void drawRoundRect(Rectangle rect, double rx, double ry, SVGPen<M> color) {
    mPath.add(new RoundRect<>(rect, rx, ry, color));

  }

  @Override
  public void drawFilledRoundRect(Rectangle rect, double rx, double ry, SVGPen<M> color) {
    mPath.add(new FilledRoundRect<>(rect, rx, ry, color));
  }

  @Override
  public void drawPoly(double[] points, SVGPen<M> color) {
    if (points.length>1) {
      SVGPath path = pointsToPath(points);
      drawPath(path, color, null);
    }
  }

  @Override
  public void drawFilledPoly(double[] points, SVGPen<M> color) {
    if (points.length>1) {
      SVGPath path = pointsToPath(points);
      drawPath(path, null, color);
    }
  }

  private static SVGPath pointsToPath(double[] points) {
    SVGPath path = new SVGPath();
    path.moveTo(points[0], points[1]);
    for(int i=2; i<points.length; i+=2) {
      path.lineTo(points[i], points[i+1]);
    }
    return path;
  }

  @Override
  public void drawPath(SVGPath path, SVGPen<M> stroke, SVGPen<M> fill) {
    mPath.add(new PaintedPath<>(path, stroke, fill));
  }

  @Override
  public Theme<SVGStrategy<M>, SVGPen<M>, SVGPath> getTheme() {
    return new SVGTheme<>(mStrategy);
  }

  @Override
  public void drawText(TextPos textPos, double x, double y, String text, double foldWidth,
                       SVGPen<M> pen) {
    final double adjustedY;
    if (sUseBaselineAlign) {
      adjustedY = y;
    } else {
      adjustedY = adjustToBaseline(textPos, y, pen);
    }
    mPath.add(new DrawText<>(textPos, x, adjustedY, text, foldWidth, pen.clone()));
// Only for debug purposes
//    mPath.add(new FilledCircle<>(pX, pY, 1d, mGreenPen));
//    mPath.add(new FilledCircle<>(pX, y, 1d, mRedPen));
  }

  private double adjustToBaseline(nl.adaptivity.diagram.Canvas.TextPos textPos, double y, SVGPen<M> pen) {
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

  public void serialize(SerializerAdapter out) {
    out.addNamespace(XMLConstants.DEFAULT_NS_PREFIX, SVG_NAMESPACE);
    out.startTag(SVG_NAMESPACE, "svg", true);
    out.addAttribute(null, "version", "1.1");
    out.addAttribute(null, "width", Double.toString(mBounds.width+mBounds.left*2));
    out.addAttribute(null, "height", Double.toString(mBounds.height+mBounds.top*2));

    for (IPaintedElem element:mPath) {
      element.serialize(out);
    }

    out.endTag(SVG_NAMESPACE, "svg", true);
  }
}
