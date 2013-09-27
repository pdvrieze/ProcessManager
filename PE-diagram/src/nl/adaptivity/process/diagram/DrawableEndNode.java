package nl.adaptivity.process.diagram;
import static nl.adaptivity.process.diagram.DrawableProcessModel.*;
import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientEndNode;
import nl.adaptivity.process.processModel.EndNode;



public class DrawableEndNode extends ClientEndNode<DrawableProcessNode> implements DrawableProcessNode {

  private Pen aBlack;

  @Override
  public Rectangle getBounds() {
    return new Rectangle(getX()-ENDNODEOUTERRADIUS, getY()-ENDNODEOUTERRADIUS, ENDNODEOUTERRADIUS*2 + ENDNODEOUTERSTROKEWIDTH, ENDNODEOUTERRADIUS*2 + ENDNODEOUTERSTROKEWIDTH);
  }

  @Override
  public void draw(Canvas pCanvas, Rectangle pClipBounds) {
    if (aBlack ==null) { aBlack = pCanvas.newColor(0,0,0,0xff).setStrokeWidth(ENDNODEOUTERSTROKEWIDTH); }
    pCanvas.drawCircle(ENDNODEOUTERRADIUS, ENDNODEOUTERRADIUS, ENDNODEOUTERRADIUS, aBlack);
    pCanvas.drawFilledCircle(ENDNODEOUTERRADIUS, ENDNODEOUTERRADIUS, ENDNODEINNERRRADIUS, aBlack);
  }

  public static DrawableEndNode from(EndNode<?> pElem) {
    DrawableEndNode result = new DrawableEndNode();
    copyProcessNodeAttrs(pElem, result);
    return result;
  }

}
