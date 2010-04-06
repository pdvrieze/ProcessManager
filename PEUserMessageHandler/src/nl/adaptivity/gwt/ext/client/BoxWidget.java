package nl.adaptivity.gwt.ext.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
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

  public BoxWidget(String pLabel) {
    setElement(uiBinder.createAndBindUi(this));
    labelSpan.setInnerText(pLabel);
  }

  public DivElement getBox() {
    return boxSpan;
  }


//  private Label aBox;
//
//  private Label aLabel;
//  private VerticalPanel aContainer;

//
//  public Label getBox() {
//    return aBox;
//  }
//
//
//  public Label getLabel() {
//    return aLabel;
//  }



}
