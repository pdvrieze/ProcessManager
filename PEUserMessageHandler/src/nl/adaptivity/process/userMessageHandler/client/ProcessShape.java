package nl.adaptivity.process.userMessageHandler.client;

import java.util.Arrays;
import java.util.List;

import pl.tecna.gwt.connectors.client.AbstractShape;
import pl.tecna.gwt.connectors.client.ConnectionPoint;
import pl.tecna.gwt.connectors.client.Diagram;
import pl.tecna.gwt.connectors.client.ConnectionPoint.ConnectionDirection;
import nl.adaptivity.process.userMessageHandler.client.processModel.EditableProcessNode;

import com.google.gwt.user.client.ui.AbsolutePanel;


public class ProcessShape extends AbstractShape {

  private static final int CP_MARGIN = 13;
  private ConnectionPoint aWestConnectionPoint;
  private ConnectionPoint aNorthConnectionPoint;
  private ConnectionPoint aSouthConnectionPoint;
  private ConnectionPoint aEastConnectionPoint;

  public ProcessShape(EditableProcessNode pW) {
    super(pW);
  }

  @Override
  public EditableProcessNode getWidget() {
    return (EditableProcessNode) widget;
  }

  @Override
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
    int yOffset = getWidget().getVerticalOffset();

    pConnectionPointsPanel.add(aWestConnectionPoint, 0, (yOffset) + (CP_MARGIN / 2));
    pConnectionPointsPanel.add(aNorthConnectionPoint, (cpPanelWidth-CP_MARGIN) / 2, 0);
    pConnectionPointsPanel.add(aEastConnectionPoint, cpPanelWidth-CP_MARGIN, (yOffset) + (CP_MARGIN / 2));
    pConnectionPointsPanel.add(aSouthConnectionPoint, (cpPanelWidth-CP_MARGIN) / 2, cpPanelHeight-CP_MARGIN);

  }

  @Override
  public void deselect() {
  }

  @Override
  public List<ConnectionPoint> getConnectionPoints() {
    return Arrays.asList(aNorthConnectionPoint, aWestConnectionPoint, aSouthConnectionPoint, aEastConnectionPoint);
  }

  @Override
  public void select() {
  }


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

}
