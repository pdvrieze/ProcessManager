/*
 * Copyright (c) 2018.
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

package nl.adaptivity.process.editor.android;


import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import net.devrieze.util.CollectionUtil;
import nl.adaptivity.android.graphics.AbstractLightView;
import nl.adaptivity.android.graphics.LineView;
import nl.adaptivity.diagram.Bounded;
import nl.adaptivity.diagram.Point;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.Theme;
import nl.adaptivity.diagram.android.*;
import nl.adaptivity.process.diagram.*;
import nl.adaptivity.process.diagram.DrawableProcessModel.Builder;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identified;

import java.util.*;


public class BaseProcessAdapter implements DiagramAdapter<LWDrawableView, DrawableProcessNode.Builder> {

  protected static class ConnectorView extends AbstractLightView {

    private Paint mPen;
    private final RectF mBounds = new RectF();
    private final DrawableProcessModel.Builder mDiagram;

    public ConnectorView(final BaseProcessAdapter parent) {
      parent.getBounds(mBounds);
      mDiagram = parent.getDiagram();
    }

    @Override
    public void getBounds(final RectF dest) {
      dest.set(mBounds);
    }

    @Override
    public void draw(final Canvas canvas, final Theme<AndroidStrategy, AndroidPen, AndroidPath> theme, final double scale) {
      if (mDiagram==null) { return; }
      if (mPen ==null) { mPen = theme.getPen(ProcessThemeItems.LINE, nl.adaptivity.diagram.Drawable.STATE_DEFAULT).getPaint(); }
      for(final IDrawableProcessNode start:new ArrayList<>(mDiagram.getChildElements())) {
        if (! (Double.isNaN(start.getX())|| Double.isNaN(start.getY()))) {
          for (final Identifiable endId: start.getSuccessors()) {
            final DrawableProcessNode.Builder end = mDiagram.getNode(endId.getId());
            if ((end != null) && !(Double.isNaN(end.getX()) || Double.isNaN(end.getY()))) {
              final float x1 =
                (float) ((start.getBounds().right()/*-DrawableProcessModel.STROKEWIDTH*/ - mBounds.left) * scale);
              final float y1 = (float) ((start.getY() - mBounds.top) * scale);
              final float x2 =
                (float) ((end.getBounds().left/*+DrawableProcessModel.STROKEWIDTH*/ - mBounds.left) * scale);
              final float y2 = (float) ((end.getY() - mBounds.top) * scale);
//              pCanvas.drawLine(x1, y1, x2, y2, mPen);
              LineView.drawArrow(canvas, theme, x1, y1, x2, y2, scale);
            }
          }
        }
      }
    }

  }

  private final Builder mDiagram;
  protected final Map<DrawableProcessNode.Builder, LWDrawableView> mViewCache = new HashMap<>();
  private LightView mBackground;
  private final RectF   mBounds  = new RectF();
  private       boolean mInvalid = true;
  private AndroidTheme mTheme;

  public BaseProcessAdapter(final DrawableProcessModel.Builder diagram) {
    mDiagram = diagram;
  }

  public void updateItem(final int pos, final DrawableProcessNode.Builder newValue) {
    // TODO do this better
    mDiagram.getNodes().set(pos, newValue);
    invalidate();
  }

  @Override
  public int getCount() {
    if (mDiagram==null) { return 0; }
    return mDiagram.getNodes().size();
  }

  @Override
  public DrawableProcessNode.Builder getItem(final int pos) {
    // TODO do this better
    return (DrawableProcessNode.Builder) mDiagram.getNodes().get(pos);
  }

  @Override
  public LWDrawableView getView(final int position) {
    final DrawableProcessNode.Builder item = getItem(position);
    LWDrawableView result = mViewCache.get(item);
    if (result!=null) {
      return result;
    }
    result = new LWProcessDrawableView(item);
    mViewCache.put(item, result);
    return result;
  }

  @Override
  public List<? extends RelativeLightView> getRelativeDecorations(final int position, final double scale, final boolean selected) {
    return Collections.emptyList();
  }

  @Override
  public LightView getBackground() {
    if (mBackground==null) { mBackground = new ConnectorView(this); }
    return mBackground;
  }

  @Override
  public LightView getOverlay() {
    return null;
  }

  @Override
  public void getBounds(final RectF diagramBounds) {
    if (mInvalid) {
      final int len = getCount();
      if (len==0) {
        diagramBounds.set(0f, 0f, 0f, 0f);
        return;
      }
      Bounded   item   = getItem(0);
      Rectangle bounds = item.getBounds();
      mBounds.set((float) bounds.left, (float) bounds.top, bounds.rightf(), bounds.bottomf());
      for(int i=1; i<len; ++i) {
        item = getItem(i);
        bounds = item.getBounds();
        mBounds.left=Math.min(mBounds.left, (float) bounds.left);
        mBounds.top=Math.min(mBounds.top, (float) bounds.top);
        mBounds.right=Math.max(mBounds.right, bounds.rightf());
        mBounds.bottom=Math.max(mBounds.bottom, bounds.bottomf());

      }
      mInvalid = false;
    }
    diagramBounds.set(mBounds);
  }

  @Override
  public AndroidTheme getTheme() {
    if (mTheme ==null) { mTheme = new AndroidTheme(AndroidStrategy.INSTANCE); }
    return mTheme;
  }

  @Override
  public void onDecorationClick(final DiagramView view, final int position, final LightView decoration) {
    // ignore
  }

  @Override
  public void onDecorationMove(final DiagramView view, final int position, final RelativeLightView decoration, final float x, final float y) {
    //ignore
  }

  @Override
  public void onDecorationUp(final DiagramView view, final int position, final RelativeLightView decoration, final float x, final float y) {
    //ignore
  }

  @Override
  public boolean onNodeClickOverride(final DiagramView diagramView, final int touchedElement, final MotionEvent e) {
    //ignore
    return false;
  }

  @Override
  public double getGravityX(final int pos) {
    return getItem(pos).getX();
  }

  @Override
  public double getGravityY(final int pos) {
    return getItem(pos).getY();
  }

  @Override
  public Point closestAttractor(final int element, final double x, final double y) {
    DrawableProcessNode.Builder node = getItem(element);
    double attrX = Double.NaN;
    double attrY = Double.NaN;
    double minDy = Double.POSITIVE_INFINITY;
    for (Identified predId : CollectionUtil.combine(node.getPredecessors(), node.getSuccessors())) {
      DrawableProcessNode.Builder pred = mDiagram.getNode(predId.getId());
      double dy = Math.abs(pred.getY()-y);
      if (dy<minDy) {
        minDy = dy;
        attrY = pred.getY();
      }
    }
    if (!Double.isNaN(attrY)) {
      return Point.Companion.of(node.getX(), attrY);
    }
    return null;
  }

  @Override
  public void setPos(final int element, final double diagx, final double diagy) {
    DrawableProcessNode.Builder item = getItem(element);
    item.setX(diagx);
    item.setY(diagy);
  }

  public boolean isInvalid() {
    return mInvalid;
  }


  public void invalidate() {
    mInvalid = true;
  }

  public DrawableProcessModel.Builder getDiagram() {
    return mDiagram;
  }

}