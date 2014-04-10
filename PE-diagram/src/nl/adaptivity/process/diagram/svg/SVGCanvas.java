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
      pOut.startTag(SVG_NAMESPACE, "rect");
      pOut.addAttribute("x", Double.toString(aBounds.left));
      pOut.addAttribute("y", Double.toString(aBounds.top));
      pOut.addAttribute("width", Double.toString(aBounds.width));
      pOut.addAttribute("height", Double.toString(aBounds.height));
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
      pOut.endTag(SVG_NAMESPACE, "rect");
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
      pOut.endTag(SVG_NAMESPACE, "rect");
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
      pOut.addAttribute("rx", Double.toString(aRx));
      pOut.addAttribute("ry", Double.toString(aRy));
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
      pOut.endTag(SVG_NAMESPACE, "rect");
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
      pOut.endTag(SVG_NAMESPACE, "rect");
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
      pOut.startTag(SVG_NAMESPACE, "circle");
      pOut.addAttribute("cx", Double.toString(mX));
      pOut.addAttribute("cy", Double.toString(mY));
      pOut.addAttribute("r", Double.toString(mRadius));
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
      pOut.endTag(SVG_NAMESPACE, "circle");
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
      pOut.endTag(SVG_NAMESPACE, "circle");
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
      pOut.startTag(SVG_NAMESPACE, "path");
      serializeStyle(pOut, mStroke, mFill, null);

      pOut.addAttribute("d", mPath.toPathData());

      pOut.endTag(SVG_NAMESPACE, "path");
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
      pOut.startTag(SVG_NAMESPACE, "text");
      pOut.addAttribute("x", Double.toString(mX));
      pOut.addAttribute("y", Double.toString(mY));
      serializeStyle(pOut, null, mColor, mTextPos);
      pOut.startTag(SVG_NAMESPACE, "tspan");
      pOut.text(mText);
      pOut.endTag(SVG_NAMESPACE, "tspan");
      pOut.endTag(SVG_NAMESPACE, "text");
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
      pOut.startTag(SVG_NAMESPACE, "g");
      pOut.addAttribute("transform", "scale("+aScale+")");
      super.serialize(pOut);
      pOut.endTag(SVG_NAMESPACE, "g");
    }

  }

  private static final String SVG_NAMESPACE = "http://www.w3.org/2000/svg";

  private SVGStrategy<M> aStrategy;

  private List<IPaintedElem> aPath = new ArrayList<>();

  public SVGCanvas(TextMeasurer<M> pTextMeasurer) {
    aStrategy = new SVGStrategy<>(pTextMeasurer);
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
        style.append("alignment-baseline").append(toBaseline(pTextPos)).append("; ");
      }
    }

    pOut.addAttribute("style", style.toString());
  }

  private static String toBaseline(TextPos pTextPos) {
    switch (pTextPos) {
      case BASELINELEFT:
      case BASELINEMIDDLE:
      case BASELINERIGHT:
        return "auto";
      case BOTTOM:
      case BOTTOMLEFT:
      case BOTTOMRIGHT:
        return "after-edge";
      case LEFT:
      case MIDDLE:
      case RIGHT:
        return "central";
      case TOP:
      case TOPLEFT:
      case TOPRIGHT:
        return "before-edge";
    }
    throw new IllegalArgumentException();
  }

  private static String toAnchor(TextPos pTextPos) {
    switch (pTextPos) {
      case TOPLEFT:
      case LEFT:
      case BASELINELEFT:
      case BOTTOMLEFT:
        return "start";
      case TOP:
      case MIDDLE:
      case BASELINEMIDDLE:
      case BOTTOM:
        return "middle";
      case TOPRIGHT:
      case RIGHT:
      case BASELINERIGHT:
      case BOTTOMRIGHT:
        return "end";
    }
    throw new IllegalArgumentException();
  }

  private static boolean hasAlpha(int pColor) {
    return (pColor >>24) !=0xff;
  }

  private static String colorToSVGOpacity(int pColor) {
    int alpha = pColor >>24;
    return Double.toString(alpha/255);
  }

  private static String colorToSVGpaint(int pColor) {
    return '#'+Integer.toHexString(pColor&0xffffff); // Ignore alpha here
  }

  public SVGCanvas(SVGStrategy<M> pStrategy) {
    aStrategy = pStrategy;
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
    aPath.add(new DrawText<>(pTextPos, pX, pY, pText, pFoldWidth, pPen.clone()));

  }

  public void serialize(SerializerAdapter pOut) {
    pOut.addNamespace(XMLConstants.DEFAULT_NS_PREFIX, SVG_NAMESPACE);
    pOut.startTag(SVG_NAMESPACE, "svg");
    pOut.addAttribute("version", "1.1");

    for (IPaintedElem element:aPath) {
      element.serialize(pOut);
    }

    pOut.endTag(SVG_NAMESPACE, "svg");
  }
}
