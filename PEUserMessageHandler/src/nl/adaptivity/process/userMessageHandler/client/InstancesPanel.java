package nl.adaptivity.process.userMessageHandler.client;

import nl.adaptivity.gwt.ext.client.RemoteListBox;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.VerticalPanel;


public class InstancesPanel extends ResizeComposite implements ClickHandler {

  private static final String PROCESSINSTANCELISTURL = PEUserMessageHandler.BASEURL+"/ProcessEngine/processInstances";

  private RemoteListBox aInstanceListBox;

  private Button aShowInstanceStatusButton;

  private final Label aStatusLabel;

  public InstancesPanel(Label pStatusLabel) {
    aStatusLabel = pStatusLabel;
    SplittedFillLeftPanel<RemoteListBox> root = new SplittedFillLeftPanel<RemoteListBox>();
//    hp1.addStyleName("tabPanel");

    aInstanceListBox = new RemoteListBox(PROCESSINSTANCELISTURL);
    aInstanceListBox.setRootElement("processInstances");
    aInstanceListBox.setListElement("processInstance");
    aInstanceListBox.setValueElement("@handle");
    aInstanceListBox.setTextElement("=@{handle}: Instance \"@{name}\" of model (@{processModel})");


    root.setTopLeftWidget(aInstanceListBox);
    aInstanceListBox.addStyleName("mhList");
    aInstanceListBox.addStyleName("tabContent");

    VerticalPanel vp1 = new VerticalPanel();
    root.setBottomLeftWidget(vp1);
    vp1.addStyleName("tabContent");

    aShowInstanceStatusButton = new Button("Show status");
    aInstanceListBox.addControlledWidget(aShowInstanceStatusButton);
    aShowInstanceStatusButton.addStyleName("inTabButton");
    vp1.add(aShowInstanceStatusButton);
    aShowInstanceStatusButton.addClickHandler(this);
    initWidget(root);
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
