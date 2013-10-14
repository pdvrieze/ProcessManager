package nl.adaptivity.process.diagram;
import static nl.adaptivity.process.diagram.DrawableProcessModel.*;
import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.ItemCache;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientEndNode;
import nl.adaptivity.process.processModel.EndNode;



public class DrawableEndNode extends ClientEndNode<DrawableProcessNode> implements DrawableProcessNode {

  private ItemCache aItems = new ItemCache();


  @Override
  public <S extends DrawingStrategy<S>> Pen<S> getFGPen(S pStrategy) {
    Pen<S> result = aItems.getPen(pStrategy, 0);
    if (result==null) {
      result = pStrategy.newPen();
      result.setColor(0,0,0,0xff);
      result.setStrokeWidth(ENDNODEOUTERSTROKEWIDTH);
      aItems.setPen(pStrategy, 0, result);
    }
    return result;
  }

  @Override
  public <S extends DrawingStrategy<S>> void setFGPen(S pStrategy, Pen<S> pPen) {
    aItems.setPen(pStrategy, 0, pPen==null ? null : pPen.setStrokeWidth(ENDNODEOUTERSTROKEWIDTH));
  }

  @Override
  public Rectangle getBounds() {
    return new Rectangle(getX()-ENDNODEOUTERRADIUS, getY()-ENDNODEOUTERRADIUS, ENDNODEOUTERRADIUS*2 + ENDNODEOUTERSTROKEWIDTH, ENDNODEOUTERRADIUS*2 + ENDNODEOUTERSTROKEWIDTH);
  }

  @Override
  public <S extends DrawingStrategy<S>> void draw(Canvas<S> pCanvas, Rectangle pClipBounds) {
    if (hasPos()) {
      Pen<S> fgPen = getFGPen(pCanvas.getStrategy());
      pCanvas.drawCircle(ENDNODEOUTERRADIUS, ENDNODEOUTERRADIUS, ENDNODEOUTERRADIUS, fgPen);
      pCanvas.drawFilledCircle(ENDNODEOUTERRADIUS, ENDNODEOUTERRADIUS, ENDNODEINNERRRADIUS, fgPen);
    }
  }

  public static  DrawableEndNode from(EndNode<?> pElem) {
    DrawableEndNode result = new DrawableEndNode();
    copyProcessNodeAttrs(pElem, result);
    return result;
  }

}
