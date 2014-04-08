package nl.adaptivity.process.diagram.svg;

import java.util.ArrayList;
import java.util.List;

import nl.adaptivity.diagram.DiagramPath;
import nl.adaptivity.process.diagram.svg.SVGCanvas.PathElem;


public class SVGPath implements DiagramPath<SVGPath>{

  
  
  private static abstract class OperTo implements PathElem {
    private final double aX;
    private final double aY;

    public OperTo(double pX, double pY) {
      aX = pX;
      aY = pY;
    }

  }
  
  private static class MoveTo extends OperTo {
    public MoveTo(double pX, double pY) {
      super(pX, pY);
    }
  }
  
  private static class LineTo extends OperTo {
    public LineTo(double pX, double pY) {
      super(pX, pY);
    }
  }
  
  private static class CubicTo extends OperTo {
    private final double aCX1;
    private final double aCY1;
    private final double aCX2;
    private final double aCY2;

    public CubicTo(double pCX1, double pCY1, double pCX2, double pCY2, double pX, double pY) {
      super(pX, pY);
      aCX1 = pCX1;
      aCY1 = pCY1;
      aCX2 = pCX2;
      aCY2 = pCY2;
    }
  }

  private static class Close implements PathElem {
  
  }

  private List<PathElem> aPath = new ArrayList<>();

  @Override
  public SVGPath moveTo(double pX, double pY) {
    aPath.add(new MoveTo(pX, pY));
    return this;
  }

  @Override
  public SVGPath lineTo(double pX, double pY) {
    aPath.add(new LineTo(pX, pY));
    return this;
  }

  @Override
  public SVGPath cubicTo(double pX1, double pY1, double pX2, double pY2, double pX3, double pY3) {
    aPath.add(new CubicTo(pX1, pY1, pX2, pY2, pX3, pY3));
    return this;
  }

  @Override
  public SVGPath close() {
    aPath.add(new Close());
    return this;
  }

}
