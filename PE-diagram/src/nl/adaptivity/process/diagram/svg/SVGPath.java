package nl.adaptivity.process.diagram.svg;

import java.util.ArrayList;
import java.util.List;

import nl.adaptivity.diagram.DiagramPath;


public class SVGPath implements DiagramPath<SVGPath>{

  private interface IPathElem {

    void appendPathSpecTo(StringBuilder builder);

  }

  private static abstract class OperTo implements IPathElem {
    final double mX;
    final double mY;

    public OperTo(double x, double y) {
      mX = x;
      mY = y;
    }

  }

  private static class MoveTo extends OperTo {
    public MoveTo(double x, double y) {
      super(x, y);
    }

    @Override
    public void appendPathSpecTo(StringBuilder builder) {
      builder.append("M").append(mX).append(' ').append(mY).append(' ');
    }
  }

  private static class LineTo extends OperTo {
    public LineTo(double x, double y) {
      super(x, y);
    }

    @Override
    public void appendPathSpecTo(StringBuilder builder) {
      builder.append("L").append(mX).append(' ').append(mY).append(' ');
    }
  }

  private static class CubicTo extends OperTo {
    private final double mCX1;
    private final double mCY1;
    private final double mCX2;
    private final double mCY2;

    public CubicTo(double cX1, double cY1, double cX2, double cY2, double x, double y) {
      super(x, y);
      mCX1 = cX1;
      mCY1 = cY1;
      mCX2 = cX2;
      mCY2 = cY2;
    }

    @Override
    public void appendPathSpecTo(StringBuilder builder) {
      builder.append("C").append(mCX1).append(' ').append(mCY1).append(' ')
                          .append(mCX2).append(' ').append(mCY2).append(' ')
                          .append(mX).append(' ').append(mY).append(' ');
    }
  }

  private static class Close implements IPathElem {

    @Override
    public void appendPathSpecTo(StringBuilder builder) {
      builder.append("Z ");
    }

  }

  private List<IPathElem> mPath = new ArrayList<>();

  @Override
  public SVGPath moveTo(double x, double y) {
    mPath.add(new MoveTo(x, y));
    return this;
  }

  @Override
  public SVGPath lineTo(double x, double y) {
    mPath.add(new LineTo(x, y));
    return this;
  }

  @Override
  public SVGPath cubicTo(double x1, double y1, double x2, double y2, double x3, double y3) {
    mPath.add(new CubicTo(x1, y1, x2, y2, x3, y3));
    return this;
  }

  @Override
  public SVGPath close() {
    mPath.add(new Close());
    return this;
  }

  public String toPathData() {
    StringBuilder result = new StringBuilder();
    for(IPathElem elem: mPath) {
      elem.appendPathSpecTo(result);
    }
    return result.toString();
  }

}
