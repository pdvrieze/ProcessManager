package nl.adaptivity.process.diagram;
import static nl.adaptivity.process.diagram.DrawableProcessModel.*;
import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.DiagramPath;
import nl.adaptivity.diagram.Drawable;
import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientEndNode;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.processModel.EndNode;



public class DrawableEndNode extends ClientEndNode<DrawableProcessNode> implements DrawableProcessNode {

  private int aState = STATE_DEFAULT;


  public DrawableEndNode(ClientProcessModel<DrawableProcessNode> pOwner) {
    super(pOwner);
  }

  public DrawableEndNode(String pId, ClientProcessModel<DrawableProcessNode> pOwner) {
    super(pId, pOwner);
  }

  @Override
  public Rectangle getBounds() {
    return new Rectangle(getX()-ENDNODEOUTERRADIUS, getY()-ENDNODEOUTERRADIUS, ENDNODEOUTERRADIUS*2 + ENDNODEOUTERSTROKEWIDTH, ENDNODEOUTERRADIUS*2 + ENDNODEOUTERSTROKEWIDTH);
  }

  @Override
  public Drawable getItemAt(double pX, double pY) {
    final double realradius=ENDNODEOUTERRADIUS+(ENDNODEOUTERSTROKEWIDTH/2);
    return ((Math.abs(pX-getX())<=realradius) && (Math.abs(pY-getY())<=realradius)) ? this : null;
  }

  @Override
  public int getState() {
    return aState ;
  }

  @Override
  public void setState(int pState) {
    aState = pState;
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void draw(Canvas<S, PEN_T, PATH_T> pCanvas, Rectangle pClipBounds) {
    if (hasPos()) {
      PEN_T outerLinePen = pCanvas.getTheme().getPen(ProcessThemeItems.ENDNODEOUTERLINE, aState & ~STATE_TOUCHED);
      PEN_T innerPen = pCanvas.getTheme().getPen(ProcessThemeItems.LINEBG, aState & ~STATE_TOUCHED);

      double hsw = ENDNODEOUTERSTROKEWIDTH/2;
      
      if ((aState&STATE_TOUCHED)!=0) {
        PEN_T touchedPen = pCanvas.getTheme().getPen(ProcessThemeItems.LINE, STATE_TOUCHED);
        pCanvas.drawCircle(ENDNODEOUTERRADIUS+hsw, ENDNODEOUTERRADIUS+hsw, ENDNODEOUTERRADIUS, touchedPen);
        pCanvas.drawCircle(ENDNODEOUTERRADIUS+hsw, ENDNODEOUTERRADIUS+hsw, ENDNODEINNERRRADIUS, touchedPen);
      }
      pCanvas.drawCircle(ENDNODEOUTERRADIUS+hsw, ENDNODEOUTERRADIUS+hsw, ENDNODEOUTERRADIUS, outerLinePen);
      pCanvas.drawFilledCircle(ENDNODEOUTERRADIUS+hsw, ENDNODEOUTERRADIUS+hsw, ENDNODEINNERRRADIUS, innerPen);
    }
  }

  public static  DrawableEndNode from(DrawableProcessModel pOwner, EndNode<?> pElem) {
    DrawableEndNode result = new DrawableEndNode(pOwner);
    copyProcessNodeAttrs(pElem, result);
    return result;
  }

}
