package nl.adaptivity.process.userMessageHandler.client.processModel;

import pl.tecna.gwt.connectors.client.Shape;
import nl.adaptivity.gwt.ext.client.BoxWidget;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;


public class EditableProcessNode extends Composite {

  private ProcessNode aNode;
  private Widget aWidget;
  private Shape aShape;

  public EditableProcessNode(ProcessNode pNode) {
    aNode = pNode;
    String extraStyle = null;
    if (pNode instanceof StartNode) {
      aWidget = new Image("images/startNode.png");
      extraStyle = "StartNode";
    } else if (pNode instanceof EndNode) {
      aWidget = new Image("images/endNode.png");
      extraStyle = "EndNode";
    } else {
      if (pNode instanceof JoinNode) {
        extraStyle = "JoinNode";
      } else if (pNode instanceof ActivityNode) {
        extraStyle = "ActivityNode";
      }
      aWidget = new BoxWidget(pNode.getId());
    }
    initWidget(aWidget);
    setStyleName("EditableProcessNode");
    if (extraStyle != null) { addStyleName(extraStyle); }
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
