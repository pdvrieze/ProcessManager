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

import nl.adaptivity.diagram.DiagramPath;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.lang.Doubles;

import java.util.ArrayList;
import java.util.List;


public class SVGPath implements DiagramPath<SVGPath>{

  private interface IPathElem {

    void appendPathSpecTo(StringBuilder builder);

    void getBounds(Rectangle storage, IPathElem previous, final Pen<?> stroke);

    double getX();
    double getY();
  }

  private static abstract class OperTo implements IPathElem {
    final double mX;
    final double mY;

    public OperTo(double x, double y) {
      mX = x;
      mY = y;
    }

    @Override
    public double getX() {
      return mX;
    }

    @Override
    public double getY() {
      return mY;
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

    @Override
    public void getBounds(final Rectangle storage, final IPathElem previous, final Pen<?> stroke) {
      storage.set(mX, mY, 0d,0d);
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

    @Override
    public void getBounds(final Rectangle storage, final IPathElem previous, final Pen<?> stroke) {
      final double sw = stroke.getStrokeWidth();
      if (Doubles.isFinite(sw) && sw > 0d) {
        double hsw = sw / 2;
        double height = getY() - previous.getY();
        double width = getX() - previous.getX();
        double angle = Math.atan(height / width);
        double extendX = Math.abs(Math.sin(angle) * hsw);
        double extendY = Math.abs(Math.cos(angle) * hsw);
        storage.set(Math.min(previous.getX(), mX)-extendX, Math.min(previous.getY(), mY)-extendY, Math.abs(previous.getX() - mX)+2*extendX, Math.abs(previous.getY() - mY)+2*extendY);
      } else {
        storage.set(Math.min(previous.getX(), mX), Math.min(previous.getY(), mY), Math.abs(previous.getX() - mX), Math.abs(previous.getY() - mY));
      }
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

// Property accessors start
    @Override
    public void getBounds(final Rectangle storage, final IPathElem previous, final Pen<?> stroke) {
      final double hsw=stroke.getStrokeWidth()/2;
      final double previousX = previous.getX();
      final double previousY         = previous.getY();

      final double dx = previousX;
      final double cx = (3 * mCX1) - (3 * previousX);
      final double bx = ((3 * mCX2) - (6 * mCX1)) + (3 * previousX);
      final double ax = ((mX - (3 * mCX2)) + (3 * mCX1)) - previousX;

      final double dy = previousY;
      final double cy = (3 * mCY1) - (3 * previousY);
      final double by = ((3 * mCY2) - (6 * mCY1)) + (3 * previousY);
      final double ay = ((mY - (3 * mCY2)) + (3 * mCY1)) - previousY;

      final double dax = 3*(getX() - (3 * mCX2) + (3 * mCX1) - previousX);
      final double day = 3*(getY() - (3 * mCY2) + (3 * mCY1) - previousY);
      final double dbx = 2*(((3 * mCX2) - (6 * mCX1)) + (3 * previousX));
      final double dby = 2*(((3 * mCY2) - (6 * mCY1)) + (3 * previousY));
      final double dcx = (3 * mCX1) - (3 * previousX);
      final double dcy = (3 * mCY1) - (3 * previousY);


      double left = Math.min(previousX, mX);
      double right = Math.max(previousX, mX);
      double top = Math.min(previousY, mY);
      double bottom = Math.max(previousY, mY);

      double t1=0d;
      double t2=0d;
      double t3=0d;
      double t4=0d;

      if (dax!=0d) {
        t1 = (-dbx + Math.sqrt((dbx * dbx) - (4 * dax * dcx))) / (2 * dax);
        t2 = (-dbx - Math.sqrt((dbx * dbx) - (4 * dax * dcx))) / (2 * dax);
      }

      if (day!=0d) {
        t3 = (-dby + Math.sqrt((dby * dby) - (4 * day * dcy))) / (2 * day);
        t4 = (-dby - Math.sqrt((dby * dby) - (4 * day * dcy))) / (2 * day);
      }

      for(double t: new double[]{t1, t2, t3, t4}) {
        if ((t > 0d) && (t < 1d)) {
          final double x = (((((ax * t) + bx) * t) + cx) * t) + dx;
          left = Math.min(x, left);
          right = Math.max(x, right);

          final double y = (((((ay * t) + by) * t) + cy) * t) + dy;
          top = Math.min(y, top);
          bottom = Math.max(y, bottom);
        }
      }

      storage.set(left-hsw, top-hsw, right - left + stroke.getStrokeWidth(), bottom - top + stroke.getStrokeWidth());
      
    }
// Property acccessors end

  }

  private static class Close implements IPathElem {

    @Override
    public void appendPathSpecTo(StringBuilder builder) {
      builder.append("Z ");
    }

    @Override
    public void getBounds(final Rectangle storage, final IPathElem previous, final Pen<?> stroke) {
      storage.set(previous.getX(), previous.getY(), 0d, 0d);
    }

    @Override
    public double getX() {
      return Double.NaN;
    }

    @Override
    public double getY() {
      return Double.NaN;
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

  @Override
  public Rectangle getBounds(final Rectangle dest, final Pen<?> stroke) {
    if (mPath.size()==1) {
      final IPathElem elem = mPath.get(0);
      dest.set(elem.getX()-stroke.getStrokeWidth()/2, elem.getY()-stroke.getStrokeWidth()/2, stroke.getStrokeWidth(), stroke.getStrokeWidth());
    } else if (mPath.size()>1) {
      Rectangle tmpRect = new Rectangle(0d,0d,0d,0d);
      mPath.get(1).getBounds(dest, mPath.get(0), stroke);
      for (int i = 2; i < mPath.size(); i++) {
        mPath.get(i).getBounds(tmpRect, mPath.get(i-1), stroke);
        dest.extendBounds(tmpRect);
      }
    }
    return dest;
  }
}
