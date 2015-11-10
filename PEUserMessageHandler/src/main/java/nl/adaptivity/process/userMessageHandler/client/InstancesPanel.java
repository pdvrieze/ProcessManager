package nl.adaptivity.process.userMessageHandler.client;

import nl.adaptivity.gwt.ext.client.ControllingListBox;
import nl.adaptivity.gwt.ext.client.RemoteListBox;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.VerticalPanel;


public class InstancesPanel extends ResizeComposite implements ClickHandler {

  private static final String PROCESSINSTANCELISTURL = PEUserMessageHandler.BASEURL + "/ProcessEngine/processInstances";

  private final RemoteListBox aInstanceListBox;

  private final Button aShowInstanceStatusButton;

  private final Label aStatusLabel;

  private Button aCancelInstanceButton;

  public InstancesPanel(final Label statusLabel) {
    aStatusLabel = statusLabel;
    final SplittedFillLeftPanel<RemoteListBox> root = new SplittedFillLeftPanel<RemoteListBox>();
    //    hp1.addStyleName("tabPanel");

    aInstanceListBox = new RemoteListBox(PROCESSINSTANCELISTURL);
    aInstanceListBox.setRootElement("processInstances");
    aInstanceListBox.setListElement("processInstance");
    aInstanceListBox.setValueElement("@handle");
    aInstanceListBox.setTextElement("=@{handle}: Instance \"@{name}\" of model (@{processModel})");


    root.setTopLeftWidget(aInstanceListBox);
    aInstanceListBox.addStyleName("mhList");
    aInstanceListBox.addStyleName("tabContent");

    final VerticalPanel vp1 = new VerticalPanel();
    root.setBottomLeftWidget(vp1);
    vp1.addStyleName("tabContent");

    aShowInstanceStatusButton = addButtonToPanel(aInstanceListBox, vp1, "Show status");

    aCancelInstanceButton = addButtonToPanel(aInstanceListBox, vp1, "Cancel instance");

    initWidget(root);
  }

  Button addButtonToPanel(ControllingListBox controller, Panel container, String label) {
    Button result = new Button(label);
    controller.addControlledWidget(result);
    result.addStyleName("inTabButton");
    container.add(result);
    result.addClickHandler(this);
    return result;
  }

  /**
   * @category event handler
   */
  @Override
  public void onClick(final ClickEvent event) {
    if (event.getSource() == aShowInstanceStatusButton) {
      showInstance();
    } else if (event.getSource() == aCancelInstanceButton) {
      cancelInstance();
    }
  }


  private void cancelInstance() {
    // TODO Auto-generated method stub
    //
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * @category action
   */
  private void showInstance() {
    aStatusLabel.setText("showInstance");
  }

  public void start() {
    aInstanceListBox.start();
  }


  public void update() {
    aInstanceListBox.update();
  }


  public void stop() {
    aInstanceListBox.stop();
  }


}
