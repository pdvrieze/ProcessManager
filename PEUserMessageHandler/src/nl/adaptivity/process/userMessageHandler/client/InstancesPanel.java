package nl.adaptivity.process.userMessageHandler.client;

import nl.adaptivity.gwt.ext.client.RemoteListBox;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;


public class InstancesPanel extends Composite implements ClickHandler {

  private static final String PROCESSINSTANCELISTURL = PEUserMessageHandler.BASEURL+"/ProcessEngine/processInstances";

  private RemoteListBox aInstanceListBox;

  private Button aShowInstanceStatusButton;

  private final Label aStatusLabel;


  public InstancesPanel(Label pStatusLabel) {
    aStatusLabel = pStatusLabel;
    HorizontalPanel hp1 = new HorizontalPanel();
    hp1.addStyleName("tabPanel");

    aInstanceListBox = new RemoteListBox(PROCESSINSTANCELISTURL);
    aInstanceListBox.setRootElement("processInstances");
    aInstanceListBox.setListElement("processInstance");
    aInstanceListBox.setValueElement("@handle");
    aInstanceListBox.setTextElement("=Instance @{handle} of model (@{processModel})");


    hp1.add(aInstanceListBox);
    aInstanceListBox.addStyleName("mhList");
    aInstanceListBox.addStyleName("tabContent");

    VerticalPanel vp1 = new VerticalPanel();
    hp1.add(vp1);
    vp1.addStyleName("tabContent");

    aShowInstanceStatusButton = new Button("Show status");
    aInstanceListBox.addControlledWidget(aShowInstanceStatusButton);
    aShowInstanceStatusButton.addStyleName("inTabButton");
    vp1.add(aShowInstanceStatusButton);
    aShowInstanceStatusButton.addClickHandler(this);
    initWidget(hp1);
  }

  /**
   * @category action
   */
  private void showInstance() {
    aStatusLabel.setText("showInstance");
  }

  /**
   * @category event handler
   */
  @Override
  public void onClick(ClickEvent pEvent) {
    if (pEvent.getSource()==aShowInstanceStatusButton) {
      showInstance();
    }
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
