package nl.adaptivity.process.diagram;

import nl.adaptivity.diagram.Drawable;
import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.process.clientProcessModel.IClientProcessNode;


public interface DrawableProcessNode extends IClientProcessNode<DrawableProcessNode>, Drawable {

  void setId(String pId);

  <S extends DrawingStrategy<S>> Pen<S> getFGPen(S pStrategy);

  <S extends DrawingStrategy<S>> void setFGPen(S pStrategy, Pen<S> pPen);

}
