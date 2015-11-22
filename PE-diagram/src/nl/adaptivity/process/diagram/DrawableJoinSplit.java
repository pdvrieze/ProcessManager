package nl.adaptivity.process.diagram;


import nl.adaptivity.diagram.*;
import nl.adaptivity.diagram.Canvas.TextPos;
import nl.adaptivity.process.clientProcessModel.ClientJoinSplit;

import static nl.adaptivity.process.diagram.DrawableProcessModel.*;


public abstract class DrawableJoinSplit extends ClientJoinSplit<DrawableProcessNode> implements DrawableProcessNode {

  protected static final boolean CURVED_ARROWS=true;
  private static final boolean TEXT_DESC=true;

  protected static final double STROKEEXTEND = Math.sqrt(2)*STROKEWIDTH;
  private static final double REFERENCE_OFFSET_X = (JOINWIDTH+STROKEEXTEND)/2;
  private static final double REFERENCE_OFFSET_Y = (JOINHEIGHT+STROKEEXTEND)/2;
  protected static final double HORIZONTALDECORATIONLEN = JOINWIDTH*0.4;
  protected static final double CENTERX = (JOINWIDTH+STROKEEXTEND)/2;
  protected static final double CENTERY = (JOINHEIGHT+STROKEEXTEND)/2;
  protected static final double ARROWHEADANGLE = (35*Math.PI)/180;
  protected static final double ARROWLEN = JOINWIDTH*0.15;
  protected static final double ARROWCONTROLRATIO=0.85;
  protected final ItemCache mItems = new ItemCache();
  private int mState = STATE_DEFAULT;

  public DrawableJoinSplit(final boolean compat) {
    super(compat);
  }

  public DrawableJoinSplit(String id, final boolean compat) {
    super(id, compat);
  }

  public DrawableJoinSplit(DrawableJoinSplit orig, final boolean compat) {
    super(orig, compat);
    mState = orig.mState;
  }

  @Override
  public abstract DrawableJoinSplit clone();

  @Override
  public Rectangle getBounds() {
    return new Rectangle(getX()-REFERENCE_OFFSET_X, getY()-REFERENCE_OFFSET_Y, JOINHEIGHT+STROKEEXTEND, JOINWIDTH+STROKEEXTEND);
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
    final double realradiusX=(JOINWIDTH+STROKEEXTEND)/2;
    final double realradiusY=(JOINHEIGHT+STROKEEXTEND)/2;
    return ((Math.abs(x-getX())<=realradiusX) && (Math.abs(y-getY())<=realradiusY)) ? this : null;
  }

  @Override
  public int getState() {
    return mState ;
  }

  @Override
  public void setState(int state) {
    mState = state;
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void draw(Canvas<S, PEN_T, PATH_T> canvas, Rectangle clipBounds) {
    final S strategy = canvas.getStrategy();
    PATH_T path = mItems.getPath(strategy, 0);
    final double dx = JOINWIDTH/2;
    final double hse = STROKEEXTEND/2;
    if (path==null) {
      final double dy = JOINHEIGHT/2;
      path = strategy.newPath();
      path.moveTo(hse,dy+hse)
          .lineTo(dx+hse, hse)
          .lineTo(JOINWIDTH+hse, dy+hse)
          .lineTo(dx+hse, JOINHEIGHT+hse)
          .close();
      mItems.setPath(strategy, 0, path);
    }
    if (hasPos()) {
      PEN_T linePen = canvas.getTheme().getPen(ProcessThemeItems.LINE, mState & ~STATE_TOUCHED);
      PEN_T bgPen = canvas.getTheme().getPen(ProcessThemeItems.BACKGROUND, mState);

      if ((mState&STATE_TOUCHED)!=0) {
        PEN_T touchedPen = canvas.getTheme().getPen(ProcessThemeItems.LINE, STATE_TOUCHED);
        canvas.drawPath(path, touchedPen, null);
      }
      canvas.drawPath(path, linePen, bgPen);

      if (getOwner()!=null || getMin()>=0 || getMax()>=0) {
        PEN_T textPen = canvas.getTheme().getPen(ProcessThemeItems.DIAGRAMTEXT, mState);
        String s = getMinMaxText();

        canvas.drawText(TextPos.DESCENT, hse+dx, -hse, s, Double.MAX_VALUE, textPen);
      }
    }
  }

  public String getMinMaxText() {
    if (TEXT_DESC) {
      if (getMin()==1 && getMax()==1) {
        return "xor";
      } else if (getMin()==1 && getMax()>=getSuccessors().size()) {
        return "or";
      } else if (getMin()==getMax() && getMax()>=getSuccessors().size()) {
        return "and";
      }
    }
    StringBuilder str = new StringBuilder();
    if (getMin()<0) {
      str.append("?...");
    } else if (getMin()==getMax()){
      return Integer.toString(getMin());
    } else {
      str.append(getMin()).append("...");
    }
    if (getMax()<0) {
      str.append("?");
    } else {
      str.append(getMax());
    }
    return str.toString();
  }

}