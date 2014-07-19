package nl.adaptivity.process.diagram;
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
import nl.adaptivity.process.processModel.Split;



public class DrawableSplit extends DrawableJoinSplit implements Split<DrawableProcessNode>{

  private static final double ARROWHEADDX = JOINWIDTH*0.2;
  private static final double ARROWHEADDY = JOINWIDTH*0.2;
  private static final double ARROWHEADADJUST = (0.5*STROKEWIDTH)*Math.sqrt(0.5/(Math.sin(ARROWHEADANGLE)* Math.sin(ARROWHEADANGLE)));
  /** The y coordinate if the line were horizontal. */
  private static final double ARROWDFAR = ARROWLEN*Math.sin(0.25*Math.PI-ARROWHEADANGLE);
  /** The x coordinate if the line were horizontal. */
  private static final double ARROWDNEAR = ARROWLEN*Math.cos(0.25*Math.PI-ARROWHEADANGLE);
  private static final double INLEN = Math.sqrt(ARROWHEADDX*ARROWHEADDX+ARROWHEADDY*ARROWHEADDY);


  public DrawableSplit() {
    super();
  }

  public DrawableSplit(String pId) {
    super(pId);
  }

  public DrawableSplit(DrawableJoinSplit pOrig) {
    super(pOrig);
  }

  @Override
  public DrawableJoinSplit clone() {
    if (getClass()==DrawableSplit.class) {
      return new DrawableSplit(this);
    }
    throw new RuntimeException(new CloneNotSupportedException());
  }

  public static DrawableSplit from(Split<?> pElem) {
    DrawableSplit result = new DrawableSplit();
    copyProcessNodeAttrs(pElem, result);
    result.setMin(pElem.getMin());
    result.setMax(pElem.getMax());
    return result;
  }

  @Override
  public int getMaxSuccessorCount() {
    return Integer.MAX_VALUE;
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void draw(Canvas<S, PEN_T, PATH_T> pCanvas, Rectangle pClipBounds) {
    super.draw(pCanvas, pClipBounds);
    final S strategy = pCanvas.getStrategy();
    PATH_T path = aItems.getPath(strategy, 1);
    if (path==null) {
      path = strategy.newPath();
      if (CURVED_ARROWS) {
        path.moveTo(CENTERX-HORIZONTALDECORATIONLEN,CENTERY)
            .lineTo(CENTERX-INLEN, CENTERY)
            .cubicTo(CENTERX-INLEN*(1-ARROWCONTROLRATIO), CENTERY, CENTERX+ARROWHEADDX*(1-ARROWCONTROLRATIO)-ARROWHEADADJUST, CENTERY-ARROWHEADDY*(1-ARROWCONTROLRATIO)+ARROWHEADADJUST, CENTERX+ARROWHEADDX-ARROWHEADADJUST,CENTERY-ARROWHEADDY+ARROWHEADADJUST)
            .moveTo(CENTERX-INLEN, CENTERY)
            .cubicTo(CENTERX-INLEN*(1-ARROWCONTROLRATIO), CENTERY, CENTERX+ARROWHEADDX*(1-ARROWCONTROLRATIO)-ARROWHEADADJUST, CENTERY+ARROWHEADDY*(1-ARROWCONTROLRATIO)-ARROWHEADADJUST, CENTERX+ARROWHEADDX-ARROWHEADADJUST,CENTERY+ARROWHEADDY-ARROWHEADADJUST);
      } else {
        path.moveTo(CENTERX-HORIZONTALDECORATIONLEN,CENTERY)
            .lineTo(CENTERX, CENTERY)
            .moveTo(CENTERX+ARROWHEADDX-ARROWHEADADJUST,CENTERY-ARROWHEADDY+ARROWHEADADJUST)
            .lineTo(CENTERX, CENTERY)
            .lineTo(CENTERX+ARROWHEADDX-ARROWHEADADJUST,CENTERY+ARROWHEADDY-ARROWHEADADJUST);
      }

      path.moveTo(CENTERX+ARROWHEADDX-ARROWDNEAR,CENTERY-ARROWHEADDY+ARROWDFAR)
          .lineTo(CENTERX+ARROWHEADDX,CENTERY-ARROWHEADDY)
          .lineTo(CENTERX+ARROWHEADDX-ARROWDFAR,CENTERY-ARROWHEADDY+ARROWDNEAR)
          .moveTo(CENTERX+ARROWHEADDX-ARROWDFAR,CENTERY+ARROWHEADDY-ARROWDNEAR)
          .lineTo(CENTERX+ARROWHEADDX,CENTERY+ARROWHEADDY)
          .lineTo(CENTERX+ARROWHEADDX-ARROWDNEAR,CENTERY+ARROWHEADDY-ARROWDFAR);

      aItems.setPath(strategy, 1, path);
    }
    if (hasPos()) {
      PEN_T linePen = pCanvas.getTheme().getPen(ProcessThemeItems.INNERLINE, getState() & ~STATE_TOUCHED);
      pCanvas.drawPath(path, linePen, null);
    }
  }

  @Override
  public void serialize(SerializerAdapter pOut) {
    serializeSplit(pOut);
  }

  @Override
  public <R> R visit(ProcessNode.Visitor<R> pVisitor) {
    return pVisitor.visitSplit(this);
  }

}
