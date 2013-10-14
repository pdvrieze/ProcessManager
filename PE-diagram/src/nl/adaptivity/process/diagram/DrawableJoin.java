package nl.adaptivity.process.diagram;
import static nl.adaptivity.process.diagram.DrawableProcessModel.*;
import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.DiagramPath;
import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.ItemCache;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientJoinNode;
import nl.adaptivity.process.processModel.Join;



public class DrawableJoin extends ClientJoinNode<DrawableProcessNode> implements DrawableProcessNode {

  private static final double STROKEEXTEND = Math.sqrt(2)*STROKEWIDTH;

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
    double dx = JOINWIDTH/2;
    double dy = JOINHEIGHT/2;
    return new Rectangle(getX()-dx, getY()-dy, JOINHEIGHT+STROKEEXTEND, JOINWIDTH+STROKEEXTEND);
  }

  @Override
  public <S extends DrawingStrategy<S>> void draw(Canvas<S> pCanvas, Rectangle pClipBounds) {
    final S strategy = pCanvas.getStrategy();
    DiagramPath<S> path = aItems.getPath(strategy, 0);
    if (path==null) {
      final double dx = JOINWIDTH/2;
      final double dy = JOINHEIGHT/2;
      path = pCanvas.newPath();
      path.moveTo(0,dy)
          .lineTo(dx, 0)
          .lineTo(JOINWIDTH, dy)
          .lineTo(dx, JOINHEIGHT)
          .close();
      aItems.setPath(strategy, 0, path);
    }
    if (hasPos()) {
      Pen<S> fgPen = getFGPen(strategy );
      Pen<S> white = aItems.getPen(strategy, 1);
      if (white ==null) { white = pCanvas.newColor(0xff,0xff,0xff,0xff); }
      pCanvas.drawFilledPath(path, white);
      pCanvas.drawPath(path, fgPen);
    }
  }

  public static DrawableJoin from(Join<?> pElem) {
    DrawableJoin result = new DrawableJoin();
    copyProcessNodeAttrs(pElem, result);
    result.setMin(pElem.getMin());
    result.setMax(pElem.getMax());
    return result;
  }

}
