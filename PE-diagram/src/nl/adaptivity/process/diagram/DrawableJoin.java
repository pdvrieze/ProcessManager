package nl.adaptivity.process.diagram;
import static nl.adaptivity.process.diagram.DrawableProcessModel.*;
import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.DiagramPath;
import nl.adaptivity.diagram.Drawable;
import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.diagram.ItemCache;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientJoinNode;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.processModel.Join;



public class DrawableJoin extends ClientJoinNode<DrawableProcessNode> implements DrawableProcessNode {

  private static final double STROKEEXTEND = Math.sqrt(2)*STROKEWIDTH;

  private static final double REFERENCE_OFFSET_X = (JOINWIDTH+STROKEEXTEND)/2;

  private static final double REFERENCE_OFFSET_Y = (JOINHEIGHT+STROKEEXTEND)/2;

  private ItemCache aItems = new ItemCache();

  private int aState = STATE_DEFAULT;

  public DrawableJoin(ClientProcessModel<DrawableProcessNode> pOwner) {
    super(pOwner);
  }

  public DrawableJoin(String pId, ClientProcessModel<DrawableProcessNode> pOwner) {
    super(pId, pOwner);
  }

  public DrawableJoin(DrawableJoin pOrig) {
    super(pOrig);
    aState = pOrig.aState;
  }

  @Override
  public DrawableJoin clone() {
    if (getClass()==DrawableJoin.class) {
      return new DrawableJoin(this);
    }
    throw new RuntimeException(new CloneNotSupportedException());
  }

  @Override
  public Rectangle getBounds() {
    return new Rectangle(getX()-REFERENCE_OFFSET_X, getY()-REFERENCE_OFFSET_Y, JOINHEIGHT+STROKEEXTEND, JOINWIDTH+STROKEEXTEND);
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
    final double realradiusX=(JOINWIDTH+STROKEEXTEND)/2;
    final double realradiusY=(JOINHEIGHT+STROKEEXTEND)/2;
    return ((Math.abs(pX-getX())<=realradiusX) && (Math.abs(pY-getY())<=realradiusY)) ? this : null;
  }

  @Override
  public int getState() {
    return aState ;
  }

  @Override
  public void setState(int pState) {
    aState = pState;
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void draw(Canvas<S, PEN_T, PATH_T> pCanvas, Rectangle pClipBounds) {
    final S strategy = pCanvas.getStrategy();
    PATH_T path = aItems.getPath(strategy, 0);
    if (path==null) {
      final double dx = JOINWIDTH/2;
      final double dy = JOINHEIGHT/2;
      path = strategy.newPath();
      final double hse = STROKEEXTEND/2;
      path.moveTo(hse,dy+hse)
          .lineTo(dx+hse, hse)
          .lineTo(JOINWIDTH+hse, dy+hse)
          .lineTo(dx+hse, JOINHEIGHT+hse)
          .close();
      aItems.setPath(strategy, 0, path);
    }
    if (hasPos()) {
      PEN_T linePen = pCanvas.getTheme().getPen(ProcessThemeItems.LINE, aState & ~STATE_TOUCHED);
      PEN_T bgPen = pCanvas.getTheme().getPen(ProcessThemeItems.BACKGROUND, aState);

      if ((aState&STATE_TOUCHED)!=0) {
        PEN_T touchedPen = pCanvas.getTheme().getPen(ProcessThemeItems.LINE, STATE_TOUCHED);
        pCanvas.drawPath(path, touchedPen, null);
      }
      pCanvas.drawPath(path, linePen, bgPen);
    }
  }

  public static DrawableJoin from(DrawableProcessModel pOwner, Join<?> pElem) {
    DrawableJoin result = new DrawableJoin(pOwner);
    copyProcessNodeAttrs(pElem, result);
    result.setMin(pElem.getMin());
    result.setMax(pElem.getMax());
    return result;
  }

}
