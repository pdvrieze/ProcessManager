package nl.adaptivity.gwt.ext.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Widget;


public class BoxWidget extends Widget {


  private static BoxWidgetUiBinder uiBinder = GWT.create(BoxWidgetUiBinder.class);

  interface BoxWidgetUiBinder extends UiBinder<Element, BoxWidget> {}

  @UiField
  DivElement labelSpan;

  @UiField
  DivElement boxSpan;

  public BoxWidget(final String pLabel) {
    setElement(uiBinder.createAndBindUi(this));
    labelSpan.setInnerText(pLabel);
  }

  public DivElement getBox() {
    return boxSpan;
  }

  public String getLabel() {
    return labelSpan.getInnerHTML();
  }

  public void setLabel(final String innerHTML) {
    labelSpan.setInnerHTML(innerHTML);
  }

}
