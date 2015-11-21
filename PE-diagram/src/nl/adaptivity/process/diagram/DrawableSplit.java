package nl.adaptivity.process.diagram;

import nl.adaptivity.diagram.*;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.Split;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlWriter;

import static nl.adaptivity.process.diagram.DrawableProcessModel.*;



public class DrawableSplit extends DrawableJoinSplit implements Split<DrawableProcessNode>{

  private static final double ARROWHEADDX = JOINWIDTH*0.2;
  private static final double ARROWHEADDY = JOINWIDTH*0.2;
  private static final double ARROWHEADADJUST = (0.5*STROKEWIDTH)*Math.sqrt(0.5/(Math.sin(ARROWHEADANGLE)* Math.sin(ARROWHEADANGLE)));
  /** The y coordinate if the line were horizontal. */
  private static final double ARROWDFAR = ARROWLEN*Math.sin(0.25*Math.PI-ARROWHEADANGLE);
  /** The x coordinate if the line were horizontal. */
  private static final double ARROWDNEAR = ARROWLEN*Math.cos(0.25*Math.PI-ARROWHEADANGLE);
  private static final double INLEN = Math.sqrt(ARROWHEADDX*ARROWHEADDX+ARROWHEADDY*ARROWHEADDY);
  public static final String IDBASE = "split";


  public DrawableSplit() {
    super();
  }

  public DrawableSplit(String id) {
    super(id);
  }

  public DrawableSplit(DrawableJoinSplit orig) {
    super(orig);
  }

  @Override
  public DrawableJoinSplit clone() {
    if (getClass()==DrawableSplit.class) {
      return new DrawableSplit(this);
    }
    throw new RuntimeException(new CloneNotSupportedException());
  }

  public static DrawableSplit from(Split<?> elem) {
    DrawableSplit result = new DrawableSplit();
    copyProcessNodeAttrs(elem, result);
    result.setMin(elem.getMin());
    result.setMax(elem.getMax());
    return result;
  }

  @Override
  public int getMaxSuccessorCount() {
    return Integer.MAX_VALUE;
  }

  @Override
  public String getIdBase() {
    return IDBASE;
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void draw(Canvas<S, PEN_T, PATH_T> canvas, Rectangle clipBounds) {
    super.draw(canvas, clipBounds);
    final S strategy = canvas.getStrategy();
    PATH_T path = mItems.getPath(strategy, 1);
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

      mItems.setPath(strategy, 1, path);
    }
    if (hasPos()) {
      PEN_T linePen = canvas.getTheme().getPen(ProcessThemeItems.INNERLINE, getState() & ~STATE_TOUCHED);
      canvas.drawPath(path, linePen, null);
    }
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void drawLabel(Canvas<S, PEN_T, PATH_T> canvas, Rectangle clipBounds, double left, double top) {
    DrawableUtil.defaultDrawLabel(this, canvas, clipBounds, left, top);
  }

  @Override
  public void serialize(XmlWriter out) throws XmlException {
    serializeSplit(out);
  }

  @Override
  public <R> R visit(ProcessNode.Visitor<R> visitor) {
    return visitor.visitSplit(this);
  }

}
