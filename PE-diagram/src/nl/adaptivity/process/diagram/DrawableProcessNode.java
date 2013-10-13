package nl.adaptivity.process.diagram;

import nl.adaptivity.diagram.Drawable;
import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.process.clientProcessModel.IClientProcessNode;


public interface DrawableProcessNode extends IClientProcessNode<DrawableProcessNode>, Drawable {

  void setId(String pId);

  <S extends DrawingStrategy> Pen<S> getPen(S pStrategy);

  <S extends DrawingStrategy> void setFGPen(S pStrategy, Pen<S> pPen);

}
