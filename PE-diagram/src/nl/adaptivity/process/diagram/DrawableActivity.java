package nl.adaptivity.process.diagram;
import static nl.adaptivity.process.diagram.DrawableProcessModel.*;
import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.Color;
import nl.adaptivity.diagram.Drawable;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.processModel.ActivityImpl;



public class DrawableActivity extends ActivityImpl implements Drawable {

  private static final long serialVersionUID = 2566853039695562412L;

  private Color aBlack;
  private Color aWhite;

  @Override
  public Rectangle getBounds() {
    return new Rectangle(getX()-(ACTIVITYWIDTH/2), getY()-(ACTIVITYHEIGHT/2), ACTIVITYHEIGHT, ACTIVITYWIDTH);
  }

  @Override
  public void draw(Canvas pCanvas, Rectangle pClipBounds) {
    if (aBlack ==null) { aBlack = pCanvas.newColor(0,0,0,0xff); }
    if (aWhite ==null) { aWhite = pCanvas.newColor(0xff,0xff,0xff,0xff); }
    pCanvas.drawFilledRoundRect(getBounds(), ACTIVITYROUNDX, ACTIVITYROUNDY, aWhite);
    pCanvas.drawRoundRect(getBounds(), ACTIVITYROUNDX, ACTIVITYROUNDY, aBlack);
  }

  public static DrawableActivity from(ActivityImpl pElem) {
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
