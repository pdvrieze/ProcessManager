package nl.adaptivity.process.diagram;
import static nl.adaptivity.process.diagram.DrawableProcessModel.*;
import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.DiagramPath;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientJoinNode;
import nl.adaptivity.process.processModel.Join;



public class DrawableJoin extends ClientJoinNode<DrawableProcessNode> implements DrawableProcessNode {

  private static final double STROKEEXTEND = Math.sqrt(2)*STROKEWIDTH;

  private Pen aFGPen;
  private Pen aWhite;
  private DiagramPath aPath;

  @Override
  public Pen getPen() {
    return aFGPen;
  }

  @Override
  public void setFGPen(Pen pPen) {
    aFGPen = pPen==null ? null : pPen.setStrokeWidth(STROKEWIDTH);
  }

  @Override
  public Rectangle getBounds() {
    double dx = JOINWIDTH/2;
    double dy = JOINHEIGHT/2;
    return new Rectangle(getX()-dx, getY()-dy, JOINHEIGHT+STROKEEXTEND, JOINWIDTH+STROKEEXTEND);
  }

  @Override
  public void draw(Canvas pCanvas, Rectangle pClipBounds) {
    if (aPath==null) {
      final double dx = JOINWIDTH/2;
      final double dy = JOINHEIGHT/2;
      aPath = pCanvas.newPath();
      aPath.moveTo(0,dy)
           .lineTo(dx, 0)
           .lineTo(JOINWIDTH, dy)
           .lineTo(dx, JOINHEIGHT)
           .close();

    }
    if (hasPos()) {
      if (aFGPen ==null) { aFGPen = pCanvas.newColor(0,0,0,0xff).setStrokeWidth(STROKEWIDTH); }
      if (aWhite ==null) { aWhite = pCanvas.newColor(0xff,0xff,0xff,0xff); }
      pCanvas.drawFilledPath(aPath, aWhite);
      pCanvas.drawPath(aPath, aFGPen);
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
