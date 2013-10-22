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

  private ItemCache aItems = new ItemCache();

  private int aState = STATE_DEFAULT;

  public DrawableJoin(ClientProcessModel<DrawableProcessNode> pOwner) {
    super(pOwner);
  }

  public DrawableJoin(String pId, ClientProcessModel<DrawableProcessNode> pOwner) {
    super(pId, pOwner);
  }

  @Override
  public Rectangle getBounds() {
    double dx = (JOINWIDTH+STROKEEXTEND)/2;
    double dy = (JOINHEIGHT+STROKEEXTEND)/2;
    return new Rectangle(getX()-dx, getY()-dy, JOINHEIGHT+STROKEEXTEND, JOINWIDTH+STROKEEXTEND);
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
      path.moveTo(0,dy)
          .lineTo(dx, 0)
          .lineTo(JOINWIDTH, dy)
          .lineTo(dx, JOINHEIGHT)
          .close();
      aItems.setPath(strategy, 0, path);
    }
    if (hasPos()) {
      PEN_T linePen = pCanvas.getTheme().getPen(ProcessThemeItems.LINE, aState);
      PEN_T bgPen = pCanvas.getTheme().getPen(ProcessThemeItems.BACKGROUND, aState);

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
