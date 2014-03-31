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

  public DrawableSplit(ClientProcessModel<DrawableProcessNode> pOwner) {
    super(pOwner);
  }

  public DrawableSplit(String pId, ClientProcessModel<DrawableProcessNode> pOwner) {
    super(pId, pOwner);
  }

  public DrawableSplit(DrawableSplit pOrig) {
    super(pOrig);
  }

  @Override
  public DrawableSplit clone() {
    if (getClass()==DrawableSplit.class) {
      return new DrawableSplit(this);
    }
    throw new RuntimeException(new CloneNotSupportedException());
  }

  public static DrawableSplit from(DrawableProcessModel pOwner, Join<?> pElem) {
    DrawableSplit result = new DrawableSplit(pOwner);
    copyProcessNodeAttrs(pElem, result);
    result.setMin(pElem.getMin());
    result.setMax(pElem.getMax());
    return result;
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void draw(Canvas<S, PEN_T, PATH_T> pCanvas, Rectangle pClipBounds) {
    super.draw(pCanvas, pClipBounds);
    final S strategy = pCanvas.getStrategy();
    PATH_T path = aItems.getPath(strategy, 1);
    if (path==null) {
      final double dx = JOINWIDTH/2;
      final double dy = JOINHEIGHT/2;
      path = strategy.newPath();
      final double hse = STROKEEXTEND/2;
      // substract the strokewith to take the CAP away.
      // How long is the arrow extend in the major direction
      final double arrowlen = Math.sqrt(JOINWIDTH*JOINWIDTH*0.115*0.115*2-STROKEWIDTH*2);
      // How long is the arrow in the minor direction (to have non 45 deg angles
      final double arrowadjust = arrowlen*0.1;
      path.moveTo(hse+JOINWIDTH*0.1,dy+hse)
          .lineTo(dx+hse, dy+hse)
          .moveTo(hse+JOINWIDTH*0.7-STROKEWIDTH*0.5,hse+JOINHEIGHT*0.3+STROKEWIDTH*0.5)
          .lineTo(hse+dx, hse+dy)
          .lineTo(hse+JOINWIDTH*0.7-STROKEWIDTH*0.5,hse+JOINHEIGHT*0.7-STROKEWIDTH*0.5)
          .moveTo(hse+JOINWIDTH*0.7-arrowlen,hse+JOINHEIGHT*0.3+arrowadjust)
          .lineTo(hse+JOINWIDTH*0.7,hse+JOINHEIGHT*0.3)
          .lineTo(hse+JOINWIDTH*0.7-arrowadjust,hse+JOINHEIGHT*0.3+arrowlen)
          .moveTo(hse+JOINWIDTH*0.7-arrowlen,hse+JOINHEIGHT*0.7-arrowadjust)
          .lineTo(hse+JOINWIDTH*0.7,hse+JOINHEIGHT*0.7)
          .lineTo(hse+JOINWIDTH*0.7-arrowadjust,hse+JOINHEIGHT*0.7-arrowlen);
      
      aItems.setPath(strategy, 1, path);
    }
    if (hasPos()) {
      PEN_T linePen = pCanvas.getTheme().getPen(ProcessThemeItems.INNERLINE, getState() & ~STATE_TOUCHED);
      pCanvas.drawPath(path, linePen, null);
    }
  }

}
