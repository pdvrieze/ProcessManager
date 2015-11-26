package nl.adaptivity.process.diagram;

import nl.adaptivity.diagram.*;
import nl.adaptivity.process.clientProcessModel.ClientJoinNode;
import nl.adaptivity.process.processModel.Join;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import org.jetbrains.annotations.NotNull;

import static nl.adaptivity.process.diagram.DrawableProcessModel.*;


public class DrawableJoin extends ClientJoinNode<DrawableProcessNode, DrawableProcessModel> implements Join<DrawableProcessNode, DrawableProcessModel>, DrawableJoinSplit {

  private static final double ARROWHEADDX = JOINWIDTH*0.375;
  private static final double ARROWHEADADJUST = 0.5*STROKEWIDTH/Math.sin(DrawableJoinSplit.ARROWHEADANGLE);

  /** The y coordinate if the line were horizontal. */
  private static final double ARROWDFAR = DrawableJoinSplit.ARROWLEN*Math.sin(DrawableJoinSplit.ARROWHEADANGLE);
  /** The x coordinate if the line were horizontal. */
  private static final double ARROWDNEAR = DrawableJoinSplit.ARROWLEN*Math.cos(DrawableJoinSplit.ARROWHEADANGLE);
  private static final double INDX = JOINWIDTH*0.2;
  private static final double INDY = JOINHEIGHT*0.2;
  private static final double INLEN = Math.sqrt(INDX*INDX+INDY*INDY);

  private final DrawableJoinSplitDelegate mDrawableJoinSplitDelegate;

  public DrawableJoin(final DrawableProcessModel ownerModel, final boolean compat) {
    super(ownerModel, compat);
    mDrawableJoinSplitDelegate = new DrawableJoinSplitDelegate();
  }

  public DrawableJoin(final DrawableProcessModel ownerModel, String id, final boolean compat) {
    super(ownerModel, id, compat);
    mDrawableJoinSplitDelegate = new DrawableJoinSplitDelegate();
  }

  public DrawableJoin(DrawableJoin orig, final boolean compat) {
    super(orig, compat);
    mDrawableJoinSplitDelegate = new DrawableJoinSplitDelegate(orig.mDrawableJoinSplitDelegate);
  }

  @Override
  public DrawableJoin clone() {
    if (getClass()==DrawableJoin.class) {
      return new DrawableJoin(this, isCompat());
    }
    throw new RuntimeException(new CloneNotSupportedException());
  }

  public static DrawableJoin from(Join<?, ?> elem, final boolean compat) {
    return new DrawableJoin((DrawableProcessModel)null, compat);
  }

  @NotNull
  public static DrawableJoin deserialize(final DrawableProcessModel ownerModel, @NotNull final XmlReader in) throws XmlException {
    return XmlUtil.deserializeHelper(new DrawableJoin(ownerModel, true), in);
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void draw(Canvas<S, PEN_T, PATH_T> canvas, Rectangle clipBounds) {
    mDrawableJoinSplitDelegate.draw(this, canvas, clipBounds);

    final S strategy = canvas.getStrategy();
    PATH_T path = mDrawableJoinSplitDelegate.mItems.getPath(strategy, 1);
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
      mDrawableJoinSplitDelegate.mItems.setPath(strategy, 1, path);
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
  public void setPos(final double left, final double top) {
    setX(left);
    setY(top);
  }

  @Override
  public Rectangle getBounds() {
    return DrawableJoinSplitDelegate.getBounds(this);
  }

  @Override
  public Drawable getItemAt(double x, double y) {
    return DrawableJoinSplitDelegate.getItemAt(this, x, y);
  }

  @Override
  public int getState() {
    return mDrawableJoinSplitDelegate.getState();
  }

  @Override
  public void setState(int state) {
    mDrawableJoinSplitDelegate.setState(state);
  }

  public String getMinMaxText() {
    return DrawableJoinSplitDelegate.getMinMaxText(this);
  }

}
