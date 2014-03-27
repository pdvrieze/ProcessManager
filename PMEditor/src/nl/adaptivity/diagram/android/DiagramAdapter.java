package nl.adaptivity.diagram.android;

import java.util.List;

import nl.adaptivity.diagram.Theme;
import android.graphics.RectF;

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

}
