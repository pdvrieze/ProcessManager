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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.diagram;

import nl.adaptivity.diagram.*;
import nl.adaptivity.diagram.Canvas.TextPos;
import nl.adaptivity.process.processModel.JoinSplit;
import nl.adaptivity.process.processModel.JoinSplitBase;
import org.jetbrains.annotations.Nullable;

import static nl.adaptivity.process.diagram.DrawableProcessModel.JOINHEIGHT;
import static nl.adaptivity.process.diagram.DrawableProcessModel.JOINWIDTH;


public class DrawableJoinSplitDelegate {

  protected final ItemCache mItems = new ItemCache();
  int mState;

  public DrawableJoinSplitDelegate() { this.mState = DrawableProcessModel.STATE_DEFAULT;}

  public DrawableJoinSplitDelegate(final DrawableJoinSplitDelegate orig) {
    mState = orig.mState;
  }

  public void setLogicalPos(JoinSplitBase<?,?> elem, double left, double top) {
    elem.setX(left + DrawableJoinSplit.REFERENCE_OFFSET_X);
    elem.setY(left + DrawableJoinSplit.REFERENCE_OFFSET_Y);
  }

  public static Rectangle getBounds(JoinSplit<?,?> elem) {
    return new Rectangle(elem.getX()-DrawableJoinSplit.REFERENCE_OFFSET_X, elem.getY()-DrawableJoinSplit.REFERENCE_OFFSET_Y, JOINHEIGHT+DrawableJoinSplit.STROKEEXTEND, JOINWIDTH+DrawableJoinSplit.STROKEEXTEND);
  }

  @Nullable
  public static Drawable getItemAt(DrawableJoinSplit elem, double x, double y) {
    final double realradiusX = (DrawableProcessModel.JOINWIDTH + DrawableJoinSplit.STROKEEXTEND) / 2;
    final double realradiusY = (DrawableProcessModel.JOINHEIGHT + DrawableJoinSplit.STROKEEXTEND) / 2;
    return ((Math.abs(x - elem.getX()) <= realradiusX) && (Math.abs(y - elem.getY()) <= realradiusY)) ? elem : null;
  }

  public int getState() {
    return mState;
  }

  public void setState(int state) {
    mState = state;
  }

  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void draw(JoinSplitBase<?,?> elem, Canvas<S, PEN_T, PATH_T> canvas, Rectangle clipBounds) {
    final S strategy = canvas.getStrategy();
    PATH_T path = mItems.getPath(strategy, 0);
    final double dx = DrawableProcessModel.JOINWIDTH / 2;
    final double hse = DrawableJoinSplit.STROKEEXTEND / 2;
    if (path == null) {
      final double dy = DrawableProcessModel.JOINHEIGHT / 2;
      path = strategy.newPath();
      path.moveTo(hse, dy + hse)
          .lineTo(dx + hse, hse)
          .lineTo(DrawableProcessModel.JOINWIDTH + hse, dy + hse)
          .lineTo(dx + hse, DrawableProcessModel.JOINHEIGHT + hse)
          .close();
      mItems.setPath(strategy, 0, path);
    }
    if (elem.hasPos()) {
      PEN_T linePen = canvas.getTheme().getPen(ProcessThemeItems.LINE, mState & ~DrawableProcessModel.STATE_TOUCHED);
      PEN_T bgPen = canvas.getTheme().getPen(ProcessThemeItems.BACKGROUND, mState);

      if ((mState & DrawableProcessModel.STATE_TOUCHED) != 0) {
        PEN_T touchedPen = canvas.getTheme().getPen(ProcessThemeItems.LINE, DrawableProcessModel.STATE_TOUCHED);
        canvas.drawPath(path, touchedPen, null);
      }
      canvas.drawPath(path, linePen, bgPen);

      if (elem.getOwnerModel() != null || elem.getMin() >= 0 || elem.getMax() >= 0) {
        PEN_T textPen = canvas.getTheme().getPen(ProcessThemeItems.DIAGRAMTEXT, mState);
        String s = getMinMaxText(elem);

        canvas.drawText(TextPos.DESCENT, hse + dx, -hse, s, Double.MAX_VALUE, textPen);
      }
    }
  }

  public static String getMinMaxText(JoinSplitBase<?,?> elem) {
    if (DrawableJoinSplit.TEXT_DESC) {
      if (elem.getMin() == 1 && elem.getMax() == 1) {
        return "xor";
      } else if (elem.getMin() == 1 && elem.getMax() >= elem.getSuccessors().size()) {
        return "or";
      } else if (elem.getMin() == elem.getMax() && elem.getMax() >= elem.getSuccessors().size()) {
        return "and";
      }
    }
    StringBuilder str = new StringBuilder();
    if (elem.getMin() < 0) {
      str.append("?...");
    } else if (elem.getMin() == elem.getMax()) {
      return Integer.toString(elem.getMin());
    } else {
      str.append(elem.getMin()).append("...");
    }
    if (elem.getMax() < 0) {
      str.append("?");
    } else {
      str.append(elem.getMax());
    }
    return str.toString();
  }
}