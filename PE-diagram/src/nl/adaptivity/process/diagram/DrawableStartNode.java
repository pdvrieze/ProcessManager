package nl.adaptivity.process.diagram;
import static nl.adaptivity.process.diagram.DrawableProcessModel.STARTNODERADIUS;
import static nl.adaptivity.process.diagram.DrawableProcessModel.STROKEWIDTH;
import static nl.adaptivity.process.diagram.DrawableProcessModel.copyProcessNodeAttrs;
import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.DiagramPath;
import nl.adaptivity.diagram.Drawable;
import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientStartNode;
import nl.adaptivity.process.processModel.StartNode;



public class DrawableStartNode extends ClientStartNode<DrawableProcessNode> implements DrawableProcessNode{

  private static final double REFERENCE_OFFSET_X = STARTNODERADIUS+(STROKEWIDTH/2);
  private static final double REFERENCE_OFFSET_Y = STARTNODERADIUS+(STROKEWIDTH/2);
  private int mState = STATE_DEFAULT;


  public DrawableStartNode() {
    super();
  }

  public DrawableStartNode(String id) {
    super(id);
  }

  public DrawableStartNode(DrawableStartNode orig) {
    super(orig);
    mState = orig.mState;
  }

  @Override
  public DrawableStartNode clone() {
    if (getClass()==DrawableStartNode.class) {
      return new DrawableStartNode(this);
    }
    throw new RuntimeException(new CloneNotSupportedException());
  }

  @Override
  public Rectangle getBounds() {
    return new Rectangle(getX()-REFERENCE_OFFSET_X, getY()-REFERENCE_OFFSET_Y, STARTNODERADIUS*2+STROKEWIDTH, STARTNODERADIUS*2+STROKEWIDTH);
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
    final double realradius=STARTNODERADIUS+(STROKEWIDTH/2);
    return ((Math.abs(x-getX())<=realradius) && (Math.abs(y-getY())<=realradius)) ? this : null;
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
    if (hasPos()) {
      final double realradius=STARTNODERADIUS+(STROKEWIDTH/2);
      PEN_T fillPen = canvas.getTheme().getPen(ProcessThemeItems.LINEBG, mState & ~STATE_TOUCHED);

      if ((mState&STATE_TOUCHED)!=0) {
        PEN_T touchedPen = canvas.getTheme().getPen(ProcessThemeItems.LINE, STATE_TOUCHED);
        canvas.drawCircle(realradius, realradius, STARTNODERADIUS, touchedPen);
      }

      canvas.drawFilledCircle(realradius, realradius, realradius, fillPen);
    }
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void drawLabel(Canvas<S, PEN_T, PATH_T> canvas, Rectangle clipBounds, double left, double top) {
    DrawableUtil.defaultDrawLabel(this, canvas, clipBounds, left, top);
  }

  public static DrawableStartNode from(StartNode<?> n) {
    DrawableStartNode result = new DrawableStartNode();
    copyProcessNodeAttrs(n, result);
    result.setDefines(n.getDefines());
    return result;
  }

}
