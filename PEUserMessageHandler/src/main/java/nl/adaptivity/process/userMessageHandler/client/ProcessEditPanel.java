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

package nl.adaptivity.process.userMessageHandler.client;

import java.util.ArrayList;
import java.util.HashMap;

import nl.adaptivity.gwt.ext.client.TextInputPopup;
import nl.adaptivity.gwt.ext.client.TextInputPopup.InputCompleteEvent;
import nl.adaptivity.gwt.ext.client.TextInputPopup.InputCompleteHandler;
import nl.adaptivity.process.userMessageHandler.client.processModel.*;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.DropEvent;
import com.google.gwt.http.client.Response;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.xml.client.XMLParser;


public class ProcessEditPanel extends Composite {


  public static interface MyHorizontalSplitPanelImages extends HorizontalSplitPanel.Resources {

    @Source(value = "blackSplitPanel.png")
    @Override
    ImageResource horizontalSplitPanelThumb();

  }

  private class ProcessElementDropController implements InputCompleteHandler {


    @Deprecated
    public void onDrop(final DropEvent event) {
      final ArrayList<Object> context = new ArrayList<Object>();
      for (final Object w : context) {
        String kind = null;

        if (w == mNewStartNode) {
          kind = "start node";
        } else if (w == mNewActivity) {
          kind = "activity";
        } else if (w == mNewJoinNode) {
          kind = "join node";
        } else if (w == mNewEndNode) {
          kind = "end node";
        } else if (w == mArrow) {
          /*
           * SectionDecoration endDecoration = new
           * SectionDecoration(SectionDecoration.DECORATE_ARROW); int x =
           * pContext.desiredDraggableX - mDiagramPanel.getAbsoluteLeft(); int y
           * = pContext.desiredDraggableY +
           * (pContext.draggable.getOffsetHeight()/2) -
           * mDiagramPanel.getAbsoluteTop(); IConnector connector = new
           * AutoConnector(x, y ,x + pContext.draggable.getOffsetWidth(), y,
           * null, endDecoration); connector.showOnDiagram(mDiagram);
           */
          throw new UnsupportedOperationException("Not supported for arrows");
          //          continue;
        }

        final TextInputPopup namePopup = new TextInputPopup("What is the name of the new " + kind, "Create");
        namePopup.addInputCompleteHandler(ProcessElementDropController.this);
        namePopup.show();
      }
    }

    /*
     * @Override public void onPreviewDrop(DragContext pContext) throws
     * VetoDragException { for (Object w: pContext.selectedWidgets) { if (!
     * (w==mNewStartNode || w==mNewEndNode || w==mNewActivity || w==mNewJoinNode
     * || w==mArrow)) { throw new VetoDragException(); } } }
     */

    @Deprecated
    @Override
    public void onComplete(final InputCompleteEvent inputCompleteEvent) {
      if (inputCompleteEvent.isSuccess()) {
        for (final Object w : new ArrayList<Object>()) {
          final String name = inputCompleteEvent.getNewValue();
          ProcessNode processNode = null;
          if (w == mNewStartNode) {
            processNode = new StartNode(name);
          } else if (w == mNewActivity) {
            processNode = new ActivityNode(name, name, null);
          } else if (w == mNewJoinNode) {
            processNode = new JoinNode(name, null, "1", Integer.toString(Integer.MAX_VALUE));
          } else if (w == mNewEndNode) {
            processNode = new EndNode(name, null);
          }
          final EditableProcessNode editNode = new EditableProcessNode(processNode);
          //          mDiagramPanel.add(editNode, mContext.desiredDraggableX-mDiagramPanel.getAbsoluteLeft(), mContext.desiredDraggableY- mDiagramPanel.getAbsoluteTop());
          final ProcessShape shape = new ProcessShape(editNode);
          //          shape.showOnDiagram(mDiagram);
          editNode.setShape(shape);

        }
      }
    }

  }

  private boolean mEditInstance;

  private final boolean mEditable;

  private EditableProcessModel mProcessModel;

  private ProcessInstance mProcessInstance;

  private final AbsolutePanel mDiagramPanel;

  private HorizontalSplitPanel mSplitPanel;

  private VerticalPanel mSourcePanel;

  private Widget mNewStartNode;

  private Widget mNewActivity;

  private Widget mNewJoinNode;

  private Widget mNewEndNode;

  private AbsolutePanel mBoundaryPanel;

  private ProcessElementDropController mDropController;

  private Image mArrow;

  public ProcessEditPanel(final boolean editable) {
    mEditable = editable;
    mDiagramPanel = new AbsolutePanel();
    mDiagramPanel.setSize("100%", "100%");
    mDiagramPanel.addStyleName("autoscroll");
    mDiagramPanel.addStyleName("ProcessEditPanel-canvas");
    final Label label = new Label("ProcessEditPanel");
    mDiagramPanel.add(label, 10, 10);

    if (mEditable) {

      final HorizontalSplitPanel.Resources splitPanelImages = GWT.create(MyHorizontalSplitPanelImages.class);
      mSplitPanel = new HorizontalSplitPanel(splitPanelImages);
      mSplitPanel.setSplitPosition("50px");
      mSplitPanel.setRightWidget(mDiagramPanel);
      mSplitPanel.addStyleName("blackHorizontalSplitPane");

      mBoundaryPanel = new AbsolutePanel();
      mBoundaryPanel.setSize("100%", "100%");
      mBoundaryPanel.add(mSplitPanel);


      mSourcePanel = new VerticalPanel();
      mSourcePanel.setWidth("100%");
      mSourcePanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
      mSplitPanel.setLeftWidget(mSourcePanel);

      mArrow = new Image("images/arrow.png");

      mNewStartNode = new EditableProcessNode(new StartNode(null)).getDragHandle();
      mNewActivity = new EditableProcessNode(new ActivityNode("activity", "activity", null)).getDragHandle();
      mNewJoinNode = new EditableProcessNode(new JoinNode("join", null, "0", Integer.toString(Integer.MAX_VALUE))).getDragHandle();
      mNewEndNode = new EditableProcessNode(new EndNode(null, null)).getDragHandle();

      /*
       * mDragController = new PickupDragController(mBoundaryPanel, false);
       * mDragController.setBehaviorDragProxy(true);
       * mDragController.setBehaviorConstrainedToBoundaryPanel(true);
       * mDropController = new ProcessElementDropController();
       * mDragController.registerDropController(mDropController);
       */

      initWidget(mBoundaryPanel);

      for (final Widget node : new Widget[] { mArrow, mNewStartNode, mNewActivity, mNewJoinNode, mNewEndNode, }) {
        node.addStyleName("sourceoptions");
        mSourcePanel.add(node);

        /*
         * mDragController.makeDraggable(node);
         */
      }

    } else {
      initWidget(mDiagramPanel);
    }


    //    ScrollPanel scrollPanel = new ScrollPanel();
    //    scrollPanel.addStyleName("bordered");
    //    scrollPanel.add(mDiagramPanel);
    //    mDiagramPanel.addStyleName("bordered");


  }

  public void setInstance(final boolean instance) {
    mEditInstance = instance;
  }

  public void reset() {
    mProcessModel = null;
    mProcessInstance = null;
    // TODO Reset visual state
    //
    while (mDiagramPanel.getWidgetCount() > 0) {
      mDiagramPanel.remove(0);
    }
  }

  public void init(final Response response) {
    final ProcessModel plainModel = ProcessModel.fromXml(XMLParser.parse(response.getText()));
    init(plainModel);
  }

  public void init(final ProcessModel model) {
    while (mDiagramPanel.getWidgetCount() > 0) {
      mDiagramPanel.remove(0);
    }
    if (model != null) {
      mProcessModel = new EditableProcessModel(model);
      mProcessModel.layout();
      final HashMap<String, EditableProcessNode> map = new HashMap<String, EditableProcessNode>();


      for (final EditableProcessNode w : mProcessModel.getNodes()) {
        map.put(w.getNode().getId(), w);
        //        ProcessShape shapeForW = new ProcessShape(w);
        mDiagramPanel.add(w, w.getX(), w.getY());
      }


      for (final EditableProcessNode start : mProcessModel.getNodes()) {
        for (final ProcessNode s : start.getNode().getSuccessors()) {
          final EditableProcessNode end = map.get(s.getId());
          if (end != null) {
            /*
             * SectionDecoration arrowDecorator = new
             * SectionDecoration(SectionDecoration.DECORATE_ARROW);
             * ConnectionPoint startPoint=
             * start.getShape().getEastConnectionPoint(); ConnectionPoint
             * endPoint = end.getShape().getWestConnectionPoint(); IConnector
             * connector = new
             * AutoConnector(startPoint.getAbsoluteLeft()+xcorrect
             * +(startPoint.getOffsetWidth()/2),
             * startPoint.getAbsoluteTop()+ycorrect +
             * (startPoint.getOffsetHeight()/2),
             * endPoint.getAbsoluteLeft()+xcorrect +
             * (endPoint.getOffsetWidth()/2),
             * endPoint.getAbsoluteTop()+ycorrect+
             * (endPoint.getOffsetHeight()/2), null, arrowDecorator);
             * connector.getStartEndPoint().glueToConnectionPoint(startPoint);
             * connector.getEndEndPoint().glueToConnectionPoint(endPoint);
             * connector.showOnDiagram(mDiagram);
             */
          }

        }
      }

    } else {
      GWT.log("Could not load process model", null);
    }
  }

  public boolean isEditable() {
    return mEditable;
  }

}
