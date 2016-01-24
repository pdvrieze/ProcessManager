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
import nl.adaptivity.process.util.Identifiable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaseProcessAdapter implements DiagramAdapter<LWDrawableView, DrawableProcessNode> {

  protected static class ConnectorView extends AbstractLightView {

    private Paint mPen;
    private RectF mBounds = new RectF();
    private DrawableProcessModel mDiagram;

    public ConnectorView(BaseProcessAdapter parent) {
      parent.getBounds(mBounds);
      mDiagram = parent.getDiagram();
    }

    @Override
    public void getBounds(RectF dest) {
      dest.set(mBounds);
    }

    @Override
    public void move(float x, float y) { /* ignore */ }

    @Override
    public void setPos(float x, float y) { /* ignore */ }

    @Override
    public void draw(Canvas canvas, Theme<AndroidStrategy, AndroidPen, AndroidPath> theme, double scale) {
      if (mPen ==null) { mPen = theme.getPen(ProcessThemeItems.LINE, nl.adaptivity.diagram.Drawable.STATE_DEFAULT).getPaint(); }
      if (mDiagram==null) { return; }
      for(DrawableProcessNode start:mDiagram.getModelNodes()) {
        if (! (Double.isNaN(start.getX())|| Double.isNaN(start.getY()))) {
          for (Identifiable endId: start.getSuccessors()) {
            DrawableProcessNode end = start.getOwnerModel().getNode(endId);
            if (! (Double.isNaN(end.getX())|| Double.isNaN(end.getY()))) {
              final float x1 = (float) ((start.getBounds().right()/*-DrawableProcessModel.STROKEWIDTH*/ - mBounds.left) * scale);
              final float y1 = (float) ((start.getY()-mBounds.top)*scale);
              final float x2 = (float) ((end.getBounds().left/*+DrawableProcessModel.STROKEWIDTH*/ - mBounds.left) * scale);
              final float y2 = (float) ((end.getY()-mBounds.top)* scale);
//              pCanvas.drawLine(x1, y1, x2, y2, mPen);
              LineView.drawArrow(canvas, theme, x1, y1, x2, y2, scale);
            }
          }
        }
      }
    }

  }

  private DrawableProcessModel mDiagram;
  protected Map<DrawableProcessNode, LWDrawableView> mViewCache = new HashMap<>();
  private LightView mBackground;
  private RectF mBounds = new RectF();
  private boolean mInvalid = true;
  private AndroidTheme mTheme;

  public BaseProcessAdapter(DrawableProcessModel diagram) {
    mDiagram = diagram;
  }

  public void updateItem(final int pos, final DrawableProcessNode newValue) {
    mDiagram.setNode(pos, newValue);
    invalidate();
  }

  @Override
  public int getCount() {
    if (mDiagram==null) { return 0; }
    return mDiagram.getModelNodes().size();
  }

  @Override
  public DrawableProcessNode getItem(int pos) {
    return mDiagram.getNode(pos);
  }

  @Override
  public LWDrawableView getView(int position) {
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
  public List<? extends RelativeLightView> getRelativeDecorations(int position, double scale, boolean selected) {
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
  public void getBounds(RectF diagramBounds) {
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
  public void onDecorationClick(DiagramView view, int position, LightView decoration) {
    // ignore
  }

  @Override
  public void onDecorationMove(DiagramView view, int position, RelativeLightView decoration, float x, float y) {
    //ignore
  }

  @Override
  public void onDecorationUp(DiagramView view, int position, RelativeLightView decoration, float x, float y) {
    //ignore
  }

  @Override
  public boolean onNodeClickOverride(DiagramView diagramView, int touchedElement, MotionEvent e) {
    //ignore
    return false;
  }

  @Override
  public double getGravityX(int pos) {
    return getItem(pos).getX();
  }

  @Override
  public double getGravityY(int pos) {
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