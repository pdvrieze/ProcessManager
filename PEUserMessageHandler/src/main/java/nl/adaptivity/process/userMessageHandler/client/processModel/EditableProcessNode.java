/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

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


  private final ProcessNode mNode;

  private Widget mWidget;

  private ProcessShape mShape;

  public EditableProcessNode(final ProcessNode node) {
    mNode = node;
    String extraStyle = null;
    if (node instanceof StartNode) {
      mWidget = new Image("images/startNode.png");
      extraStyle = "StartNode";
    } else if (node instanceof EndNode) {
      mWidget = new Image("images/endNode.png");
      extraStyle = "EndNode";
    } else {
      if (node instanceof JoinNode) {
        extraStyle = "JoinNode";
      } else if (node instanceof ActivityNode) {
        extraStyle = "ActivityNode";
      }
      mWidget = new BoxWidget(node.getId());
    }
    initWidget(mWidget);
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
    return mNode.getX();
  }

  public int getY() {
    return mNode.getY();
  }

  public void setShape(final ProcessShape shape) {

    // TODO evaluate the necessity of this, for now ignore
    mShape = shape;
  }

  public ProcessNode getNode() {
    return mNode;
  }

  public ProcessShape getShape() {
    return mShape;
  }

  public Widget getDragHandle() {
    if (mWidget instanceof Image) {
      final Image img = (Image) mWidget;
      return wrapMouseEventSource(new Image(img.getUrl()));
    } else if (mWidget instanceof BoxWidget) {
      if (mNode instanceof JoinNode) {
        return wrapMouseEventSource(new BoxWidget("join"));
      } else if (mNode instanceof ActivityNode) {
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
    if (mWidget instanceof BoxWidget) {
      return ((BoxWidget) mWidget).getBox().getOffsetHeight() / 2;
    } else {
      return mWidget.getOffsetHeight() / 2;
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
