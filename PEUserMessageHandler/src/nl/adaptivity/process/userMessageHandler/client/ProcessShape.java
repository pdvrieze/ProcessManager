package nl.adaptivity.process.userMessageHandler.client;

import nl.adaptivity.process.userMessageHandler.client.processModel.EditableProcessNode;

import com.google.gwt.dom.client.Element;

/**
 * 
 * @author Paul de Vrieze
 * @deprecated Does not add anything
 */
@Deprecated
public class ProcessShape {

  private static final int CP_MARGIN = 13;
  private EditableProcessNode aNode;
  /*
  private ConnectionPoint aWestConnectionPoint;
  private ConnectionPoint aNorthConnectionPoint;
  private ConnectionPoint aSouthConnectionPoint;
  private ConnectionPoint aEastConnectionPoint;
  */

  public ProcessShape(EditableProcessNode pW) {
    aNode = pW;
    aNode.getElement().setDraggable(Element.DRAGGABLE_TRUE);
  }

  public EditableProcessNode getWidget() {
    return aNode;
  }

  /*
  protected void createConnectionPoints(AbsolutePanel pConnectionPointsPanel, Diagram pDiagram) {
    aWestConnectionPoint = new ConnectionPoint(ConnectionDirection.LEFT);
    aNorthConnectionPoint = new ConnectionPoint(ConnectionDirection.UP);
    aSouthConnectionPoint = new ConnectionPoint(ConnectionDirection.DOWN);
    aEastConnectionPoint = new ConnectionPoint(ConnectionDirection.RIGHT);

    aWestConnectionPoint.showOnDiagram(pDiagram);
    aNorthConnectionPoint.showOnDiagram(pDiagram);
    aSouthConnectionPoint.showOnDiagram(pDiagram);
    aEastConnectionPoint.showOnDiagram(pDiagram);

    int cpPanelHeight = pConnectionPointsPanel.getOffsetHeight();
    int cpPanelWidth = pConnectionPointsPanel.getOffsetWidth();
    int yOffset = getContainedWidget().getVerticalOffset();

    pConnectionPointsPanel.add(aWestConnectionPoint, 0, (yOffset) + (CP_MARGIN / 2));
    pConnectionPointsPanel.add(aNorthConnectionPoint, (cpPanelWidth-CP_MARGIN) / 2, 0);
    pConnectionPointsPanel.add(aEastConnectionPoint, cpPanelWidth-CP_MARGIN, (yOffset) + (CP_MARGIN / 2));
    pConnectionPointsPanel.add(aSouthConnectionPoint, (cpPanelWidth-CP_MARGIN) / 2, cpPanelHeight-CP_MARGIN);

  }*/

  public void deselect() {
  }

  /*
  @Override
  public List<ConnectionPoint> getConnectionPoints() {
    return Arrays.asList(aNorthConnectionPoint, aWestConnectionPoint, aSouthConnectionPoint, aEastConnectionPoint);
  }*/
/*

  public ConnectionPoint getWestConnectionPoint() {
    return aWestConnectionPoint;
  }


  public ConnectionPoint getNorthConnectionPoint() {
    return aNorthConnectionPoint;
  }


  public ConnectionPoint getSouthConnectionPoint() {
    return aSouthConnectionPoint;
  }


  public ConnectionPoint getEastConnectionPoint() {
    return aEastConnectionPoint;
  }
*/
}
