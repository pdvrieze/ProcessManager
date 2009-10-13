package nl.adaptivity.process.userMessageHandler.client.processModel;

import pl.tecna.gwt.connectors.client.Shape;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;


public class EditableProcessNode extends Composite {

  private ProcessNode aNode;
  private Label aWidget;
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

  public int getX() {
    return aNode.getX();
  }

  public int getY() {
    return aNode.getY();
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
