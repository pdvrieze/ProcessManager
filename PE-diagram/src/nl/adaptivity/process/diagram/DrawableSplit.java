package nl.adaptivity.process.diagram;

import nl.adaptivity.diagram.*;
import nl.adaptivity.process.clientProcessModel.ClientSplitNode;
import nl.adaptivity.process.processModel.Split;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import org.jetbrains.annotations.NotNull;

import static nl.adaptivity.process.diagram.DrawableProcessModel.JOINWIDTH;
import static nl.adaptivity.process.diagram.DrawableProcessModel.STROKEWIDTH;


public class DrawableSplit extends ClientSplitNode<DrawableProcessNode, DrawableProcessModel> implements Split<DrawableProcessNode, DrawableProcessModel>, DrawableJoinSplit {

  private static final double ARROWHEADDX = JOINWIDTH*0.2;
  private static final double ARROWHEADDY = JOINWIDTH*0.2;
  private static final double ARROWHEADADJUST = (0.5*STROKEWIDTH)*Math.sqrt(0.5/(Math.sin(ARROWHEADANGLE)* Math.sin(ARROWHEADANGLE)));
  /** The y coordinate if the line were horizontal. */
  private static final double ARROWDFAR = ARROWLEN*Math.sin(0.25*Math.PI-ARROWHEADANGLE);
  /** The x coordinate if the line were horizontal. */
  private static final double ARROWDNEAR = ARROWLEN*Math.cos(0.25*Math.PI-ARROWHEADANGLE);
  private static final double INLEN = Math.sqrt(ARROWHEADDX*ARROWHEADDX+ARROWHEADDY*ARROWHEADDY);
  public static final String IDBASE = "split";
  private final DrawableJoinSplitDelegate mDrawableJoinSplitDelegate;


  public DrawableSplit(final DrawableProcessModel ownerModel) {
    super(ownerModel);
    mDrawableJoinSplitDelegate = new DrawableJoinSplitDelegate();
  }

  public DrawableSplit(final DrawableProcessModel ownerModel, String id) {
    super(ownerModel, id);
    mDrawableJoinSplitDelegate = new DrawableJoinSplitDelegate();
  }

  public DrawableSplit(Split<?,?> orig) {
    super(orig);
    if (orig instanceof DrawableSplit) {
      mDrawableJoinSplitDelegate = new DrawableJoinSplitDelegate(((DrawableSplit) orig).mDrawableJoinSplitDelegate);
    } else {
      mDrawableJoinSplitDelegate = new DrawableJoinSplitDelegate();
    }
  }

  @SuppressWarnings("CloneDoesntCallSuperClone")
  @Override
  public DrawableSplit clone() {
    if (getClass()==DrawableSplit.class) {
      return new DrawableSplit(this);
    }
    throw new RuntimeException(new CloneNotSupportedException());
  }

  @NotNull
  public static DrawableSplit deserialize(final DrawableProcessModel ownerModel, @NotNull final XmlReader in) throws XmlException {
    return XmlUtil.deserializeHelper(new DrawableSplit(ownerModel), in);
  }

  @Override
  public boolean isCompat() {
    return false;
  }

  public static DrawableSplit from(Split<?, ?> elem) {
    return new DrawableSplit(elem);
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
    mDrawableJoinSplitDelegate.draw(this, canvas, clipBounds);

    final S strategy = canvas.getStrategy();
    PATH_T path = mDrawableJoinSplitDelegate.mItems.getPath(strategy, 1);
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

      mDrawableJoinSplitDelegate.mItems.setPath(strategy, 1, path);
    }
    if (hasPos()) {
      PEN_T linePen = canvas.getTheme().getPen(ProcessThemeItems.INNERLINE, mDrawableJoinSplitDelegate.getState() & ~STATE_TOUCHED);
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
