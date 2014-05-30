package nl.adaptivity.process.diagram.svg;

import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;

import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.Theme;
import nl.adaptivity.process.clientProcessModel.SerializerAdapter;
import nl.adaptivity.process.diagram.svg.TextMeasurer.MeasureInfo;


public class SVGCanvas<M extends MeasureInfo> implements Canvas<SVGStrategy<M>, SVGPen<M>, SVGPath> {

  interface IPaintedElem {

    void serialize(SerializerAdapter pOut);

  }

  private static abstract class PaintedElem<M extends MeasureInfo> implements IPaintedElem {
    final SVGPen<M> mColor;

    PaintedElem(SVGPen<M> pColor) {
      mColor = pColor;
    }

    void serializeFill(SerializerAdapter pOut) {
      serializeStyle(pOut, null, mColor, null);
    }

    void serializeStroke(SerializerAdapter pOut) {
      serializeStyle(pOut, mColor, null, null);
    }

  }

  private static abstract class BaseRect<M extends MeasureInfo> extends PaintedElem<M> {
    final Rectangle aBounds;

    BaseRect(Rectangle pBounds, SVGPen<M> pColor) {
      super(pColor);
      aBounds = pBounds;
    }

    void serializeRect(SerializerAdapter pOut) {
      pOut.startTag(SVG_NAMESPACE, "rect", true);
      pOut.addAttribute(null, "x", Double.toString(aBounds.left));
      pOut.addAttribute(null, "y", Double.toString(aBounds.top));
      pOut.addAttribute(null, "width", Double.toString(aBounds.width));
      pOut.addAttribute(null, "height", Double.toString(aBounds.height));
    }
  }

  private static class Rect<M extends MeasureInfo> extends BaseRect<M> {

    public Rect(Rectangle pBounds, SVGPen<M> pColor) {
      super(pBounds, pColor);
    }

    @Override
    public void serialize(SerializerAdapter pOut) {
      serializeRect(pOut);
      serializeStroke(pOut);
      pOut.endTag(SVG_NAMESPACE, "rect", true);
    }

  }

  private static class FilledRect<M extends MeasureInfo> extends BaseRect<M> {

    FilledRect(Rectangle pBounds, SVGPen<M> pColor) {
      super(pBounds, pColor);
    }

    @Override
    public void serialize(SerializerAdapter pOut) {
      serializeRect(pOut);
      serializeFill(pOut);
      pOut.endTag(SVG_NAMESPACE, "rect", true);
    }

  }

  private static abstract class BaseRoundRect<M extends MeasureInfo> extends BaseRect<M> {
    final double aRx;
    final double aRy;

    BaseRoundRect(Rectangle pBounds, double pRx, double pRy, SVGPen<M> pColor) {
      super(pBounds, pColor);
      aRx = pRx;
      aRy = pRy;
    }

    void serializeRoundRect(SerializerAdapter pOut) {
      serializeRect(pOut);
      pOut.addAttribute(null, "rx", Double.toString(aRx));
      pOut.addAttribute(null, "ry", Double.toString(aRy));
    }
  }

  private static class RoundRect<M extends MeasureInfo> extends BaseRoundRect<M> {

    RoundRect(Rectangle pBounds, double pRx, double pRy, SVGPen<M> pColor) {
      super(pBounds, pRx, pRy, pColor);
    }

    @Override
    public void serialize(SerializerAdapter pOut) {
      serializeRoundRect(pOut);
      serializeStroke(pOut);
      pOut.endTag(SVG_NAMESPACE, "rect", true);
    }

  }

  private static class FilledRoundRect<M extends MeasureInfo> extends BaseRoundRect<M> {

    FilledRoundRect(Rectangle pBounds, double pRx, double pRy, SVGPen<M> pColor) {
      super(pBounds, pRx, pRy, pColor);
    }

    @Override
    public void serialize(SerializerAdapter pOut) {
      serializeRoundRect(pOut);
      serializeFill(pOut);
      pOut.endTag(SVG_NAMESPACE, "rect", true);
    }

  }

  private static abstract class BaseCircle<M extends MeasureInfo> extends PaintedElem<M> {

    final double mX;
    final double mY;
    final double mRadius;

    public BaseCircle(double pX, double pY, double pRadius, SVGPen<M> pColor) {
      super(pColor);
      mX = pX;
      mY = pY;
      mRadius = pRadius;
    }

    public void serializeCircle(SerializerAdapter pOut) {
      pOut.startTag(SVG_NAMESPACE, "circle", true);
      pOut.addAttribute(null, "cx", Double.toString(mX));
      pOut.addAttribute(null, "cy", Double.toString(mY));
      pOut.addAttribute(null, "r", Double.toString(mRadius));
    }

  }

  private static class Circle<M extends MeasureInfo> extends BaseCircle<M> {

    public Circle(double pX, double pY, double pRadius, SVGPen<M> pColor) {
      super(pX, pY, pRadius, pColor);
    }

    @Override
    public void serialize(SerializerAdapter pOut) {
      serializeCircle(pOut);
      serializeStroke(pOut);
      pOut.endTag(SVG_NAMESPACE, "circle", true);
    }

  }

  private static class FilledCircle<M extends MeasureInfo> extends BaseCircle<M> {

    public FilledCircle(double pX, double pY, double pRadius, SVGPen<M> pColor) {
      super(pX, pY, pRadius, pColor);
    }

    @Override
    public void serialize(SerializerAdapter pOut) {
      serializeCircle(pOut);
      serializeFill(pOut);
      pOut.endTag(SVG_NAMESPACE, "circle", true);
    }

  }

  private static class PaintedPath<M extends MeasureInfo> implements IPaintedElem {

    final SVGPath mPath;
    final SVGPen<M> mStroke;
    final SVGPen<M> mFill;

    public PaintedPath(SVGPath pPath, SVGPen<M> pStroke, SVGPen<M> pFill) {
      mPath = pPath;
      mStroke = pStroke;
      mFill = pFill;
    }

    @Override
    public void serialize(SerializerAdapter pOut) {
      pOut.startTag(SVG_NAMESPACE, "path", true);
      serializeStyle(pOut, mStroke, mFill, null);

      pOut.addAttribute(null, "d", mPath.toPathData());

      pOut.endTag(SVG_NAMESPACE, "path", true);
    }

  }


  private static class DrawText<M extends MeasureInfo> extends PaintedElem<M> {

    final TextPos mTextPos;
    final double mX;
    final double mY;
    final String mText;
    @SuppressWarnings("unused")
    final double mFoldWidth;

    public DrawText(TextPos pTextPos, double pX, double pY, String pText, double pFoldWidth, SVGPen<M> pColor) {
      super(pColor);
      mTextPos = pTextPos;
      mX = pX;
      mY = pY;
      mText = pText;
      mFoldWidth = pFoldWidth;
    }

    @Override
    public void serialize(SerializerAdapter pOut) {
      pOut.startTag(SVG_NAMESPACE, "text", true);
      pOut.addAttribute(null, "x", Double.toString(mX));
      pOut.addAttribute(null, "y", Double.toString(mY));
      serializeStyle(pOut, null, mColor, mTextPos);
      pOut.startTag(SVG_NAMESPACE, "tspan", false);
      pOut.text(mText);
      pOut.endTag(SVG_NAMESPACE, "tspan", true);
      pOut.endTag(SVG_NAMESPACE, "text", true);
    }

  }

  private static class SubCanvas<M extends MeasureInfo> extends SVGCanvas<M> implements IPaintedElem {

    final double aScale;
    final double aX;
    final double aY;

    SubCanvas(SVGStrategy<M> pStrategy, Rectangle pArea, double pScale) {
      super(pStrategy);
      aX = pArea.left;
      aY = pArea.top;
      aScale = pScale;
    }

    @Override
    public void serialize(SerializerAdapter pOut) {
      pOut.startTag(SVG_NAMESPACE, "g", true);
      if (aX==0 && aY==0) {
        pOut.addAttribute(null, "transform", "scale("+aScale+")");
      } else {
        pOut.addAttribute(null, "transform", "matrix("+aScale+",0,0,"+aScale+","+aX*aScale+","+aY*aScale+")");
      }
      for (IPaintedElem element:this.aPath) {
        element.serialize(pOut);
      }
      pOut.endTag(SVG_NAMESPACE, "g", true);
    }

  }

  private static final String SVG_NAMESPACE = "http://www.w3.org/2000/svg";

  private SVGStrategy<M> aStrategy;
  
  private static boolean sUseBaselineAlign = false;

  List<IPaintedElem> aPath = new ArrayList<>();

  private Rectangle aBounds;

// Only for debug purposes
//  private SVGPen<M> aRedPen;
//  private SVGPen<M> aGreenPen;

  public SVGCanvas(TextMeasurer<M> pTextMeasurer) {
    this(new SVGStrategy<>(pTextMeasurer));
  }

  public SVGCanvas(SVGStrategy<M> pStrategy) {
    aStrategy = pStrategy;
// Only for debug purposes
//    aRedPen = aStrategy.newPen();
//    aRedPen.setColor(0xff, 0, 0);
//    aGreenPen = aStrategy.newPen();
//    aGreenPen.setColor(0, 0xff, 0);
  }

  public static void serializeStyle(SerializerAdapter pOut, SVGPen<?> pStroke, SVGPen<?> pFill, TextPos pTextPos) {
    StringBuilder style = new StringBuilder();
    if (pStroke!=null) {
      final int color = pStroke.getColor();
      style.append("stroke: ").append(colorToSVGpaint(color)).append("; ");
      if (hasAlpha(color)) {
        style.append("stroke-opacity: ").append(colorToSVGOpacity(color)).append("; ");
      }
      style.append("stroke-width: ").append(Double.toString(pStroke.getStrokeWidth())).append("; ");
    } else {
      style.append("stroke:none;");
    }
    if (pFill!=null) {
      final int color = pFill.getColor();
      style.append("fill: ").append(colorToSVGpaint(color)).append("; ");
      if (hasAlpha(color)) {
        style.append("fill-opacity: ").append(colorToSVGOpacity(color)).append("; ");
      }
      if (pTextPos!=null) {
        style.append("font-family: Arial, Helvetica, sans; ");
        style.append("font-size: ").append(Double.toString(pFill.getFontSize())).append("; ");
        if (pFill.isTextItalics()) {
          style.append("font-style: italic; ");
        } else {
          style.append("font-style: normal; ");
        }
        style.append("text-anchor: ").append(toAnchor(pTextPos)).append("; ");
        if (sUseBaselineAlign) {
          style.append("alignment-baseline: ").append(toBaseline(pTextPos)).append("; ");
        }
      }
    } else {
      style.append("fill:none; ");
    }

    pOut.addAttribute(null, "style", style.toString());
  }

  private static String toBaseline(TextPos pTextPos) {
    switch (pTextPos) {
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

  private static String toAnchor(TextPos pTextPos) {
    switch (pTextPos) {
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

  private static boolean hasAlpha(int pColor) {
    return (pColor >>>24) !=0xff;
  }

  private static String colorToSVGOpacity(int pColor) {
    int alpha = pColor >>>24;
    return String.format("%5f", Double.valueOf(alpha/255d));
  }

  private static String colorToSVGpaint(int pColor) {
    return String.format("#%06x", Integer.valueOf(pColor&0xffffff)); // Ignore alpha here
  }

  public void setBounds(Rectangle pBounds) {
    aBounds = pBounds;
  }

  @Override
  public SVGStrategy<M> getStrategy() {
    return aStrategy;
  }

  @Override
  public Canvas<SVGStrategy<M>, SVGPen<M>, SVGPath> childCanvas(Rectangle pArea, double pScale) {
    final SubCanvas<M> result = new SubCanvas<>(aStrategy, pArea, pScale);
    aPath.add(result);
    return result;
  }

  @Override
  public void drawFilledCircle(double pX, double pY, double pRadius, SVGPen<M> pColor) {
    aPath.add(new FilledCircle<>(pX, pY, pRadius, pColor.clone()));
  }

  @Override
  public void drawRect(Rectangle pRect, SVGPen<M> pColor) {
    aPath.add(new Rect<>(pRect, pColor));
  }

  @Override
  public void drawFilledRect(Rectangle pRect, SVGPen<M> pColor) {
    aPath.add(new FilledRect<>(pRect, pColor));
  }

  @Override
  public void drawCircle(double pX, double pY, double pRadius, SVGPen<M> pColor) {
    aPath.add(new Circle<>(pX, pY, pRadius, pColor));
  }

  @Override
  public void drawRoundRect(Rectangle pRect, double pRx, double pRy, SVGPen<M> pColor) {
    aPath.add(new RoundRect<>(pRect, pRx, pRy, pColor));

  }

  @Override
  public void drawFilledRoundRect(Rectangle pRect, double pRx, double pRy, SVGPen<M> pColor) {
    aPath.add(new FilledRoundRect<>(pRect, pRx, pRy, pColor));
  }

  @Override
  public void drawPoly(double[] pPoints, SVGPen<M> pColor) {
    if (pPoints.length>1) {
      SVGPath path = pointsToPath(pPoints);
      drawPath(path, pColor, null);
    }
  }

  @Override
  public void drawFilledPoly(double[] pPoints, SVGPen<M> pColor) {
    if (pPoints.length>1) {
      SVGPath path = pointsToPath(pPoints);
      drawPath(path, null, pColor);
    }
  }

  private static SVGPath pointsToPath(double[] pPoints) {
    SVGPath path = new SVGPath();
    path.moveTo(pPoints[0], pPoints[1]);
    for(int i=2; i<pPoints.length; i+=2) {
      path.lineTo(pPoints[i], pPoints[i+1]);
    }
    return path;
  }

  @Override
  public void drawPath(SVGPath pPath, SVGPen<M> pStroke, SVGPen<M> pFill) {
    aPath.add(new PaintedPath<>(pPath, pStroke, pFill));
  }

  @Override
  public Theme<SVGStrategy<M>, SVGPen<M>, SVGPath> getTheme() {
    return new SVGTheme<>(aStrategy);
  }

  @Override
  public void drawText(TextPos pTextPos, double pX, double pY, String pText, double pFoldWidth,
                       SVGPen<M> pPen) {
    final double y;
    if (sUseBaselineAlign) {
      y = pY;
    } else {
      y = adjustToBaseline(pTextPos, pY, pPen);
    }
    aPath.add(new DrawText<>(pTextPos, pX, y, pText, pFoldWidth, pPen.clone()));
// Only for debug purposes
//    aPath.add(new FilledCircle<>(pX, pY, 1d, aGreenPen));
//    aPath.add(new FilledCircle<>(pX, y, 1d, aRedPen));
  }

  private double adjustToBaseline(nl.adaptivity.diagram.Canvas.TextPos pTextPos, double pY, SVGPen<M> pPen) {
    switch (pTextPos) {
    case BASELINELEFT:
    case BASELINEMIDDLE:
    case BASELINERIGHT:
      return pY;
    case BOTTOM:
    case BOTTOMLEFT:
    case BOTTOMRIGHT:
      return pY-pPen.getTextMaxDescent();
    case DESCENT:
    case DESCENTLEFT:
    case DESCENTRIGHT:
      return pY-pPen.getTextDescent();
    case LEFT:
    case MIDDLE:
    case RIGHT: 
      return (pY+0.5*(pPen.getTextAscent()-pPen.getTextDescent()));
    case ASCENT:
    case ASCENTLEFT:
    case ASCENTRIGHT:
      return pY +pPen.getTextAscent();
    case MAXTOP:
    case MAXTOPLEFT:
    case MAXTOPRIGHT:
      return pY +pPen.getTextMaxAscent();
    }
    throw new IllegalArgumentException();
  }

  public void serialize(SerializerAdapter pOut) {
    pOut.addNamespace(XMLConstants.DEFAULT_NS_PREFIX, SVG_NAMESPACE);
    pOut.startTag(SVG_NAMESPACE, "svg", true);
    pOut.addAttribute(null, "version", "1.1");
    pOut.addAttribute(null, "width", Double.toString(aBounds.width+aBounds.left*2));
    pOut.addAttribute(null, "height", Double.toString(aBounds.height+aBounds.top*2));

    for (IPaintedElem element:aPath) {
      element.serialize(pOut);
    }

    pOut.endTag(SVG_NAMESPACE, "svg", true);
  }
}
