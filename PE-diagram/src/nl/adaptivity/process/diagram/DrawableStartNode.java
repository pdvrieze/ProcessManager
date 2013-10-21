package nl.adaptivity.process.diagram;
import static nl.adaptivity.process.diagram.DrawableProcessModel.*;
import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.DiagramPath;
import nl.adaptivity.diagram.Drawable;
import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.diagram.ItemCache;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.clientProcessModel.ClientStartNode;
import nl.adaptivity.process.processModel.StartNode;



public class DrawableStartNode extends ClientStartNode<DrawableProcessNode> implements DrawableProcessNode{

  private ItemCache aItems = new ItemCache();


  public DrawableStartNode(ClientProcessModel<DrawableProcessNode> pOwner) {
    super(pOwner);
  }

  public DrawableStartNode(String pId, ClientProcessModel<DrawableProcessNode> pOwner) {
    super(pId, pOwner);
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> PEN_T getFGPen(S pStrategy) {
    PEN_T result = aItems.getPen(pStrategy, 0);
    if (result==null) {
      result = pStrategy.newPen();
      result.setColor(0,0,0,0xff);
      aItems.setPen(pStrategy, 0, result);
    }
    return result;
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void setFGPen(S pStrategy, PEN_T pPen) {
    aItems.setPen(pStrategy, 0, pPen);
  }

  @Override
  public Rectangle getBounds() {
    return new Rectangle(getX()-STARTNODERADIUS, getY()-STARTNODERADIUS, STARTNODERADIUS*2+STROKEWIDTH, STARTNODERADIUS*2+STROKEWIDTH);
  }

  @Override
  public Drawable getItemAt(double pX, double pY) {
    final double realradius=STARTNODERADIUS+(STROKEWIDTH/2);
    return ((Math.abs(pX-getX())<=realradius) && (Math.abs(pY-getY())<=realradius)) ? this : null;
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void draw(Canvas<S, PEN_T, PATH_T> pCanvas, Rectangle pClipBounds) {
    if (hasPos()) {
      final S strategy = pCanvas.getStrategy();
      PEN_T fgPen = getFGPen(strategy);
      if (fgPen ==null) { fgPen = strategy.newPen().setColor(0,0,0,0xff); }
      pCanvas.drawFilledCircle(STARTNODERADIUS, STARTNODERADIUS, STARTNODERADIUS, fgPen);
    }
  }

  public static DrawableStartNode from(DrawableProcessModel pOwner, StartNode<?> pN) {
    DrawableStartNode result = new DrawableStartNode(pOwner);
    copyProcessNodeAttrs(pN, result);
    result.getImports().clear();
    result.getImports().addAll(pN.getImports());
    return result;
  }

}
