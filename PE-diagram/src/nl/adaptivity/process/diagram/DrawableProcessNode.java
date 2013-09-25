package nl.adaptivity.process.diagram;

import nl.adaptivity.diagram.Drawable;
import nl.adaptivity.process.processModel.ProcessNode;


public interface DrawableProcessNode extends ProcessNode, Drawable {

  void setId(String pId);

  void setX(double pX);

  void setY(double pY);

}
