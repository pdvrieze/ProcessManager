package nl.adaptivity.process.diagram.svg;

import java.util.ArrayList;
import java.util.List;

import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.Theme;
import nl.adaptivity.process.diagram.svg.SVGCanvas.PathElem;


public class SVGCanvas implements Canvas<SVGStrategy, SVGPen, SVGPath> {
  
  
  interface PathElem {
  
  }

  private static abstract class PaintedElem implements PathElem {
    SVGPen mColor;

    PaintedElem(SVGPen pColor) {
      mColor = pColor;
    }
    
  }
  
  private static class BaseRect extends PaintedElem {
    Rectangle aBounds;
    
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

  
  private static class SubCanvas extends SVGCanvas {

    final double aScale;

    SubCanvas(SVGStrategy pStrategy, double pScale) {
      super(pStrategy);
      aScale = pScale;
    }

  }

  private SVGStrategy aStrategy;
  
  private List<PathElem> aPath = new ArrayList<PathElem>(); 
  
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
    // TODO Auto-generated method stub
    
  }

  @Override
  public void drawFilledRoundRect(Rectangle pRect, double pRx, double pRy, SVGPen pColor) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void drawPoly(double[] pPoints, SVGPen pColor) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void drawFilledPoly(double[] pPoints, SVGPen pColor) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void drawPath(SVGPath pPath, SVGPen pStroke, SVGPen pFill) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Theme<SVGStrategy, SVGPen, SVGPath> getTheme() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void drawText(nl.adaptivity.diagram.Canvas.TextPos pTextPos, double pLeft, double pBaselineY, String pText, double pFoldWidth,
                       SVGPen pPen) {
    // TODO Auto-generated method stub
    
  }

}
