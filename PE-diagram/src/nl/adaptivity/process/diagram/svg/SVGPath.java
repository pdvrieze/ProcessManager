package nl.adaptivity.process.diagram.svg;

import java.util.ArrayList;
import java.util.List;

import nl.adaptivity.diagram.DiagramPath;


public class SVGPath implements DiagramPath<SVGPath>{

  private interface IPathElem {

    void appendPathSpecTo(StringBuilder builder);

  }

  private static abstract class OperTo implements IPathElem {
    final double aX;
    final double aY;

    public OperTo(double x, double y) {
      aX = x;
      aY = y;
    }

  }

  private static class MoveTo extends OperTo {
    public MoveTo(double x, double y) {
      super(x, y);
    }

    @Override
    public void appendPathSpecTo(StringBuilder builder) {
      builder.append("M").append(aX).append(' ').append(aY).append(' ');
    }
  }

  private static class LineTo extends OperTo {
    public LineTo(double x, double y) {
      super(x, y);
    }

    @Override
    public void appendPathSpecTo(StringBuilder builder) {
      builder.append("L").append(aX).append(' ').append(aY).append(' ');
    }
  }

  private static class CubicTo extends OperTo {
    private final double aCX1;
    private final double aCY1;
    private final double aCX2;
    private final double aCY2;

    public CubicTo(double cX1, double cY1, double cX2, double cY2, double x, double y) {
      super(x, y);
      aCX1 = cX1;
      aCY1 = cY1;
      aCX2 = cX2;
      aCY2 = cY2;
    }

    @Override
    public void appendPathSpecTo(StringBuilder builder) {
      builder.append("C").append(aCX1).append(' ').append(aCY1).append(' ')
                          .append(aCX2).append(' ').append(aCY2).append(' ')
                          .append(aX).append(' ').append(aY).append(' ');
    }
  }

  private static class Close implements IPathElem {

    @Override
    public void appendPathSpecTo(StringBuilder builder) {
      builder.append("Z ");
    }

  }

  private List<IPathElem> aPath = new ArrayList<>();

  @Override
  public SVGPath moveTo(double x, double y) {
    aPath.add(new MoveTo(x, y));
    return this;
  }

  @Override
  public SVGPath lineTo(double x, double y) {
    aPath.add(new LineTo(x, y));
    return this;
  }

  @Override
  public SVGPath cubicTo(double x1, double y1, double x2, double y2, double x3, double y3) {
    aPath.add(new CubicTo(x1, y1, x2, y2, x3, y3));
    return this;
  }

  @Override
  public SVGPath close() {
    aPath.add(new Close());
    return this;
  }

  public String toPathData() {
    StringBuilder result = new StringBuilder();
    for(IPathElem elem: aPath) {
      elem.appendPathSpecTo(result);
    }
    return result.toString();
  }

}
