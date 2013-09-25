package nl.adaptivity.process.diagram;
import static nl.adaptivity.process.diagram.DrawableProcessModel.*;
import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.Color;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientJoinNode;
import nl.adaptivity.process.processModel.Join;



public class DrawableJoin extends ClientJoinNode implements DrawableProcessNode {

  private static final long serialVersionUID = 1515121146796084920L;

  private Color aBlack;
  private Color aWhite;

  @Override
  public Rectangle getBounds() {
    double dx = JOINWIDTH/2;
    double dy = JOINHEIGHT/2;
    return new Rectangle(getY()-dy, getX()-dx, JOINWIDTH, JOINHEIGHT);
  }

  @Override
  public void draw(Canvas pCanvas, Rectangle pClipBounds) {
    double dx = JOINWIDTH/2;
    double dy = JOINHEIGHT/2;
    if (aBlack ==null) { aBlack = pCanvas.newColor(0,0,0,0xff); }
    if (aWhite ==null) { aWhite = pCanvas.newColor(0xff,0xff,0xff,0xff); }
    double[] points = new double[] {
      getX()-dx,getY(),
      getX(), getY()-dy,
      getX()+dx, getY(),
      getX(), getY()+dy,
      getX()-dx, getY()
    };
    pCanvas.drawFilledPath(points, aWhite);
    pCanvas.drawPath(points, aBlack);
  }

  public static DrawableJoin from(Join pElem) {
    DrawableJoin result = new DrawableJoin();
    copyProcessNodeAttrs(pElem, result);
    result.setMin(pElem.getMin());
    result.setMax(pElem.getMax());
    return result;
  }

}
