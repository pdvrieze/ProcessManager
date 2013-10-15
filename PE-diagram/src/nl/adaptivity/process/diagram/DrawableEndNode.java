package nl.adaptivity.process.diagram;
import static nl.adaptivity.process.diagram.DrawableProcessModel.*;
import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.DiagramPath;
import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.diagram.ItemCache;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientEndNode;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.processModel.EndNode;



public class DrawableEndNode extends ClientEndNode<DrawableProcessNode> implements DrawableProcessNode {

  private ItemCache aItems = new ItemCache();


  public DrawableEndNode(ClientProcessModel<DrawableProcessNode> pOwner) {
    super(pOwner);
  }

  public DrawableEndNode(String pId, ClientProcessModel<DrawableProcessNode> pOwner) {
    super(pId, pOwner);
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> PEN_T getFGPen(S pStrategy) {
    PEN_T result = aItems.getPen(pStrategy, 0);
    if (result==null) {
      result = pStrategy.newPen();
      result.setColor(0,0,0,0xff);
      result.setStrokeWidth(ENDNODEOUTERSTROKEWIDTH);
      aItems.setPen(pStrategy, 0, result);
    }
    return result;
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void setFGPen(S pStrategy, PEN_T pPen) {
    aItems.setPen(pStrategy, 0, pPen==null ? null : pPen.setStrokeWidth(ENDNODEOUTERSTROKEWIDTH));
  }

  @Override
  public Rectangle getBounds() {
    return new Rectangle(getX()-ENDNODEOUTERRADIUS, getY()-ENDNODEOUTERRADIUS, ENDNODEOUTERRADIUS*2 + ENDNODEOUTERSTROKEWIDTH, ENDNODEOUTERRADIUS*2 + ENDNODEOUTERSTROKEWIDTH);
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void draw(Canvas<S, PEN_T, PATH_T> pCanvas, Rectangle pClipBounds) {
    if (hasPos()) {
      PEN_T fgPen = getFGPen(pCanvas.getStrategy());
      pCanvas.drawCircle(ENDNODEOUTERRADIUS, ENDNODEOUTERRADIUS, ENDNODEOUTERRADIUS, fgPen);
      pCanvas.drawFilledCircle(ENDNODEOUTERRADIUS, ENDNODEOUTERRADIUS, ENDNODEINNERRRADIUS, fgPen);
    }
  }

  public static  DrawableEndNode from(DrawableProcessModel pOwner, EndNode<?> pElem) {
    DrawableEndNode result = new DrawableEndNode(pOwner);
    copyProcessNodeAttrs(pElem, result);
    return result;
  }

}
