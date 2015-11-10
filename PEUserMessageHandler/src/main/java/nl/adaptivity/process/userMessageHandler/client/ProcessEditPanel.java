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

        if (w == aNewStartNode) {
          kind = "start node";
        } else if (w == aNewActivity) {
          kind = "activity";
        } else if (w == aNewJoinNode) {
          kind = "join node";
        } else if (w == aNewEndNode) {
          kind = "end node";
        } else if (w == aArrow) {
          /*
           * SectionDecoration endDecoration = new
           * SectionDecoration(SectionDecoration.DECORATE_ARROW); int x =
           * pContext.desiredDraggableX - aDiagramPanel.getAbsoluteLeft(); int y
           * = pContext.desiredDraggableY +
           * (pContext.draggable.getOffsetHeight()/2) -
           * aDiagramPanel.getAbsoluteTop(); IConnector connector = new
           * AutoConnector(x, y ,x + pContext.draggable.getOffsetWidth(), y,
           * null, endDecoration); connector.showOnDiagram(aDiagram);
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
     * (w==aNewStartNode || w==aNewEndNode || w==aNewActivity || w==aNewJoinNode
     * || w==aArrow)) { throw new VetoDragException(); } } }
     */

    @Deprecated
    @Override
    public void onComplete(final InputCompleteEvent inputCompleteEvent) {
      if (inputCompleteEvent.isSuccess()) {
        for (final Object w : new ArrayList<Object>()) {
          final String name = inputCompleteEvent.getNewValue();
          ProcessNode processNode = null;
          if (w == aNewStartNode) {
            processNode = new StartNode(name);
          } else if (w == aNewActivity) {
            processNode = new ActivityNode(name, name, null);
          } else if (w == aNewJoinNode) {
            processNode = new JoinNode(name, null, "1", Integer.toString(Integer.MAX_VALUE));
          } else if (w == aNewEndNode) {
            processNode = new EndNode(name, null);
          }
          final EditableProcessNode editNode = new EditableProcessNode(processNode);
          //          aDiagramPanel.add(editNode, aContext.desiredDraggableX-aDiagramPanel.getAbsoluteLeft(), aContext.desiredDraggableY- aDiagramPanel.getAbsoluteTop());
          final ProcessShape shape = new ProcessShape(editNode);
          //          shape.showOnDiagram(aDiagram);
          editNode.setShape(shape);

        }
      }
    }

  }

  private boolean aEditInstance;

  private final boolean aEditable;

  private EditableProcessModel aProcessModel;

  private ProcessInstance aProcessInstance;

  private final AbsolutePanel aDiagramPanel;

  private HorizontalSplitPanel aSplitPanel;

  private VerticalPanel aSourcePanel;

  private Widget aNewStartNode;

  private Widget aNewActivity;

  private Widget aNewJoinNode;

  private Widget aNewEndNode;

  private AbsolutePanel aBoundaryPanel;

  private ProcessElementDropController aDropController;

  private Image aArrow;

  public ProcessEditPanel(final boolean editable) {
    aEditable = editable;
    aDiagramPanel = new AbsolutePanel();
    aDiagramPanel.setSize("100%", "100%");
    aDiagramPanel.addStyleName("autoscroll");
    aDiagramPanel.addStyleName("ProcessEditPanel-canvas");
    final Label label = new Label("ProcessEditPanel");
    aDiagramPanel.add(label, 10, 10);

    if (aEditable) {

      final HorizontalSplitPanel.Resources splitPanelImages = GWT.create(MyHorizontalSplitPanelImages.class);
      aSplitPanel = new HorizontalSplitPanel(splitPanelImages);
      aSplitPanel.setSplitPosition("50px");
      aSplitPanel.setRightWidget(aDiagramPanel);
      aSplitPanel.addStyleName("blackHorizontalSplitPane");

      aBoundaryPanel = new AbsolutePanel();
      aBoundaryPanel.setSize("100%", "100%");
      aBoundaryPanel.add(aSplitPanel);


      aSourcePanel = new VerticalPanel();
      aSourcePanel.setWidth("100%");
      aSourcePanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
      aSplitPanel.setLeftWidget(aSourcePanel);

      aArrow = new Image("images/arrow.png");

      aNewStartNode = new EditableProcessNode(new StartNode(null)).getDragHandle();
      aNewActivity = new EditableProcessNode(new ActivityNode("activity", "activity", null)).getDragHandle();
      aNewJoinNode = new EditableProcessNode(new JoinNode("join", null, "0", Integer.toString(Integer.MAX_VALUE))).getDragHandle();
      aNewEndNode = new EditableProcessNode(new EndNode(null, null)).getDragHandle();

      /*
       * aDragController = new PickupDragController(aBoundaryPanel, false);
       * aDragController.setBehaviorDragProxy(true);
       * aDragController.setBehaviorConstrainedToBoundaryPanel(true);
       * aDropController = new ProcessElementDropController();
       * aDragController.registerDropController(aDropController);
       */

      initWidget(aBoundaryPanel);

      for (final Widget node : new Widget[] { aArrow, aNewStartNode, aNewActivity, aNewJoinNode, aNewEndNode, }) {
        node.addStyleName("sourceoptions");
        aSourcePanel.add(node);

        /*
         * aDragController.makeDraggable(node);
         */
      }

    } else {
      initWidget(aDiagramPanel);
    }


    //    ScrollPanel scrollPanel = new ScrollPanel();
    //    scrollPanel.addStyleName("bordered");
    //    scrollPanel.add(aDiagramPanel);
    //    aDiagramPanel.addStyleName("bordered");


  }

  public void setInstance(final boolean instance) {
    aEditInstance = instance;
  }

  public void reset() {
    aProcessModel = null;
    aProcessInstance = null;
    // TODO Reset visual state
    //
    while (aDiagramPanel.getWidgetCount() > 0) {
      aDiagramPanel.remove(0);
    }
  }

  public void init(final Response response) {
    final ProcessModel plainModel = ProcessModel.fromXml(XMLParser.parse(response.getText()));
    init(plainModel);
  }

  public void init(final ProcessModel model) {
    while (aDiagramPanel.getWidgetCount() > 0) {
      aDiagramPanel.remove(0);
    }
    if (model != null) {
      aProcessModel = new EditableProcessModel(model);
      aProcessModel.layout();
      final HashMap<String, EditableProcessNode> map = new HashMap<String, EditableProcessNode>();


      for (final EditableProcessNode w : aProcessModel.getNodes()) {
        map.put(w.getNode().getId(), w);
        //        ProcessShape shapeForW = new ProcessShape(w);
        aDiagramPanel.add(w, w.getX(), w.getY());
      }


      for (final EditableProcessNode start : aProcessModel.getNodes()) {
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
             * connector.showOnDiagram(aDiagram);
             */
          }

        }
      }

    } else {
      GWT.log("Could not load process model", null);
    }
  }

  public boolean isEditable() {
    return aEditable;
  }

}
