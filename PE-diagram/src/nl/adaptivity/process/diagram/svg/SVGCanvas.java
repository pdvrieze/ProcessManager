package nl.adaptivity.process.diagram.svg;

import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;

import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.Theme;
import nl.adaptivity.diagram.Canvas.TextPos;
import nl.adaptivity.process.clientProcessModel.SerializerAdapter;


public class SVGCanvas implements Canvas<SVGStrategy, SVGPen, SVGPath> {



  interface PathElem {

  }

  interface IPaintedElem extends PathElem {

    void serialize(SerializerAdapter pOut);

  }

  private static abstract class PaintedElem implements IPaintedElem {
    final SVGPen mColor;

    PaintedElem(SVGPen pColor) {
      mColor = pColor;
    }

    void serializeFill(SerializerAdapter pOut) {
      serializeStyle(pOut, null, mColor, null);
    }

    void serializeStroke(SerializerAdapter pOut) {
      serializeStyle(pOut, mColor, null, null);
    }

  }

  private static abstract class BaseRect extends PaintedElem {
    final Rectangle aBounds;

    BaseRect(Rectangle pBounds, SVGPen pColor) {
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

  private static class Rect extends BaseRect {

    public Rect(Rectangle pBounds, SVGPen pColor) {
      super(pBounds, pColor);
    }

    @Override
    public void serialize(SerializerAdapter pOut) {
      serializeRect(pOut);
      serializeStroke(pOut);
      pOut.endTag(SVG_NAMESPACE, "rect");
    }

  }

  private static class FilledRect extends BaseRect {

    FilledRect(Rectangle pBounds, SVGPen pColor) {
      super(pBounds, pColor);
    }

    @Override
    public void serialize(SerializerAdapter pOut) {
      serializeRect(pOut);
      serializeFill(pOut);
      pOut.endTag(SVG_NAMESPACE, "rect");
    }

  }

  private static abstract class BaseRoundRect extends BaseRect {
    final double aRx;
    final double aRy;

    BaseRoundRect(Rectangle pBounds, double pRx, double pRy, SVGPen pColor) {
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

  private static class RoundRect extends BaseRoundRect {

    RoundRect(Rectangle pBounds, double pRx, double pRy, SVGPen pColor) {
      super(pBounds, pRx, pRy, pColor);
    }

    @Override
    public void serialize(SerializerAdapter pOut) {
      serializeRoundRect(pOut);
      serializeStroke(pOut);
      pOut.endTag(SVG_NAMESPACE, "rect");
    }

  }

  private static class FilledRoundRect extends BaseRoundRect {

    FilledRoundRect(Rectangle pBounds, double pRx, double pRy, SVGPen pColor) {
      super(pBounds, pRx, pRy, pColor);
    }

    @Override
    public void serialize(SerializerAdapter pOut) {
      serializeRoundRect(pOut);
      serializeFill(pOut);
      pOut.endTag(SVG_NAMESPACE, "rect");
    }

  }

  private static abstract class BaseCircle extends PaintedElem {

    final double mX;
    final double mY;
    final double mRadius;

    public BaseCircle(double pX, double pY, double pRadius, SVGPen pColor) {
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

  private static class Circle extends BaseCircle {

    public Circle(double pX, double pY, double pRadius, SVGPen pColor) {
      super(pX, pY, pRadius, pColor);
    }

    @Override
    public void serialize(SerializerAdapter pOut) {
      serializeCircle(pOut);
      serializeStroke(pOut);
      pOut.endTag(SVG_NAMESPACE, "circle");
    }

  }

  private static class FilledCircle extends BaseCircle {

    public FilledCircle(double pX, double pY, double pRadius, SVGPen pColor) {
      super(pX, pY, pRadius, pColor);
    }

    @Override
    public void serialize(SerializerAdapter pOut) {
      serializeCircle(pOut);
      serializeFill(pOut);
      pOut.endTag(SVG_NAMESPACE, "circle");
    }

  }

  private static class PaintedPath implements IPaintedElem {

    final SVGPath mPath;
    final SVGPen mStroke;
    final SVGPen mFill;

    public PaintedPath(SVGPath pPath, SVGPen pStroke, SVGPen pFill) {
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


  private static class DrawText extends PaintedElem {

    final TextPos mTextPos;
    final double mX;
    final double mY;
    final String mText;
    final double mFoldWidth;

    public DrawText(TextPos pTextPos, double pX, double pY, String pText, double pFoldWidth, SVGPen pColor) {
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

  private static class SubCanvas extends SVGCanvas implements IPaintedElem {

    final double aScale;

    SubCanvas(SVGStrategy pStrategy, double pScale) {
      super(pStrategy);
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

  private SVGStrategy aStrategy;

  private List<IPaintedElem> aPath = new ArrayList<>();

  public SVGCanvas(TextMeasurer pTextMeasurer) {
    aStrategy = new SVGStrategy(pTextMeasurer);
  }

  public static void serializeStyle(SerializerAdapter pOut, SVGPen pStroke, SVGPen pFill, TextPos pTextPos) {
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

  public SVGCanvas(SVGStrategy pStrategy) {
    aStrategy = pStrategy;
  }

  @Override
  public SVGStrategy getStrategy() {
    return aStrategy;
  }

  @Override
  public Canvas<SVGStrategy, SVGPen, SVGPath> childCanvas(Rectangle pArea, double pScale) {
    return new SubCanvas(aStrategy, pScale);
  }

  @Override
  public void drawFilledCircle(double pX, double pY, double pRadius, SVGPen pColor) {
    aPath.add(new FilledCircle(pX, pY, pRadius, pColor.clone()));
  }

  @Override
  public void drawRect(Rectangle pRect, SVGPen pColor) {
    aPath.add(new Rect(pRect, pColor));
  }

  @Override
  public void drawFilledRect(Rectangle pRect, SVGPen pColor) {
    aPath.add(new FilledRect(pRect, pColor));
  }

  @Override
  public void drawCircle(double pX, double pY, double pRadius, SVGPen pColor) {
    aPath.add(new Circle(pX, pY, pRadius, pColor));
  }

  @Override
  public void drawRoundRect(Rectangle pRect, double pRx, double pRy, SVGPen pColor) {
    aPath.add(new RoundRect(pRect, pRx, pRy, pColor));

  }

  @Override
  public void drawFilledRoundRect(Rectangle pRect, double pRx, double pRy, SVGPen pColor) {
    aPath.add(new FilledRoundRect(pRect, pRx, pRy, pColor));
  }

  @Override
  public void drawPoly(double[] pPoints, SVGPen pColor) {
    if (pPoints.length>1) {
      SVGPath path = pointsToPath(pPoints);
      drawPath(path, pColor, null);
    }
  }

  @Override
  public void drawFilledPoly(double[] pPoints, SVGPen pColor) {
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
  public void drawPath(SVGPath pPath, SVGPen pStroke, SVGPen pFill) {
    aPath.add(new PaintedPath(pPath, pStroke, pFill));
  }

  @Override
  public Theme<SVGStrategy, SVGPen, SVGPath> getTheme() {
    return new SVGTheme(aStrategy);
  }

  @Override
  public void drawText(TextPos pTextPos, double pX, double pY, String pText, double pFoldWidth,
                       SVGPen pPen) {
    aPath.add(new DrawText(pTextPos, pX, pY, pText, pFoldWidth, pPen.clone()));

  }

  public void serialize(SerializerAdapter pOut) {
    pOut.addNamespace(XMLConstants.DEFAULT_NS_PREFIX, SVG_NAMESPACE);
    pOut.endTag(SVG_NAMESPACE, "svg");
    pOut.addAttribute("version", "1.1");

    for (IPaintedElem element:aPath) {
      element.serialize(pOut);
    }

    pOut.endTag(SVG_NAMESPACE, "svg");
  }
}
