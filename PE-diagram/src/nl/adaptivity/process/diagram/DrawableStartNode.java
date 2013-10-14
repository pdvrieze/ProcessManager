package nl.adaptivity.process.diagram;
import static nl.adaptivity.process.diagram.DrawableProcessModel.*;
import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.diagram.ItemCache;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientStartNode;
import nl.adaptivity.process.processModel.StartNode;



public class DrawableStartNode extends ClientStartNode<DrawableProcessNode> implements DrawableProcessNode{

  private ItemCache aItems = new ItemCache();


  @Override
  public <S extends DrawingStrategy<S>> Pen<S> getFGPen(S pStrategy) {
    Pen<S> result = aItems.getPen(pStrategy, 0);
    if (result==null) {
      result = pStrategy.newPen();
      result.setColor(0,0,0,0xff);
      aItems.setPen(pStrategy, 0, result);
    }
    return result;
  }

  @Override
  public <S extends DrawingStrategy<S>> void setFGPen(S pStrategy, Pen<S> pPen) {
    aItems.setPen(pStrategy, 0, pPen==null ? null : pPen.setStrokeWidth(STROKEWIDTH));
  }

  @Override
  public Rectangle getBounds() {
    return new Rectangle(getX()-STARTNODERADIUS, getY()-STARTNODERADIUS, STARTNODERADIUS*2+STROKEWIDTH, STARTNODERADIUS*2+STROKEWIDTH);
  }

  @Override
  public <S extends DrawingStrategy<S>> void draw(Canvas<S> pCanvas, Rectangle pClipBounds) {
    if (hasPos()) {
      Pen<S> fgPen = getFGPen(pCanvas.getStrategy());
      if (fgPen ==null) { fgPen = pCanvas.newColor(0,0,0,0xff); }
      pCanvas.drawFilledCircle(STARTNODERADIUS, STARTNODERADIUS, STARTNODERADIUS, fgPen);
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
