package nl.adaptivity.process.userMessageHandler.client.processModel;

import pl.tecna.gwt.connectors.client.Shape;
import nl.adaptivity.gwt.ext.client.BoxWidget;

import com.allen_sauer.gwt.dnd.client.HasDragHandle;
import com.google.gwt.user.client.ui.*;


public class EditableProcessNode extends Composite implements HasDragHandle {

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

  public Widget getDragHandle() {
    if (aWidget instanceof Image) {
      Image img = (Image) aWidget;
      return wrapMouseEventSource(new Image(img.getUrl()));
    } else if (aWidget instanceof BoxWidget) {
      if (aNode instanceof JoinNode) {
        return wrapMouseEventSource(new BoxWidget("join"));
      } else if (aNode instanceof ActivityNode) {
        return wrapMouseEventSource(new BoxWidget("activity"));
      }
    }
    return this;
  }

  @SuppressWarnings("deprecation")
  private Widget wrapMouseEventSource(Widget pWidget) {
    if (pWidget instanceof SourcesMouseEvents) {
      return pWidget;
    }
    return new FocusPanel(pWidget);
  }

}
