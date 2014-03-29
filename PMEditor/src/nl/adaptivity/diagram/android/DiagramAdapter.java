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

  V getItem(int pPosition);

  T getView(int pPosition);

  List<? extends RelativeLightView> getRelativeDecorations(int pPosition, double pScale, boolean pSelected);

  LightView getBackground();

  LightView getOverlay();

  void getBounds(RectF pDiagramBounds);

  Theme<AndroidStrategy, AndroidPen, AndroidPath> getTheme();

  void onDecorationClick(DiagramView pView, int pPosition, LightView pDecoration);

  void onDecorationMove(DiagramView pView, int pPosition, RelativeLightView pDecoration, float pX, float pY);

  void onDecorationUp(DiagramView pView, int pPosition, RelativeLightView pDecoration, float pX, float pY);

  /** Called by a view to allow it to handle an event before any listeners.
   * @return <code>true</code> to stop propagation. <code>false</code> for unhandled events.
   */
  boolean onNodeClickOverride(DiagramView pDiagramView, int pTouchedElement, MotionEvent pE);

}
