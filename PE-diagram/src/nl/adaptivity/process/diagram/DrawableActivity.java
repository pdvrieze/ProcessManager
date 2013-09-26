package nl.adaptivity.process.diagram;
import static nl.adaptivity.process.diagram.DrawableProcessModel.*;
import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.Color;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientActivityNode;
import nl.adaptivity.process.processModel.Activity;



public class DrawableActivity extends ClientActivityNode<DrawableProcessNode> implements DrawableProcessNode {

  private Color aBlack;
  private Color aWhite;
  private static Rectangle _bounds;

  @Override
  public Rectangle getBounds() {
    return new Rectangle(getX()-(ACTIVITYWIDTH/2), getY()-(ACTIVITYHEIGHT/2), ACTIVITYWIDTH, ACTIVITYHEIGHT);
  }

  @Override
  public void draw(Canvas pCanvas, Rectangle pClipBounds) {
    if (aBlack ==null) { aBlack = pCanvas.newColor(0,0,0,0xff); }
    if (aWhite ==null) { aWhite = pCanvas.newColor(0xff,0xff,0xff,0xff); }
    if (_bounds==null) { _bounds = new Rectangle(0,0, ACTIVITYWIDTH, ACTIVITYHEIGHT); }
    pCanvas.drawFilledRoundRect(_bounds, ACTIVITYROUNDX, ACTIVITYROUNDY, aWhite);
    pCanvas.drawRoundRect(_bounds, ACTIVITYROUNDX, ACTIVITYROUNDY, aBlack);
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
