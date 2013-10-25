package nl.adaptivity.process.diagram;
import static nl.adaptivity.process.diagram.DrawableProcessModel.*;
import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.DiagramPath;
import nl.adaptivity.diagram.Drawable;
import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientActivityNode;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.processModel.Activity;



public class DrawableActivity extends ClientActivityNode<DrawableProcessNode> implements DrawableProcessNode {

  private int aState = STATE_DEFAULT;
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
  public int getState() {
    return aState;
  }

  @Override
  public void setState(int pState) {
    if (pState==aState) { return ; }
    aState = pState;
    getOwner().nodeChanged(this);
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void draw(Canvas<S, PEN_T, PATH_T> pCanvas, Rectangle pClipBounds) {
    if (hasPos()) {
      PEN_T linePen = pCanvas.getTheme().getPen(ProcessThemeItems.LINE, aState & ~STATE_TOUCHED);
      PEN_T bgPen = pCanvas.getTheme().getPen(ProcessThemeItems.BACKGROUND, aState);

      if (_bounds==null) { _bounds = new Rectangle(0,0, ACTIVITYWIDTH, ACTIVITYHEIGHT); }

      if ((aState&STATE_TOUCHED)!=0) {
        PEN_T touchedPen = pCanvas.getTheme().getPen(ProcessThemeItems.LINE, STATE_TOUCHED);
        pCanvas.drawRoundRect(_bounds, ACTIVITYROUNDX, ACTIVITYROUNDY, touchedPen);
      }
      pCanvas.drawFilledRoundRect(_bounds, ACTIVITYROUNDX, ACTIVITYROUNDY, bgPen);
      pCanvas.drawRoundRect(_bounds, ACTIVITYROUNDX, ACTIVITYROUNDY, linePen);
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
