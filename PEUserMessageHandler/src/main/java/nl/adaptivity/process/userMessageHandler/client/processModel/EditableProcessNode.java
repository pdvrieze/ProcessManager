package nl.adaptivity.process.userMessageHandler.client.processModel;

import nl.adaptivity.gwt.ext.client.BoxWidget;
import nl.adaptivity.process.userMessageHandler.client.ProcessShape;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.*;


public class EditableProcessNode extends Composite implements HasAllDragAndDropHandlers {

  class MyDragHandler implements DragHandler, DragStartHandler {

    @Override
    public void onDragStart(final DragStartEvent event) {
      // TODO Auto-generated method stub
      // 
    }

    @Override
    public void onDrag(final DragEvent event) {
      // TODO Auto-generated method stub
      // 
    }

  }


  private final ProcessNode aNode;

  private Widget aWidget;

  private ProcessShape aShape;

  public EditableProcessNode(final ProcessNode node) {
    aNode = node;
    String extraStyle = null;
    if (node instanceof StartNode) {
      aWidget = new Image("images/startNode.png");
      extraStyle = "StartNode";
    } else if (node instanceof EndNode) {
      aWidget = new Image("images/endNode.png");
      extraStyle = "EndNode";
    } else {
      if (node instanceof JoinNode) {
        extraStyle = "JoinNode";
      } else if (node instanceof ActivityNode) {
        extraStyle = "ActivityNode";
      }
      aWidget = new BoxWidget(node.getId());
    }
    initWidget(aWidget);
    setStyleName("EditableProcessNode");
    if (extraStyle != null) {
      addStyleName(extraStyle);
    }
    getElement().setDraggable(Element.DRAGGABLE_TRUE);
    //    addDragStartHandler(new MyDragHandler());
  }

  public static EditableProcessNode create(final ProcessNode node) {
    return new EditableProcessNode(node);
  }

  public int getX() {
    return aNode.getX();
  }

  public int getY() {
    return aNode.getY();
  }

  public void setShape(final ProcessShape shape) {

    // TODO evaluate the necessity of this, for now ignore
    aShape = shape;
  }

  public ProcessNode getNode() {
    return aNode;
  }

  public ProcessShape getShape() {
    return aShape;
  }

  public Widget getDragHandle() {
    if (aWidget instanceof Image) {
      final Image img = (Image) aWidget;
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
  private Widget wrapMouseEventSource(final Widget widget) {
    if (widget instanceof SourcesMouseEvents) {
      return widget;
    }
    return new FocusPanel(widget);
  }

  public int getVerticalOffset() {
    if (aWidget instanceof BoxWidget) {
      return ((BoxWidget) aWidget).getBox().getOffsetHeight() / 2;
    } else {
      return aWidget.getOffsetHeight() / 2;
    }
  }

  @Override
  public HandlerRegistration addDragEndHandler(final DragEndHandler handler) {
    return addDomHandler(handler, DragEndEvent.getType());
  }

  @Override
  public HandlerRegistration addDragEnterHandler(final DragEnterHandler handler) {
    return addDomHandler(handler, DragEnterEvent.getType());
  }

  @Override
  public HandlerRegistration addDragLeaveHandler(final DragLeaveHandler handler) {
    return addDomHandler(handler, DragLeaveEvent.getType());
  }

  @Override
  public HandlerRegistration addDragHandler(final DragHandler handler) {
    return addDomHandler(handler, DragEvent.getType());
  }

  @Override
  public HandlerRegistration addDragOverHandler(final DragOverHandler handler) {
    return addDomHandler(handler, DragOverEvent.getType());
  }

  @Override
  public HandlerRegistration addDragStartHandler(final DragStartHandler handler) {
    return addDomHandler(handler, DragStartEvent.getType());
  }

  @Override
  public HandlerRegistration addDropHandler(final DropHandler handler) {
    return addDomHandler(handler, DropEvent.getType());
  }

}
