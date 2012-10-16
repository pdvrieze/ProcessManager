package nl.adaptivity.darwin.gwt.client;

import nl.adaptivity.gwt.ext.client.XMLUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
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

  @UiField
  InputElement redirect;

  public LoginContent() {
    final Document document = Document.get();
    Element parentForm = document.getElementById("xloginform");
    if (parentForm != null) {
      parentForm = parentForm.cloneNode(true).cast();
      parentForm.setId("loginform");
      //      parentForm.removeFromParent();
      parentForm.removeAttribute("style"); // Remove the display:hidden
      parentForm.removeAttribute("action");
      username = XMLUtil.descendentWithAttribute(parentForm, "name", "xusername").cast();
      username.setName("username");
      password = XMLUtil.descendentWithAttribute(parentForm, "name", "xpassword").cast();
      password.setName("password");

      login = XMLUtil.descendentWithAttribute(parentForm, "name", "login").cast();
      cancel = XMLUtil.descendentWithAttribute(parentForm, "name", "cancel").cast();
      setElement(parentForm);
    } else {
      setElement(uiBinder.createAndBindUi(this));
    }

  }

}
