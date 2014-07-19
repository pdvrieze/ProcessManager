package nl.adaptivity.process.diagram;
import static nl.adaptivity.process.diagram.DrawableProcessModel.ACTIVITYHEIGHT;
import static nl.adaptivity.process.diagram.DrawableProcessModel.ACTIVITYROUNDX;
import static nl.adaptivity.process.diagram.DrawableProcessModel.ACTIVITYROUNDY;
import static nl.adaptivity.process.diagram.DrawableProcessModel.ACTIVITYWIDTH;
import static nl.adaptivity.process.diagram.DrawableProcessModel.STROKEWIDTH;
import static nl.adaptivity.process.diagram.DrawableProcessModel.copyProcessNodeAttrs;
import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.Canvas.TextPos;
import nl.adaptivity.diagram.DiagramPath;
import nl.adaptivity.diagram.Drawable;
import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientActivityNode;
import nl.adaptivity.process.processModel.Activity;



public class DrawableActivity extends ClientActivityNode<DrawableProcessNode> implements DrawableProcessNode {

  private static final double REFERENCE_OFFSET_X = (ACTIVITYWIDTH+STROKEWIDTH)/2;
  private static final double REFERENCE_OFFSET_Y = (ACTIVITYHEIGHT+STROKEWIDTH)/2;
  private int aState = STATE_DEFAULT;
  private static Rectangle _bounds;

  public DrawableActivity() {
    super();
  }

  public DrawableActivity(String pId) {
    super(pId);
  }

  public DrawableActivity(DrawableActivity pDrawableActivity) {
    super(pDrawableActivity);
    aState = pDrawableActivity.aState;
  }

  @Override
  public DrawableActivity clone() {
    if (getClass()==DrawableActivity.class) {
      return new DrawableActivity(this);
    }
    throw new RuntimeException(new CloneNotSupportedException());
  }

  @Override
  public Rectangle getBounds() {
    return new Rectangle(getX()-REFERENCE_OFFSET_X, getY()-REFERENCE_OFFSET_Y, ACTIVITYWIDTH + STROKEWIDTH, ACTIVITYHEIGHT + STROKEWIDTH);
  }

  @Override
  public void move(double pX, double pY) {
    setX(getX()+pX);
    setY(getY()+pY);
  }

  @Override
  public void setPos(double pLeft, double pTop) {
    setX(pLeft+REFERENCE_OFFSET_X);
    setY(pLeft+REFERENCE_OFFSET_Y);
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
    if (getOwner()!=null) {
      getOwner().nodeChanged(this);
    }
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void draw(Canvas<S, PEN_T, PATH_T> pCanvas, Rectangle pClipBounds) {
    if (hasPos()) {
      PEN_T linePen = pCanvas.getTheme().getPen(ProcessThemeItems.LINE, aState & ~STATE_TOUCHED);
      PEN_T bgPen = pCanvas.getTheme().getPen(ProcessThemeItems.BACKGROUND, aState);

      if (_bounds==null) { _bounds = new Rectangle(STROKEWIDTH/2,STROKEWIDTH/2, ACTIVITYWIDTH, ACTIVITYHEIGHT); }

      if ((aState&STATE_TOUCHED)!=0) {
        PEN_T touchedPen = pCanvas.getTheme().getPen(ProcessThemeItems.LINE, STATE_TOUCHED);
        pCanvas.drawRoundRect(_bounds, ACTIVITYROUNDX, ACTIVITYROUNDY, touchedPen);
      }
      pCanvas.drawFilledRoundRect(_bounds, ACTIVITYROUNDX, ACTIVITYROUNDY, bgPen);
      pCanvas.drawRoundRect(_bounds, ACTIVITYROUNDX, ACTIVITYROUNDY, linePen);

      PEN_T textPen = pCanvas.getTheme().getPen(ProcessThemeItems.DIAGRAMLABEL, aState);
      String label = getLabel();
      if (label==null) { label = getName(); }
      if (label==null && getOwner()!=null) {
        label='<'+getId()+'>';
        textPen.setTextItalics(true);
      } else if (label!=null) {
        textPen.setTextItalics(false);
      }
      if (label!=null) {
        double topCenter = ACTIVITYHEIGHT+STROKEWIDTH +textPen.getTextLeading()/2;
        pCanvas.drawText(TextPos.ASCENT, REFERENCE_OFFSET_X, topCenter, label, Double.MAX_VALUE, textPen);
      }
    }
  }

  public static DrawableActivity from(Activity<?> pElem) {
    DrawableActivity result = new DrawableActivity();
    copyProcessNodeAttrs(pElem, result);
    result.setName(pElem.getName());
    result.setLabel(pElem.getLabel());
    result.setCondition(pElem.getCondition());
    result.setImports(pElem.getImports());
    result.setExports(pElem.getExports());
    result.setMessage(pElem.getMessage());
    return result;
  }



}
