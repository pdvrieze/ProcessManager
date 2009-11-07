package nl.adaptivity.gwt.ext.client;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;


public class BoxWidget extends Composite {

  private Label aBox;

  private Label aLabel;
  private VerticalPanel aContainer;

  public BoxWidget(String pLabel) {
    aContainer = new VerticalPanel();
    aContainer.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

    aBox = new Label("");
    aBox.setStyleName("pwt-BoxWidget-box");
    aBox.setSize("20px", "20px");
    aLabel = new Label(pLabel);
    aLabel.setStyleName("pwt-BoxWidget-label");
    aContainer.add(aBox);
    aContainer.add(aLabel);
    initWidget(aContainer);
    setStyleName("pwt-BoxWidget");

    // TODO Auto-generated constructor stub
  }


  public Label getBox() {
    return aBox;
  }


  public Label getLabel() {
    return aLabel;
  }



}
