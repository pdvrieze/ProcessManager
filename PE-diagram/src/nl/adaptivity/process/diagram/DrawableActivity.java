package nl.adaptivity.process.diagram;
import nl.adaptivity.diagram.*;
import nl.adaptivity.diagram.Canvas.TextPos;
import nl.adaptivity.process.clientProcessModel.ClientActivityNode;
import nl.adaptivity.process.processModel.Activity;

import static nl.adaptivity.process.diagram.DrawableProcessModel.*;



public class DrawableActivity extends ClientActivityNode<DrawableProcessNode> implements DrawableProcessNode {

  private static final double REFERENCE_OFFSET_X = (ACTIVITYWIDTH+STROKEWIDTH)/2;
  private static final double REFERENCE_OFFSET_Y = (ACTIVITYHEIGHT+STROKEWIDTH)/2;
  public static final String IDBASE = "ac";
  private int mState = STATE_DEFAULT;
  private static Rectangle _bounds;

  public DrawableActivity(final boolean compat) {
    super(compat);
  }

  public DrawableActivity(String id, final boolean compat) {
    super(id, compat);
  }

  public DrawableActivity(DrawableActivity drawableActivity, final boolean compat) {
    super(drawableActivity, compat);
    mState = drawableActivity.mState;
  }

  @Override
  public DrawableActivity clone() {
    if (getClass()==DrawableActivity.class) {
      return new DrawableActivity(this, isCompat());
    }
    throw new RuntimeException(new CloneNotSupportedException());
  }

  @Override
  public Rectangle getBounds() {

    return new Rectangle(getX()-REFERENCE_OFFSET_X, getY()-REFERENCE_OFFSET_Y, ACTIVITYWIDTH + STROKEWIDTH, ACTIVITYHEIGHT + STROKEWIDTH);
  }

  @Override
  public void move(double x, double y) {
    setX(getX()+x);
    setY(getY()+y);
  }

  @Override
  public void setPos(double left, double top) {
    setX(left+REFERENCE_OFFSET_X);
    setY(left+REFERENCE_OFFSET_Y);
  }

  @Override
  public Drawable getItemAt(double x, double y) {
    double hwidth = (ACTIVITYWIDTH+STROKEWIDTH)/2;
    double hheight = (ACTIVITYHEIGHT+STROKEWIDTH)/2;
    return ((Math.abs(x-getX())<=hwidth) && (Math.abs(y-getY())<=hheight)) ? this : null;
  }

  @Override
  public int getState() {
    return mState;
  }

  @Override
  public void setState(int state) {
    if (state==mState) { return ; }
    mState = state;
    if (getOwner()!=null) {
      getOwner().nodeChanged(this);
    }
  }

  @Override
  public String getIdBase() {
    return IDBASE;
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void draw(Canvas<S, PEN_T, PATH_T> canvas, Rectangle clipBounds) {
    if (hasPos()) {
      PEN_T linePen = canvas.getTheme().getPen(ProcessThemeItems.LINE, mState & ~STATE_TOUCHED);
      PEN_T bgPen = canvas.getTheme().getPen(ProcessThemeItems.BACKGROUND, mState);

      if (_bounds==null) { _bounds = new Rectangle(STROKEWIDTH/2,STROKEWIDTH/2, ACTIVITYWIDTH, ACTIVITYHEIGHT); }

      if ((mState&STATE_TOUCHED)!=0) {
        PEN_T touchedPen = canvas.getTheme().getPen(ProcessThemeItems.LINE, STATE_TOUCHED);
        canvas.drawRoundRect(_bounds, ACTIVITYROUNDX, ACTIVITYROUNDY, touchedPen);
      }
      canvas.drawFilledRoundRect(_bounds, ACTIVITYROUNDX, ACTIVITYROUNDY, bgPen);
      canvas.drawRoundRect(_bounds, ACTIVITYROUNDX, ACTIVITYROUNDY, linePen);

    }
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void drawLabel(Canvas<S, PEN_T, PATH_T> canvas, Rectangle clipBounds, double left, double top) {
    PEN_T textPen = canvas.getTheme().getPen(ProcessThemeItems.DIAGRAMLABEL, mState);
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
      canvas.drawText(TextPos.ASCENT, REFERENCE_OFFSET_X, topCenter, label, Double.MAX_VALUE, textPen);
    }
  }

  public static DrawableActivity from(Activity<?> elem, final boolean compat) {
    DrawableActivity result = new DrawableActivity(compat);
    copyProcessNodeAttrs(elem, result);
    result.setName(elem.getName());
    result.setLabel(elem.getLabel());
    result.setCondition(elem.getCondition());
    result.setDefines(elem.getDefines());
    result.setResults(elem.getResults());
    result.setMessage(elem.getMessage());
    return result;
  }



}
