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

  private final RemoteListBox mInstanceListBox;

  private final Button mShowInstanceStatusButton;

  private final Label mStatusLabel;

  private Button mCancelInstanceButton;

  public InstancesPanel(final Label statusLabel) {
    mStatusLabel = statusLabel;
    final SplittedFillLeftPanel<RemoteListBox> root = new SplittedFillLeftPanel<RemoteListBox>();
    //    hp1.addStyleName("tabPanel");

    mInstanceListBox = new RemoteListBox(PROCESSINSTANCELISTURL);
    mInstanceListBox.setRootElement("processInstances");
    mInstanceListBox.setListElement("processInstance");
    mInstanceListBox.setValueElement("@handle");
    mInstanceListBox.setTextElement("=@{handle}: Instance \"@{name}\" of model (@{processModel})");


    root.setTopLeftWidget(mInstanceListBox);
    mInstanceListBox.addStyleName("mhList");
    mInstanceListBox.addStyleName("tabContent");

    final VerticalPanel vp1 = new VerticalPanel();
    root.setBottomLeftWidget(vp1);
    vp1.addStyleName("tabContent");

    mShowInstanceStatusButton = addButtonToPanel(mInstanceListBox, vp1, "Show status");

    mCancelInstanceButton = addButtonToPanel(mInstanceListBox, vp1, "Cancel instance");

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
    if (event.getSource() == mShowInstanceStatusButton) {
      showInstance();
    } else if (event.getSource() == mCancelInstanceButton) {
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
    mStatusLabel.setText("showInstance");
  }

  public void start() {
    mInstanceListBox.start();
  }


  public void update() {
    mInstanceListBox.update();
  }


  public void stop() {
    mInstanceListBox.stop();
  }


}
