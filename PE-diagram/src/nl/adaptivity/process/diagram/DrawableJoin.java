package nl.adaptivity.process.diagram;
import static nl.adaptivity.process.diagram.DrawableProcessModel.*;
import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.DiagramPath;
import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.processModel.Join;



public class DrawableJoin extends DrawableJoinSplit implements Join<DrawableProcessNode> {

  public DrawableJoin(ClientProcessModel<DrawableProcessNode> pOwner) {
    super(pOwner);
  }

  public DrawableJoin(String pId, ClientProcessModel<DrawableProcessNode> pOwner) {
    super(pId, pOwner);
  }

  public DrawableJoin(DrawableJoin pOrig) {
    super(pOrig);
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

  public static DrawableJoin from(DrawableProcessModel pOwner, Join<?> pElem) {
    DrawableJoin result = new DrawableJoin(pOwner);
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
      path.moveTo(dx+hse,dy+hse)
          .lineTo(JOINWIDTH*0.875f, dy+hse)
          .moveTo(JOINWIDTH*0.75f+hse, JOINHEIGHT*0.375f+hse)
          .lineTo(JOINWIDTH*0.875f+hse, dy+hse)
          .lineTo(JOINWIDTH*0.75f+hse, JOINHEIGHT*0.625f+hse)
          .moveTo(hse+JOINWIDTH*0.3,hse+JOINHEIGHT*0.3)
          .lineTo(hse+dx, hse+dy)
          .lineTo(hse+JOINWIDTH*0.3,hse+JOINHEIGHT*0.7);
      aItems.setPath(strategy, 1, path);
    }
    if (hasPos()) {
      PEN_T linePen = pCanvas.getTheme().getPen(ProcessThemeItems.INNERLINE, getState());
      pCanvas.drawPath(path, linePen, null);
    }
  }

}
