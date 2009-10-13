package nl.adaptivity.process.userMessageHandler.client;

import java.util.HashMap;

import pl.tecna.gwt.connectors.client.*;
import nl.adaptivity.process.userMessageHandler.client.processModel.*;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.xml.client.XMLParser;


public class ProcessEditPanel extends Composite {

  private boolean aEditInstance;
  private EditableProcessModel aProcessModel;
  private ProcessInstance aProcessInstance;
  private AbsolutePanel aDiagramPanel;
  private Diagram aDiagram;

  public ProcessEditPanel() {
    aDiagramPanel = new AbsolutePanel();
    aDiagramPanel.setSize("100%", "100%");
    Label label = new Label("ProcessEditPanel");
    aDiagramPanel.add(label, 10, 10);




//    ScrollPanel scrollPanel = new ScrollPanel();
//    scrollPanel.addStyleName("bordered");
//    scrollPanel.add(aDiagramPanel);
    aDiagramPanel.addStyleName("bordered");

    initWidget(aDiagramPanel);

    aDiagram = new Diagram(aDiagramPanel);
  }

  public void setInstance(boolean pInstance) {
    aEditInstance = pInstance;
  }

  public void reset() {
    aProcessModel = null;
    aProcessInstance = null;
    // TODO Reset visual state
    //
    while (aDiagramPanel.getWidgetCount()>0) {
      aDiagramPanel.remove(0);
    }
  }

  public void init(Response pResponse) {
    while (aDiagramPanel.getWidgetCount()>0) {
      aDiagramPanel.remove(0);
    }
    final ProcessModel plainModel = ProcessModel.fromXml(XMLParser.parse(pResponse.getText()));
    if (plainModel !=null) {
      aProcessModel = new EditableProcessModel(plainModel);
      aProcessModel.layout();
      // TODO Auto-generated method stub
      //
      int posx = 10;

      HashMap<String, EditableProcessNode> map = new HashMap<String, EditableProcessNode>();


      int xcorrect = -aDiagramPanel.getAbsoluteLeft();
      int ycorrect = -aDiagramPanel.getAbsoluteTop();

      for (EditableProcessNode w: aProcessModel.getNodes()) {
        map.put(w.getNode().getId(), w);
        Shape shapeForW = new Shape(w);
        aDiagramPanel.add(w, w.getX()+xcorrect, w.getY()+ycorrect);
        shapeForW.showOnDiagram(aDiagram);
        w.setShape(shapeForW);
        posx+=100;
      }


      for (EditableProcessNode start: aProcessModel.getNodes()) {
        for (ProcessNode s: start.getNode().getSuccessors()) {
          EditableProcessNode end = map.get(s.getId());
          if (end!=null) {
            SectionDecoration arrowDecorator = new SectionDecoration(SectionDecoration.DECORATE_ARROW);
            ConnectionPoint startPoint= start.getShape().connectionPoints[Shape.E];
            ConnectionPoint endPoint = end.getShape().connectionPoints[Shape.W];
            Connector connector = new Connector(startPoint.getAbsoluteLeft()+xcorrect,
                startPoint.getAbsoluteTop()+ycorrect,
                endPoint.getAbsoluteLeft()+xcorrect, endPoint.getAbsoluteTop()+ycorrect,
                null, arrowDecorator);
            connector.startEndPoint.glueToConnectionPoint(startPoint);
            connector.endEndPoint.glueToConnectionPoint(endPoint);
            connector.showOnDiagram(aDiagram);
          }

        }
      }

    } else {
      GWT.log("Could not load process model", null);
    }

  }

}
