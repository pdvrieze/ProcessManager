package nl.adaptivity.process.diagram;
import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.DiagramPath;
import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.processModel.Join;
import nl.adaptivity.process.processModel.Split;
import static nl.adaptivity.process.diagram.DrawableProcessModel.*;



public class DrawableSplit extends DrawableJoinSplit implements Split<DrawableProcessNode>{

  private static final double ARROWHEADDX = JOINWIDTH*0.2;
  private static final double ARROWHEADDY = JOINWIDTH*0.2;
  private static final double ARROWHEADADJUST = (0.5*STROKEWIDTH)*Math.sqrt(0.5/(Math.sin(ARROWHEADANGLE)* Math.sin(ARROWHEADANGLE)));
  /** The y coordinate if the line were horizontal. */ 
  private static final double ARROWDFAR = ARROWLEN*Math.sin(0.25*Math.PI-ARROWHEADANGLE);
  /** The x coordinate if the line were horizontal. */ 
  private static final double ARROWDNEAR = ARROWLEN*Math.cos(0.25*Math.PI-ARROWHEADANGLE);
  

  public DrawableSplit(ClientProcessModel<DrawableProcessNode> pOwner) {
    super(pOwner);
  }

  public DrawableSplit(String pId, ClientProcessModel<DrawableProcessNode> pOwner) {
    super(pId, pOwner);
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

  public static DrawableJoinSplit from(DrawableProcessModel pOwner, Join<?> pElem) {
    DrawableJoinSplit result = new DrawableSplit(pOwner);
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
      path.moveTo(CENTERX-HORIZONTALDECORATIONLEN,CENTERY)
          .lineTo(CENTERX, CENTERY)
          .moveTo(CENTERX+ARROWHEADDX-ARROWHEADADJUST,CENTERY-ARROWHEADDY+ARROWHEADADJUST)
          .lineTo(CENTERX, CENTERY)
          .lineTo(CENTERX+ARROWHEADDX-ARROWHEADADJUST,CENTERY+ARROWHEADDY-ARROWHEADADJUST)
          .moveTo(CENTERX+ARROWHEADDX-ARROWDNEAR,CENTERY-ARROWHEADDY+ARROWDFAR)
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

}
