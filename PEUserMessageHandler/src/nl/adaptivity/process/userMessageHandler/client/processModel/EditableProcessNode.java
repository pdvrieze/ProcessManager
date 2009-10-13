package nl.adaptivity.process.userMessageHandler.client.processModel;

import pl.tecna.gwt.connectors.client.Shape;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;


public class EditableProcessNode extends Composite {

  private ProcessNode aNode;
  private Label aWidget;
  private int aX = -1;
  private int aY = -1;
  private Shape aShape;

  public EditableProcessNode(ProcessNode pNode) {
    aNode = pNode;
    aWidget = new Label(pNode.getId());
    initWidget(aWidget);
    setStyleName("EditableProcessNode");
  }

  public static EditableProcessNode create(ProcessNode pNode) {
    return new EditableProcessNode(pNode);
  }

  int getX() {
    return aX;
  }

  int getY() {
    return aY;
  }

  public void setShape(Shape pShape) {

    // TODO evaluate the necessity of this, for now ignore
    aShape = pShape;
  }

  public ProcessNode getNode() {
    return aNode;
  }

  public Shape getShape() {
    return aShape;
  }

}
