package nl.adaptivity.process.userMessageHandler.client;

import nl.adaptivity.process.userMessageHandler.client.processModel.EditableProcessNode;

import com.google.gwt.dom.client.Element;


/**
 * @author Paul de Vrieze
 * @deprecated Does not add anything
 */
@Deprecated
public class ProcessShape {

  private static final int CP_MARGIN = 13;

  private final EditableProcessNode mNode;

  /*
   * private ConnectionPoint mWestConnectionPoint; private ConnectionPoint
   * mNorthConnectionPoint; private ConnectionPoint mSouthConnectionPoint;
   * private ConnectionPoint mEastConnectionPoint;
   */

  public ProcessShape(final EditableProcessNode w) {
    mNode = w;
    mNode.getElement().setDraggable(Element.DRAGGABLE_TRUE);
  }

  public EditableProcessNode getWidget() {
    return mNode;
  }

  /*
   * protected void createConnectionPoints(AbsolutePanel pConnectionPointsPanel,
   * Diagram pDiagram) { mWestConnectionPoint = new
   * ConnectionPoint(ConnectionDirection.LEFT); mNorthConnectionPoint = new
   * ConnectionPoint(ConnectionDirection.UP); mSouthConnectionPoint = new
   * ConnectionPoint(ConnectionDirection.DOWN); mEastConnectionPoint = new
   * ConnectionPoint(ConnectionDirection.RIGHT);
   * mWestConnectionPoint.showOnDiagram(pDiagram);
   * mNorthConnectionPoint.showOnDiagram(pDiagram);
   * mSouthConnectionPoint.showOnDiagram(pDiagram);
   * mEastConnectionPoint.showOnDiagram(pDiagram); int cpPanelHeight =
   * pConnectionPointsPanel.getOffsetHeight(); int cpPanelWidth =
   * pConnectionPointsPanel.getOffsetWidth(); int yOffset =
   * getContainedWidget().getVerticalOffset();
   * pConnectionPointsPanel.add(mWestConnectionPoint, 0, (yOffset) + (CP_MARGIN
   * / 2)); pConnectionPointsPanel.add(mNorthConnectionPoint,
   * (cpPanelWidth-CP_MARGIN) / 2, 0);
   * pConnectionPointsPanel.add(mEastConnectionPoint, cpPanelWidth-CP_MARGIN,
   * (yOffset) + (CP_MARGIN / 2));
   * pConnectionPointsPanel.add(mSouthConnectionPoint, (cpPanelWidth-CP_MARGIN)
   * / 2, cpPanelHeight-CP_MARGIN); }
   */

  public void deselect() {}

  /*
   * @Override public List<ConnectionPoint> getConnectionPoints() { return
   * Arrays.asList(mNorthConnectionPoint, mWestConnectionPoint,
   * mSouthConnectionPoint, mEastConnectionPoint); }
   */
  /*
   * public ConnectionPoint getWestConnectionPoint() { return
   * mWestConnectionPoint; } public ConnectionPoint getNorthConnectionPoint() {
   * return mNorthConnectionPoint; } public ConnectionPoint
   * getSouthConnectionPoint() { return mSouthConnectionPoint; } public
   * ConnectionPoint getEastConnectionPoint() { return mEastConnectionPoint; }
   */
}
