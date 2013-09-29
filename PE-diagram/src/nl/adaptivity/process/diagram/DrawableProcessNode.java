package nl.adaptivity.process.diagram;

import nl.adaptivity.diagram.Drawable;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.process.clientProcessModel.IClientProcessNode;


public interface DrawableProcessNode extends IClientProcessNode<DrawableProcessNode>, Drawable {

  void setId(String pId);

  Pen getPen();

  void setFGPen(Pen pPen);

}
