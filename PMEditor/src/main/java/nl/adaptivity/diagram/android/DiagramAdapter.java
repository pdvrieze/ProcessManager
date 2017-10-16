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

package nl.adaptivity.diagram.android;

import android.graphics.RectF;
import android.view.MotionEvent;
import nl.adaptivity.diagram.Point;
import nl.adaptivity.diagram.Theme;

import java.util.List;

/**
 * A {@link DiagramAdapter} is responsible for providing the diagram to the diagramView.
 * @author Paul de Vrieze
 *
 * @param <T> The type of child view.
 * @param <V> The type of the actual child nodes.
 */
public interface DiagramAdapter<T extends LightView, V> {

  /**
   * Return a point that is to be the attractor for the element when considering the given coordinates as reference
   * @param element The element number
   * @param x The x coordinate of the reference point
   * @param y The y coordinate of the reference point
   * @return A point that is the reference
   */
  Point closestAttractor(int element, double x, double y);

  int getCount();

  V getItem(int position);

  T getView(int position);

  List<? extends RelativeLightView> getRelativeDecorations(int position, double scale, boolean selected);

  LightView getBackground();

  LightView getOverlay();

  void getBounds(RectF diagramBounds);

  Theme<AndroidStrategy, AndroidPen, AndroidPath> getTheme();

  void onDecorationClick(DiagramView view, int position, LightView decoration);

  void onDecorationMove(DiagramView view, int position, RelativeLightView decoration, float x, float y);

  void onDecorationUp(DiagramView view, int position, RelativeLightView decoration, float x, float y);

  /** Called by a view to allow it to handle an event before any listeners.
   * @return <code>true</code> to stop propagation. <code>false</code> for unhandled events.
   */
  boolean onNodeClickOverride(DiagramView diagramView, int touchedElement, MotionEvent e);

  double getGravityX(int pos);

  double getGravityY(int pos);

}
