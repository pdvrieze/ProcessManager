package nl.adaptivity.process.diagram;

import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.DiagramPath;
import nl.adaptivity.diagram.Drawable;
import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.IClientProcessNode;


public interface DrawableProcessNode extends IClientProcessNode<DrawableProcessNode>, Drawable {

  void setId(String pId);

  void setLabel(String pLabel);

  <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void drawLabel(Canvas<S, PEN_T, PATH_T> pCanvas, Rectangle pClipBounds, double left, double top);



  @Override
  public DrawableProcessNode clone();

}
