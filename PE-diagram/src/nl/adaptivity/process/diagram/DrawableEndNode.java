package nl.adaptivity.process.diagram;
import static nl.adaptivity.process.diagram.DrawableProcessModel.*;
import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.DiagramPath;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientEndNode;
import nl.adaptivity.process.processModel.EndNode;



public class DrawableEndNode<PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath> extends ClientEndNode<DrawableProcessNode<PEN_T, PATH_T>> implements DrawableProcessNode<PEN_T,PATH_T> {

  private PEN_T aFGPen;

  @Override
  public PEN_T getPen() {
    return aFGPen;
  }

  @Override
  public void setFGPen(PEN_T pPen) {
    aFGPen = pPen==null ? null : pPen.setStrokeWidth(ENDNODEOUTERSTROKEWIDTH);
  }

  @Override
  public Rectangle getBounds() {
    return new Rectangle(getX()-ENDNODEOUTERRADIUS, getY()-ENDNODEOUTERRADIUS, ENDNODEOUTERRADIUS*2 + ENDNODEOUTERSTROKEWIDTH, ENDNODEOUTERRADIUS*2 + ENDNODEOUTERSTROKEWIDTH);
  }

  @Override
  public void draw(Canvas<PEN_T, PATH_T> pCanvas, Rectangle pClipBounds) {
    if (hasPos()) {
      if (aFGPen ==null) { aFGPen = pCanvas.newColor(0,0,0,0xff).setStrokeWidth(ENDNODEOUTERSTROKEWIDTH); }
      pCanvas.drawCircle(ENDNODEOUTERRADIUS, ENDNODEOUTERRADIUS, ENDNODEOUTERRADIUS, aFGPen);
      pCanvas.drawFilledCircle(ENDNODEOUTERRADIUS, ENDNODEOUTERRADIUS, ENDNODEINNERRRADIUS, aFGPen);
    }
  }

  public static <PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath> DrawableEndNode<PEN_T, PATH_T> from(EndNode<?> pElem) {
    DrawableEndNode result = new DrawableEndNode();
    copyProcessNodeAttrs(pElem, result);
    return result;
  }

}
