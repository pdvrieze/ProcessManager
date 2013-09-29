package nl.adaptivity.process.diagram;
import static nl.adaptivity.process.diagram.DrawableProcessModel.*;
import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientJoinNode;
import nl.adaptivity.process.processModel.Join;



public class DrawableJoin extends ClientJoinNode<DrawableProcessNode> implements DrawableProcessNode {

  private static final double STROKEEXTEND = Math.sqrt(2)*STROKEWIDTH;

  private Pen aFGPen;
  private Pen aWhite;

  @Override
  public Pen getPen() {
    return aFGPen;
  }

  @Override
  public void setFGPen(Pen pPen) {
    aFGPen = pPen.setStrokeWidth(STROKEWIDTH);
  }

  @Override
  public Rectangle getBounds() {
    double dx = JOINWIDTH/2;
    double dy = JOINHEIGHT/2;
    return new Rectangle(getX()-dx, getY()-dy, JOINHEIGHT+STROKEEXTEND, JOINWIDTH+STROKEEXTEND);
  }

  @Override
  public void draw(Canvas pCanvas, Rectangle pClipBounds) {
    if (hasPos()) {
      double dx = JOINWIDTH/2;
      double dy = JOINHEIGHT/2;
      if (aFGPen ==null) { aFGPen = pCanvas.newColor(0,0,0,0xff).setStrokeWidth(STROKEWIDTH); }
      if (aWhite ==null) { aWhite = pCanvas.newColor(0xff,0xff,0xff,0xff); }
      double[] points = new double[] {
        0,dy,
        dx, 0,
        JOINWIDTH, dy,
        dx, JOINHEIGHT,
        0, dy
      };
      pCanvas.drawFilledPath(points, aWhite);
      pCanvas.drawPath(points, aFGPen);
    }
  }

  public static DrawableJoin from(Join<?> pElem) {
    DrawableJoin result = new DrawableJoin();
    copyProcessNodeAttrs(pElem, result);
    result.setMin(pElem.getMin());
    result.setMax(pElem.getMax());
    return result;
  }

}
