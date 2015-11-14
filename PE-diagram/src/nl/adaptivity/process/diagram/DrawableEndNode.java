package nl.adaptivity.process.diagram;
import nl.adaptivity.diagram.*;
import nl.adaptivity.process.clientProcessModel.ClientEndNode;
import nl.adaptivity.process.processModel.EndNode;

import static nl.adaptivity.process.diagram.DrawableProcessModel.*;



public class DrawableEndNode extends ClientEndNode<DrawableProcessNode> implements DrawableProcessNode {

  private static final double REFERENCE_OFFSET_X = ENDNODEOUTERRADIUS;
  private static final double REFERENCE_OFFSET_Y = ENDNODEOUTERRADIUS;
  public static final String IDBASE = "end";
  private int mState = STATE_DEFAULT;


  public DrawableEndNode() {
    super();
  }

  public DrawableEndNode(String id) {
    super(id);
  }

  public DrawableEndNode(DrawableEndNode orig) {
    super(orig);
    mState = orig.mState;
  }

  @Override
  public DrawableEndNode clone() {
    if (getClass()==DrawableEndNode.class) {
      return new DrawableEndNode(this);
    }
    throw new RuntimeException(new CloneNotSupportedException());
  }

  @Override
  public Rectangle getBounds() {
    return new Rectangle(getX()-ENDNODEOUTERRADIUS, getY()-ENDNODEOUTERRADIUS, ENDNODEOUTERRADIUS*2 + ENDNODEOUTERSTROKEWIDTH, ENDNODEOUTERRADIUS*2 + ENDNODEOUTERSTROKEWIDTH);
  }

  @Override
  public void setPos(double left, double top) {
    setX(left+REFERENCE_OFFSET_X);
    setY(left+REFERENCE_OFFSET_Y);
  }

  @Override
  public void move(double x, double y) {
    setX(getX()+x);
    setY(getY()+y);
  }

  @Override
  public Drawable getItemAt(double x, double y) {
    final double realradius=ENDNODEOUTERRADIUS+(ENDNODEOUTERSTROKEWIDTH/2);
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
  public String getIdBase() {
    return IDBASE;
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void draw(Canvas<S, PEN_T, PATH_T> canvas, Rectangle clipBounds) {
    if (hasPos()) {
      PEN_T outerLinePen = canvas.getTheme().getPen(ProcessThemeItems.ENDNODEOUTERLINE, mState & ~STATE_TOUCHED);
      PEN_T innerPen = canvas.getTheme().getPen(ProcessThemeItems.LINEBG, mState & ~STATE_TOUCHED);

      double hsw = ENDNODEOUTERSTROKEWIDTH/2;

      if ((mState&STATE_TOUCHED)!=0) {
        PEN_T touchedPen = canvas.getTheme().getPen(ProcessThemeItems.LINE, STATE_TOUCHED);
        canvas.drawCircle(ENDNODEOUTERRADIUS+hsw, ENDNODEOUTERRADIUS+hsw, ENDNODEOUTERRADIUS, touchedPen);
        canvas.drawCircle(ENDNODEOUTERRADIUS+hsw, ENDNODEOUTERRADIUS+hsw, ENDNODEINNERRRADIUS, touchedPen);
      }
      canvas.drawCircle(ENDNODEOUTERRADIUS+hsw, ENDNODEOUTERRADIUS+hsw, ENDNODEOUTERRADIUS, outerLinePen);
      canvas.drawFilledCircle(ENDNODEOUTERRADIUS+hsw, ENDNODEOUTERRADIUS+hsw, ENDNODEINNERRRADIUS, innerPen);
    }
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void drawLabel(Canvas<S, PEN_T, PATH_T> canvas, Rectangle clipBounds, double left, double top) {
    DrawableUtil.defaultDrawLabel(this, canvas, clipBounds, left, top);
  }

  public static  DrawableEndNode from(EndNode<?> elem) {
    DrawableEndNode result = new DrawableEndNode();
    copyProcessNodeAttrs(elem, result);
    return result;
  }

}
