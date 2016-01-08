package nl.adaptivity.diagram.android;

import java.util.List;

import nl.adaptivity.diagram.Theme;
import android.graphics.RectF;
import android.view.MotionEvent;

/**
 * A {@link DiagramAdapter} is responsible for providing the diagram to the diagramView.
 * @author Paul de Vrieze
 *
 * @param <T> The type of child view.
 * @param <V> The type of the actual child nodes.
 */
public interface DiagramAdapter<T extends LightView, V> {

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
