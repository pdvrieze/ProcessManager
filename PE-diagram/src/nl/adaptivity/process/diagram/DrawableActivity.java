package nl.adaptivity.process.diagram;
import static nl.adaptivity.process.diagram.DrawableProcessModel.*;
import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientActivityNode;
import nl.adaptivity.process.processModel.Activity;



public class DrawableActivity extends ClientActivityNode<DrawableProcessNode> implements DrawableProcessNode {

  private Pen aFGPen;
  private Pen aWhite;
  private static Rectangle _bounds;

  @Override
  public Rectangle getBounds() {
    return new Rectangle(getX()-(ACTIVITYWIDTH/2), getY()-(ACTIVITYHEIGHT/2), ACTIVITYWIDTH + STROKEWIDTH, ACTIVITYHEIGHT + STROKEWIDTH);
  }

  @Override
  public Pen getPen() {
    return aFGPen;
  }

  @Override
  public void setFGPen(Pen pPen) {
    aFGPen = pPen==null ? null : pPen.setStrokeWidth(STROKEWIDTH);
  }

  @Override
  public void draw(Canvas pCanvas, Rectangle pClipBounds) {
    if (hasPos()) {
      if (aFGPen ==null) { aFGPen = pCanvas.newColor(0,0,0,0xff); aFGPen.setStrokeWidth(STROKEWIDTH); }
      if (aWhite ==null) { aWhite = pCanvas.newColor(0xff,0xff,0xff,0xff); }
      if (_bounds==null) { _bounds = new Rectangle(0,0, ACTIVITYWIDTH, ACTIVITYHEIGHT); }
      pCanvas.drawFilledRoundRect(_bounds, ACTIVITYROUNDX, ACTIVITYROUNDY, aWhite);
      pCanvas.drawRoundRect(_bounds, ACTIVITYROUNDX, ACTIVITYROUNDY, aFGPen);
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
