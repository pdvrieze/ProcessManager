package nl.adaptivity.process.diagram;
import static nl.adaptivity.process.diagram.DrawableProcessModel.JOINHEIGHT;
import static nl.adaptivity.process.diagram.DrawableProcessModel.JOINWIDTH;
import static nl.adaptivity.process.diagram.DrawableProcessModel.STROKEWIDTH;
import static nl.adaptivity.process.diagram.DrawableProcessModel.copyProcessNodeAttrs;
import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.DiagramPath;
import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.SerializerAdapter;
import nl.adaptivity.process.processModel.Join;
import nl.adaptivity.process.processModel.ProcessNode;



public class DrawableJoin extends DrawableJoinSplit implements Join<DrawableProcessNode> {

  private static final double ARROWHEADDX = JOINWIDTH*0.375;
  private static final double ARROWHEADADJUST = 0.5*STROKEWIDTH/Math.sin(ARROWHEADANGLE);

  /** The y coordinate if the line were horizontal. */
  private static final double ARROWDFAR = ARROWLEN*Math.sin(ARROWHEADANGLE);
  /** The x coordinate if the line were horizontal. */
  private static final double ARROWDNEAR = ARROWLEN*Math.cos(ARROWHEADANGLE);
  private static final double INDX = JOINWIDTH*0.2;
  private static final double INDY = JOINHEIGHT*0.2;
  private static final double INLEN = Math.sqrt(INDX*INDX+INDY*INDY);

  public DrawableJoin() {
    super();
  }

  public DrawableJoin(String id) {
    super(id);
  }

  public DrawableJoin(DrawableJoin orig) {
    super(orig);
  }

  @Override
  public DrawableJoin clone() {
    if (getClass()==DrawableJoin.class) {
      return new DrawableJoin(this);
    }
    throw new RuntimeException(new CloneNotSupportedException());
  }

  @Override
  public int getMaxPredecessorCount() {
    return Integer.MAX_VALUE;
  }

  public static DrawableJoin from(Join<?> elem) {
    DrawableJoin result = new DrawableJoin();
    copyProcessNodeAttrs(elem, result);
    result.setMin(elem.getMin());
    result.setMax(elem.getMax());
    return result;
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void draw(Canvas<S, PEN_T, PATH_T> canvas, Rectangle clipBounds) {
    super.draw(canvas, clipBounds);
    final S strategy = canvas.getStrategy();
    PATH_T path = mItems.getPath(strategy, 1);
    if (path==null) {
      path = strategy.newPath();
      if (CURVED_ARROWS) {
        path.moveTo(CENTERX+INLEN,CENTERY)
            .lineTo(CENTERX + ARROWHEADDX - ARROWHEADADJUST, CENTERX)
            .moveTo(CENTERX+ARROWHEADDX-ARROWDNEAR, CENTERY-ARROWDFAR)
            .lineTo(CENTERX+ARROWHEADDX, CENTERY)
            .lineTo(CENTERX+ARROWHEADDX-ARROWDNEAR, CENTERY+ARROWDFAR)
            .moveTo(CENTERX-INDX,CENTERY-INDY)
            .cubicTo(CENTERX-(INDX*(1-ARROWCONTROLRATIO)), CENTERY-(INDY*(1-ARROWCONTROLRATIO)), CENTERX+INLEN*(1-ARROWCONTROLRATIO), CENTERY, CENTERX+INLEN, CENTERY)
            .cubicTo(CENTERX+INLEN*(1-ARROWCONTROLRATIO), CENTERY, CENTERX-(INDX*(1-ARROWCONTROLRATIO)), CENTERY+(INDY*(1-ARROWCONTROLRATIO)), CENTERX-INDX,CENTERY+INDY);
      } else {
        path.moveTo(CENTERX,CENTERY)
            .lineTo(CENTERX + ARROWHEADDX - ARROWHEADADJUST, CENTERX)
            .moveTo(CENTERX+ARROWHEADDX-ARROWDNEAR, CENTERY-ARROWDFAR)
            .lineTo(CENTERX+ARROWHEADDX, CENTERY)
            .lineTo(CENTERX+ARROWHEADDX-ARROWDNEAR, CENTERY+ARROWDFAR)
            .moveTo(CENTERX-INDX,CENTERY-INDY)
            .lineTo(CENTERX, CENTERY)
            .lineTo(CENTERX-INDX,CENTERY+INDY);
      }
      mItems.setPath(strategy, 1, path);
    }
    if (hasPos()) {
      PEN_T linePen = canvas.getTheme().getPen(ProcessThemeItems.INNERLINE, getState());
      canvas.drawPath(path, linePen, null);
    }
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void drawLabel(Canvas<S, PEN_T, PATH_T> canvas, Rectangle clipBounds, double left, double top) {
    DrawableUtil.defaultDrawLabel(this, canvas, clipBounds, left, top);
  }

  @Override
  public void serialize(SerializerAdapter out) {
    serializeJoin(out);
  }

  @Override
  public <R> R visit(ProcessNode.Visitor<R> visitor) {
    return visitor.visitJoin(this);
  }

}
