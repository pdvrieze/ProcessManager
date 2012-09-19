package nl.adaptivity.darwin.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.UIObject;


public class LoginContent extends UIObject {

  private static LoginContentUiBinder uiBinder = GWT.create(LoginContentUiBinder.class);

  interface LoginContentUiBinder extends UiBinder<Element, LoginContent> {}

  @UiField
  TextBox usernameBox;

  @UiField
  PasswordTextBox passwordBox;

  @UiField
  Button loginButton;

  @UiField
  Button cancelButton;


  public LoginContent() {
    setElement(uiBinder.createAndBindUi(this));
  }

}
