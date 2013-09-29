package nl.adaptivity.process.diagram;
import static nl.adaptivity.process.diagram.DrawableProcessModel.*;
import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientStartNode;
import nl.adaptivity.process.processModel.StartNode;



public class DrawableStartNode extends ClientStartNode<DrawableProcessNode> implements DrawableProcessNode{

  private Pen aFGPen;

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
    return new Rectangle(getX()-STARTNODERADIUS, getY()-STARTNODERADIUS, STARTNODERADIUS*2+STROKEWIDTH, STARTNODERADIUS*2+STROKEWIDTH);
  }

  @Override
  public void draw(Canvas pCanvas, Rectangle pClipBounds) {
    if (hasPos()) {
      if (aFGPen ==null) { aFGPen = pCanvas.newColor(0,0,0,0xff); }
      pCanvas.drawFilledCircle(STARTNODERADIUS, STARTNODERADIUS, STARTNODERADIUS, aFGPen);
    }
  }

  public static DrawableStartNode from(StartNode<?> pN) {
    DrawableStartNode result = new DrawableStartNode();
    copyProcessNodeAttrs(pN, result);
    result.getImports().clear();
    result.getImports().addAll(pN.getImports());
    return result;
  }

}
