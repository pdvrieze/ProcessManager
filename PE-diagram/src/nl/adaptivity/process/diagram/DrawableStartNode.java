package nl.adaptivity.process.diagram;
import static nl.adaptivity.process.diagram.DrawableProcessModel.*;
import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientStartNode;
import nl.adaptivity.process.processModel.StartNode;



public class DrawableStartNode extends ClientStartNode<DrawableProcessNode> implements DrawableProcessNode{

  private Pen aBlack;

  @Override
  public Rectangle getBounds() {
    return new Rectangle(getX()-STARTNODERADIUS, getY()-STARTNODERADIUS, STARTNODERADIUS*2, STARTNODERADIUS*2);
  }

  @Override
  public void draw(Canvas pCanvas, Rectangle pClipBounds) {
    if (aBlack ==null) { aBlack = pCanvas.newColor(0,0,0,0xff); }
    pCanvas.drawFilledCircle(STARTNODERADIUS, STARTNODERADIUS, STARTNODERADIUS, aBlack);
  }

  public static DrawableStartNode from(StartNode<?> pN) {
    DrawableStartNode result = new DrawableStartNode();
    copyProcessNodeAttrs(pN, result);
    result.getImports().clear();
    result.getImports().addAll(pN.getImports());
    return result;
  }

}
