package nl.adaptivity.process.diagram.svg;

import java.util.ArrayList;
import java.util.List;

import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.Theme;


public class SVGCanvas implements Canvas<SVGStrategy, SVGPen, SVGPath> {
  
  
  
  interface PathElem {
  
  }

  interface IPaintedElem extends PathElem {
    
  }
  
  private static abstract class PaintedElem implements IPaintedElem {
    final SVGPen mColor;

    PaintedElem(SVGPen pColor) {
      mColor = pColor;
    }
    
  }
  
  private static class BaseRect extends PaintedElem {
    final Rectangle aBounds;
    
    BaseRect(Rectangle pBounds, SVGPen pColor) {
      super(pColor);
      aBounds = pBounds;
    }
  }
  
  private static class FilledRect extends BaseRect {

    FilledRect(Rectangle pBounds, SVGPen pColor) {
      super(pBounds, pColor);
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
  }
  
  private static class RoundRect extends BaseRoundRect {

    RoundRect(Rectangle pBounds, double pRx, double pRy, SVGPen pColor) {
      super(pBounds, pRx, pRy, pColor);
    }
    
  }
  
  private static class FilledRoundRect extends BaseRoundRect {

    FilledRoundRect(Rectangle pBounds, double pRx, double pRy, SVGPen pColor) {
      super(pBounds, pRx, pRy, pColor);
    }
    
  }

  private static class Rect extends BaseRect {

    public Rect(Rectangle pBounds, SVGPen pColor) {
      super(pBounds, pColor);
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

  }
  
  private static class Circle extends BaseCircle {

    public Circle(double pX, double pY, double pRadius, SVGPen pColor) {
      super(pX, pY, pRadius, pColor);
    }
    
  }

  private static class FilledCircle extends BaseCircle {

    public FilledCircle(double pX, double pY, double pRadius, SVGPen pColor) {
      super(pX, pY, pRadius, pColor);
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
  
  }

  private static class SubCanvas extends SVGCanvas implements IPaintedElem {

    final double aScale;

    SubCanvas(SVGStrategy pStrategy, double pScale) {
      super(pStrategy);
      aScale = pScale;
    }

  }

  private SVGStrategy aStrategy;
  
  private List<IPaintedElem> aPath = new ArrayList<>(); 
  
  public SVGCanvas(TextMeasurer pTextMeasurer) {
    aStrategy = new SVGStrategy(pTextMeasurer);
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

}
