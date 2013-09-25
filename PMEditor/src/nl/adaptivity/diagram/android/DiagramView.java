package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.Diagram;
import nl.adaptivity.diagram.Rectangle;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;


public class DiagramView extends View {

  private Diagram aDiagram;

  public DiagramView(Context pContext, AttributeSet pAttrs, int pDefStyle) {
    super(pContext, pAttrs, pDefStyle);
  }

  public DiagramView(Context pContext, AttributeSet pAttrs) {
    super(pContext, pAttrs);
  }

  public DiagramView(Context pContext) {
    super(pContext);
  }

  public Diagram getDiagram() {
    return aDiagram;
  }

  public void setDiagram(Diagram pDiagram) {
    aDiagram = pDiagram;
  }

  @Override
  public void draw(Canvas pCanvas) {
    super.draw(pCanvas);
    final Rectangle clipBounds = new Rectangle(0d, 0d, getHeight(), getWidth());
    final AndroidCanvas canvas = new AndroidCanvas(pCanvas);
    getDiagram().draw(canvas, clipBounds);
  }

}
