package nl.adaptivity.process.diagram;
import static nl.adaptivity.process.diagram.DrawableProcessModel.*;
import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.Color;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientEndNode;
import nl.adaptivity.process.processModel.EndNode;



public class DrawableEndNode extends ClientEndNode<DrawableProcessNode> implements DrawableProcessNode {

  private Color aBlack;

  @Override
  public Rectangle getBounds() {
    return new Rectangle(getX()-ENDNODEOUTERRADIUS, getY()-ENDNODEOUTERRADIUS, ENDNODEOUTERRADIUS*2, ENDNODEOUTERRADIUS*2);
  }

  @Override
  public void draw(Canvas pCanvas, Rectangle pClipBounds) {
    if (aBlack ==null) { aBlack = pCanvas.newColor(0,0,0,0xff); }
    pCanvas.drawCircle(ENDNODEOUTERRADIUS, ENDNODEOUTERRADIUS, ENDNODEOUTERRADIUS, aBlack);
    pCanvas.drawFilledCircle(ENDNODEOUTERRADIUS, ENDNODEOUTERRADIUS, ENDNODEINNERRRADIUS, aBlack);
  }

  public static DrawableEndNode from(EndNode<?> pElem) {
    DrawableEndNode result = new DrawableEndNode();
    copyProcessNodeAttrs(pElem, result);
    return result;
  }

}
