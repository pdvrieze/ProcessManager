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
    public void onDragStart(final DragStartEvent pEvent) {
      // TODO Auto-generated method stub
      // 
    }

    @Override
    public void onDrag(final DragEvent pEvent) {
      // TODO Auto-generated method stub
      // 
    }

  }


  private final ProcessNode aNode;

  private Widget aWidget;

  private ProcessShape aShape;

  public EditableProcessNode(final ProcessNode pNode) {
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
    if (extraStyle != null) {
      addStyleName(extraStyle);
    }
    getElement().setDraggable(Element.DRAGGABLE_TRUE);
    //    addDragStartHandler(new MyDragHandler());
  }

  public static EditableProcessNode create(final ProcessNode pNode) {
    return new EditableProcessNode(pNode);
  }

  public int getX() {
    return aNode.getX();
  }

  public int getY() {
    return aNode.getY();
  }

  public void setShape(final ProcessShape pShape) {

    // TODO evaluate the necessity of this, for now ignore
    aShape = pShape;
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
  private Widget wrapMouseEventSource(final Widget pWidget) {
    if (pWidget instanceof SourcesMouseEvents) {
      return pWidget;
    }
    return new FocusPanel(pWidget);
  }

  public int getVerticalOffset() {
    if (aWidget instanceof BoxWidget) {
      return ((BoxWidget) aWidget).getBox().getOffsetHeight() / 2;
    } else {
      return aWidget.getOffsetHeight() / 2;
    }
  }

  @Override
  public HandlerRegistration addDragEndHandler(final DragEndHandler pHandler) {
    return addDomHandler(pHandler, DragEndEvent.getType());
  }

  @Override
  public HandlerRegistration addDragEnterHandler(final DragEnterHandler pHandler) {
    return addDomHandler(pHandler, DragEnterEvent.getType());
  }

  @Override
  public HandlerRegistration addDragLeaveHandler(final DragLeaveHandler pHandler) {
    return addDomHandler(pHandler, DragLeaveEvent.getType());
  }

  @Override
  public HandlerRegistration addDragHandler(final DragHandler pHandler) {
    return addDomHandler(pHandler, DragEvent.getType());
  }

  @Override
  public HandlerRegistration addDragOverHandler(final DragOverHandler pHandler) {
    return addDomHandler(pHandler, DragOverEvent.getType());
  }

  @Override
  public HandlerRegistration addDragStartHandler(final DragStartHandler pHandler) {
    return addDomHandler(pHandler, DragStartEvent.getType());
  }

  @Override
  public HandlerRegistration addDropHandler(final DropHandler pHandler) {
    return addDomHandler(pHandler, DropEvent.getType());
  }

}
