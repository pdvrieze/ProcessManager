package nl.adaptivity.darwin.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;


public class LoginContent extends Widget {

  private static LoginContentUiBinder uiBinder = GWT.create(LoginContentUiBinder.class);

  interface LoginContentUiBinder extends UiBinder<Element, LoginContent> {/* */}

  @UiField
  InputElement username;
  
  TextBox usernameBox;

  @UiField
  InputElement password;
  
  PasswordTextBox passwordBox;

  @UiField
  InputElement login;
  Button loginButton;

  @UiField
  InputElement cancel;
  Button cancelButton;


  public LoginContent() {
    
    setElement(uiBinder.createAndBindUi(this));
    usernameBox = TextBox.wrap(username);
    passwordBox = PasswordTextBox.wrap(password);
    loginButton = Button.wrap(login);
    cancelButton = Button.wrap(cancel);
    
  }

}
