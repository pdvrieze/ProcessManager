package nl.adaptivity.darwin.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Widget;


public class LoginContent extends Widget {

  private static LoginContentUiBinder uiBinder = GWT.create(LoginContentUiBinder.class);

  interface LoginContentUiBinder extends UiBinder<Element, LoginContent> {/* */}

  @UiField
  InputElement username;

  @UiField
  InputElement password;

  @UiField
  InputElement login;

  @UiField
  InputElement cancel;

  public LoginContent() {

    setElement(uiBinder.createAndBindUi(this));

  }

}
