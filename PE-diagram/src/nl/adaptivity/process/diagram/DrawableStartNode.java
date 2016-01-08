package nl.adaptivity.process.diagram;
import nl.adaptivity.diagram.*;
import nl.adaptivity.process.clientProcessModel.ClientStartNode;
import nl.adaptivity.process.processModel.StartNode;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import org.jetbrains.annotations.NotNull;

import static nl.adaptivity.process.diagram.DrawableProcessModel.*;



public class DrawableStartNode extends ClientStartNode<DrawableProcessNode, DrawableProcessModel> implements DrawableProcessNode{

  private static final double REFERENCE_OFFSET_X = STARTNODERADIUS+(STROKEWIDTH/2);
  private static final double REFERENCE_OFFSET_Y = STARTNODERADIUS+(STROKEWIDTH/2);
  public static final String IDBASE = "start";
  private int mState = STATE_DEFAULT;

  public DrawableStartNode(final DrawableProcessModel ownerModel) {
    this(ownerModel, false);
  }

  public DrawableStartNode(final DrawableProcessModel ownerModel, final boolean compat) {
    super(ownerModel, compat);
  }

  public DrawableStartNode(final DrawableProcessModel ownerModel, String id, final boolean compat) {
    super(ownerModel, id, compat);
  }

  public DrawableStartNode(DrawableStartNode orig, final boolean compat) {
    super(orig, compat);
    mState = orig.mState;
  }

  @Override
  public DrawableStartNode clone() {
    if (getClass()==DrawableStartNode.class) {
      return new DrawableStartNode(this, isCompat());
    }
    throw new RuntimeException(new CloneNotSupportedException());
  }

  @NotNull
  public static DrawableStartNode deserialize(final DrawableProcessModel ownerModel, @NotNull final XmlReader in) throws XmlException {
    return XmlUtil.deserializeHelper(new DrawableStartNode(ownerModel, true), in);
  }

  @Override
  public Rectangle getBounds() {
    return new Rectangle(getX()-REFERENCE_OFFSET_X, getY()-REFERENCE_OFFSET_Y, STARTNODERADIUS*2+STROKEWIDTH, STARTNODERADIUS*2+STROKEWIDTH);
  }

  @Override
  public void translate(double dX, double dY) {
    setX(getX() + dX);
    setY(getY() + dY);
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
  public String getIdBase() {
    return IDBASE;
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void drawLabel(Canvas<S, PEN_T, PATH_T> canvas, Rectangle clipBounds, double left, double top) {
    DrawableUtil.defaultDrawLabel(this, canvas, clipBounds, left, top);
  }

  public static DrawableStartNode from(StartNode<?, ?> n, final boolean compat) {
    DrawableStartNode result = new DrawableStartNode((DrawableProcessModel) null, compat);
    copyProcessNodeAttrs(n, result);
    result.setDefines(n.getDefines());
    return result;
  }

}
