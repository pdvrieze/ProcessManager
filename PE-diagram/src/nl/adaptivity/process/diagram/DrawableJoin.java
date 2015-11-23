package nl.adaptivity.process.diagram;

import nl.adaptivity.diagram.*;
import nl.adaptivity.process.processModel.Join;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlWriter;

import javax.xml.namespace.QName;

import static nl.adaptivity.process.diagram.DrawableProcessModel.*;



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
  public static final String IDBASE = "join";

  public DrawableJoin(final boolean compat) {
    super(compat);
  }

  public DrawableJoin(String id, final boolean compat) {
    super(id, compat);
  }

  public DrawableJoin(DrawableJoin orig, final boolean compat) {
    super(orig, compat);
  }

  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  @Override
  public DrawableJoin clone() {
    if (getClass()==DrawableJoin.class) {
      return new DrawableJoin(this, isCompat());
    }
    throw new RuntimeException(new CloneNotSupportedException());
  }

  @Override
  public int getMaxPredecessorCount() {
    return Integer.MAX_VALUE;
  }

  public static DrawableJoin from(Join<?> elem, final boolean compat) {
    DrawableJoin result = new DrawableJoin(compat);
    copyProcessNodeAttrs(elem, result);
    result.setMin(elem.getMin());
    result.setMax(elem.getMax());
    return result;
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
  public void serialize(XmlWriter out) throws XmlException {
    serializeJoin(out);
  }

  @Override
  public <R> R visit(ProcessNode.Visitor<R> visitor) {
    return visitor.visitJoin(this);
  }

}
