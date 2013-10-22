package nl.adaptivity.process.diagram;
import static nl.adaptivity.process.diagram.DrawableProcessModel.*;
import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.DiagramPath;
import nl.adaptivity.diagram.Drawable;
import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.diagram.ItemCache;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientActivityNode;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.processModel.Activity;



public class DrawableActivity extends ClientActivityNode<DrawableProcessNode> implements DrawableProcessNode {

  private ItemCache aItems = new ItemCache();
  private static Rectangle _bounds;

  public DrawableActivity(ClientProcessModel<DrawableProcessNode> pOwner) {
    super(pOwner);
  }

  public DrawableActivity(String pId, ClientProcessModel<DrawableProcessNode> pOwner) {
    super(pId, pOwner);
  }

  @Override
  public Rectangle getBounds() {
    return new Rectangle(getX()-(ACTIVITYWIDTH/2), getY()-(ACTIVITYHEIGHT/2), ACTIVITYWIDTH + STROKEWIDTH, ACTIVITYHEIGHT + STROKEWIDTH);
  }

  @Override
  public Drawable getItemAt(double pX, double pY) {
    double hwidth = (ACTIVITYWIDTH+STROKEWIDTH)/2;
    double hheight = (ACTIVITYHEIGHT+STROKEWIDTH)/2; 
    return ((Math.abs(pX-getX())<=hwidth) && (Math.abs(pY-getY())<=hheight)) ? this : null;
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> PEN_T getFGPen(S pStrategy) {
    PEN_T result = aItems.getPen(pStrategy, 0);
    if (result==null) {
      result = pStrategy.newPen()
                        .setColor(0,0,0,0xff)
                        .setStrokeWidth(STROKEWIDTH);
      aItems.setPen(pStrategy, 0, result);
    }
    return result;
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void setFGPen(S pStrategy, PEN_T pPen) {
    aItems.setPen(pStrategy, 0, pPen==null ? null : pPen.setStrokeWidth(STROKEWIDTH));
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void draw(Canvas<S, PEN_T, PATH_T> pCanvas, Rectangle pClipBounds) {
    if (hasPos()) {
      final S strategy = pCanvas.getStrategy();
      PEN_T fgPen = getFGPen(strategy);
      PEN_T white = aItems.getPen(strategy, 1);
      if (white ==null) { aItems.setPen(strategy, 1, white = strategy.newPen().setColor(0xff,0xff,0xff,0xff)); }

      if (_bounds==null) { _bounds = new Rectangle(0,0, ACTIVITYWIDTH, ACTIVITYHEIGHT); }
      pCanvas.drawFilledRoundRect(_bounds, ACTIVITYROUNDX, ACTIVITYROUNDY, white);
      pCanvas.drawRoundRect(_bounds, ACTIVITYROUNDX, ACTIVITYROUNDY, fgPen);
    }
  }

  public static DrawableActivity from(DrawableProcessModel pOwner, Activity<?> pElem) {
    DrawableActivity result = new DrawableActivity(pOwner);
    copyProcessNodeAttrs(pElem, result);
    result.setName(pElem.getName());
    result.setCondition(pElem.getCondition());
    result.setImports(pElem.getImports());
    result.setExports(pElem.getExports());
    result.setMessage(pElem.getMessage());
    return result;
  }



}
