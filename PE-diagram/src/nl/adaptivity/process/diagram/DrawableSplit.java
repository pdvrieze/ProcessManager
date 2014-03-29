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
      path.moveTo(hse,dy+hse)
          .lineTo(JOINWIDTH+hse, dy+hse)
          .moveTo(hse+dx+dx/2,hse+dy/2)
          .lineTo(hse+dx, hse+dy)
          .lineTo(hse+dx+dx/2,hse+dy+dy/2);
      aItems.setPath(strategy, 1, path);
    }
    if (hasPos()) {
      PEN_T linePen = pCanvas.getTheme().getPen(ProcessThemeItems.LINE, getState() & ~STATE_TOUCHED);
      pCanvas.drawPath(path, linePen, null);
    }
  }

}
