package nl.adaptivity.process.diagram;
import static nl.adaptivity.process.diagram.DrawableProcessModel.*;
import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.PenCache;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientActivityNode;
import nl.adaptivity.process.processModel.Activity;



public class DrawableActivity extends ClientActivityNode<DrawableProcessNode> implements DrawableProcessNode {

  private PenCache aPens = new PenCache();
  private static Rectangle _bounds;

  @Override
  public Rectangle getBounds() {
    return new Rectangle(getX()-(ACTIVITYWIDTH/2), getY()-(ACTIVITYHEIGHT/2), ACTIVITYWIDTH + STROKEWIDTH, ACTIVITYHEIGHT + STROKEWIDTH);
  }

  @Override
  public <S extends DrawingStrategy> Pen<S> getPen(S pStrategy) {
    return aPens.getPen(pStrategy, 0);
  }

  @Override
  public <S extends DrawingStrategy> void setFGPen(S pStrategy, Pen<S> pPen) {
    aPens.setPen(pStrategy, 0, pPen==null ? null : pPen.setStrokeWidth(STROKEWIDTH));
  }

  @Override
  public <S extends DrawingStrategy> void draw(Canvas<S> pCanvas, Rectangle pClipBounds) {
    if (hasPos()) {
      final S strategy = pCanvas.getStrategy();
      Pen<S> fgPen = aPens.getPen(strategy, 0);
      Pen<S> white = aPens.getPen(strategy, 1);
      if (fgPen ==null) { aPens.setPen(strategy, 0,fgPen = pCanvas.newColor(0,0,0,0xff)); }
      if (white ==null) { aPens.setPen(strategy, 1, white = pCanvas.newColor(0xff,0xff,0xff,0xff)); }
      if (_bounds==null) { _bounds = new Rectangle(0,0, ACTIVITYWIDTH, ACTIVITYHEIGHT); }
      pCanvas.drawFilledRoundRect(_bounds, ACTIVITYROUNDX, ACTIVITYROUNDY, white);
      pCanvas.drawRoundRect(_bounds, ACTIVITYROUNDX, ACTIVITYROUNDY, fgPen);
    }
  }

  public static DrawableActivity from(Activity<?> pElem) {
    DrawableActivity result = new DrawableActivity();
    copyProcessNodeAttrs(pElem, result);
    result.setName(pElem.getName());
    result.setCondition(pElem.getCondition());
    result.setImports(pElem.getImports());
    result.setExports(pElem.getExports());
    result.setMessage(pElem.getMessage());
    return result;
  }



}
