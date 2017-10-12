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

package nl.adaptivity.process.editor.android;


import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import nl.adaptivity.android.graphics.AbstractLightView;
import nl.adaptivity.android.graphics.LineView;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.Theme;
import nl.adaptivity.diagram.android.*;
import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.diagram.DrawableProcessNode;
import nl.adaptivity.process.diagram.ProcessThemeItems;
import nl.adaptivity.process.diagram.RootDrawableProcessModel;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.IdentifyableSet;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaseProcessAdapter implements DiagramAdapter<LWDrawableView, DrawableProcessNode> {

  protected static class ConnectorView extends AbstractLightView {

    private Paint mPen;
    private final RectF mBounds = new RectF();
    private final DrawableProcessModel mDiagram;

    public ConnectorView(final BaseProcessAdapter parent) {
      parent.getBounds(mBounds);
      mDiagram = parent.getDiagram();
    }

    @Override
    public void getBounds(final RectF dest) {
      dest.set(mBounds);
    }

    @Override
    public void move(final float x, final float y) { /* ignore */ }

    @Override
    public void setPos(final float x, final float y) { /* ignore */ }

    @Override
    public void draw(final Canvas canvas, final Theme<AndroidStrategy, AndroidPen, AndroidPath> theme, final double scale) {
      if (mDiagram==null) { return; }
      if (mPen ==null) { mPen = theme.getPen(ProcessThemeItems.LINE, nl.adaptivity.diagram.Drawable.STATE_DEFAULT).getPaint(); }
      for(final DrawableProcessNode start:mDiagram.getModelNodes()) {
        if (! (Double.isNaN(start.getX())|| Double.isNaN(start.getY()))) {
          for (final Identifiable endId: start.getSuccessors()) {
            final DrawableProcessNode end = start.getOwnerModel().getNode(endId);
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

  private final DrawableProcessModel mDiagram;
  protected final Map<DrawableProcessNode, LWDrawableView> mViewCache = new HashMap<>();
  private LightView mBackground;
  private final RectF   mBounds  = new RectF();
  private       boolean mInvalid = true;
  private AndroidTheme mTheme;

  public BaseProcessAdapter(final DrawableProcessModel diagram) {
    mDiagram = diagram;
  }

  public void updateItem(final int pos, final DrawableProcessNode newValue) {
    // TODO do this better
    ((List<DrawableProcessNode>) mDiagram.getModelNodes()).set(pos, newValue);
    invalidate();
  }

  @Override
  public int getCount() {
    if (mDiagram==null) { return 0; }
    return mDiagram.getModelNodes().size();
  }

  @Override
  public DrawableProcessNode getItem(final int pos) {
    // TODO do this better
    return mDiagram.getModelNodes().get(pos);
  }

  @Override
  public LWDrawableView getView(final int position) {
    final DrawableProcessNode item = getItem(position);
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
      DrawableProcessNode item = getItem(0);
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

  public boolean isInvalid() {
    return mInvalid;
  }


  public void invalidate() {
    mInvalid = true;
  }

  public DrawableProcessModel getDiagram() {
    return mDiagram;
  }

}