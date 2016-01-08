/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.darwin.gwt.client;

import nl.adaptivity.gwt.base.client.Clickable;
import nl.adaptivity.gwt.ext.client.XMLUtil;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.XMLParser;


public class Darwin implements EntryPoint {

  private static final String LOGIN_LOCATION = "/accounts/login.php";

  private static final String[] INLINEPREFIXES = new String[] {
    "/accounts/chpasswd",
    "/accounts/myaccount"
  };

  private class LoginReceivedCallback implements RequestCallback {

    @Override
    public void onResponseReceived(final Request request, final Response response) {
      final String text = response.getText();
      final int cpos = text.indexOf(':');
      final int eolpos = text.indexOf('\n', cpos);
      String result, payload;
      if (cpos >= 0) {
        result = text.substring(0, cpos);
        if (eolpos>=0) {
          payload = text.substring(cpos + 1, eolpos);
        } else {
          payload = text.substring(cpos + 1);
        }
      } else {
        result = text;
        payload = null;
      }

      if ("login".equals(result) && (payload != null)) {
        mUsername = payload;
        closeDialogs();
        updateLoginPanel();
        requestRefreshMenu(mLocation);
      } else if ("logout".equals(result)) {
        mUsername = null;
        closeDialogs();
        updateLoginPanel();
        requestRefreshMenu(mLocation);
        navigateTo("/", true, true);
      } else if ("error".equals(result)) {
        closeDialogs();
        error("Error validating login: " + payload, null);
      } else if ("invalid".equals(result)) {
        updateDialogTitle("Log in - Credentials invalid");
        mLoginContent.password.setValue("");
      } else {
        closeDialogs();
        error("Invalid response received from login form :" + response.getStatusCode(), null);
      }

    }

    @Override
    public void onError(final Request request, final Throwable exception) {
      error("Error validating credentials:" + exception.getMessage(), exception);
    }

  }

  private class LoginHandler implements ClickHandler {

    @Override
    public void onClick(final ClickEvent event) {
      event.stopPropagation(); // We handle the propagation.
      final com.google.gwt.user.client.Element dialogBase = dialogContent.getElement();
      final InputElement username = InputElement.as(XMLUtil.descendentWithAttribute(dialogBase, "name", "username"));
      final InputElement password = InputElement.as(XMLUtil.descendentWithAttribute(dialogBase, "name", "password"));

      RequestBuilder rBuilder;
      rBuilder = new RequestBuilder(RequestBuilder.POST, LOGIN_LOCATION);
      rBuilder.setHeader("Accept", "text/plain");
      rBuilder.setHeader("Content-Type", "application/x-www-form-urlencoded");
      final String postData = "username=" + URL.encodeQueryString(username.getValue()) + "&password=" + URL.encodeQueryString(password.getValue());
      try {
        rBuilder.sendRequest(postData, new LoginReceivedCallback());
      } catch (final RequestException e) {
        error("Could not send login request", e);
        closeDialogs();
      }

    }

  }

  private class DialogCloseHandler implements ClickHandler {

    @Override
    public void onClick(final ClickEvent event) {
      closeDialogs();
    }

  }

  private class LoginoutClickHandler implements ClickHandler {

    @Override
    public void onClick(final ClickEvent event) {
      if (mUsername == null) {
        loginDialog();
        // Login
      } else {
        try {
          RequestBuilder rBuilder;
          rBuilder = new RequestBuilder(RequestBuilder.GET, "/accounts/logout");
          rBuilder.setHeader("Accept", "application/binary");
          rBuilder.sendRequest(null, new LoginReceivedCallback());
        } catch (final RequestException e) {
          error("Could not log out", e);
        }
      }
    }

  }

  private class HistoryChangeHandler implements ValueChangeHandler<String> {

    @Override
    public void onValueChange(final ValueChangeEvent<String> event) {
      final String newValue = event.getValue();
      navigateTo(newValue, false, false);
    }

  }

  private class LinkClickHandler implements ClickHandler {

    @Override
    public void onClick(final ClickEvent event) {
      if(event.getNativeButton()==NativeEvent.BUTTON_LEFT) {
        final EventTarget target = event.getNativeEvent().getEventTarget();
        String href = target.<Element> cast().getAttribute("href");
        // handle urls to virtual pages
        if (href!=null && href.startsWith("/#")) {
          href=href.substring(2);
        }
        navigateTo(href, true, true);
        event.preventDefault();
        event.stopPropagation();
      }
    }

  }

  private class MenuReceivedCallback implements RequestCallback {

    @Override
    public void onResponseReceived(final Request request, final Response response) {
      if(response.getStatusCode()>=200 && response.getStatusCode()<300) {
        final String text = response.getText();
        mMenu.setInnerHTML(text);
        Element childElement = mMenu.getFirstChildElement();
        if (childElement!=null && childElement.getNodeName().equalsIgnoreCase("menu")) {
          while(childElement.getChildCount()>0) {
            mMenu.appendChild(childElement.getChild(0));
          }
          mMenu.removeChild(childElement);
        }
        updateMenuElements();
      } else {
        log("Error updating the menu ["+response.getStatusCode()+" "+response.getStatusText()+']');
      }
    }

    @Override
    public void onError(final Request request, final Throwable exception) {
      log("Error updating the menu", exception);
    }

  }

  private class ContentPanelCallback implements RequestCallback {

    @Override
    public void onResponseReceived(Request request, Response response) {
      int statusCode = response.getStatusCode();
      if (statusCode==401) {
        hideBanner();
        loginDialog(); // just return
        return;
      } else if (statusCode>=400 || statusCode<200) {
        hideBanner();
        error("Failure to load panel: "+statusCode, null);
        return;
      }
      mContentPanel.clear();
      hideBanner();
      final String text = response.getText();
      com.google.gwt.xml.client.Document responseDocument = XMLParser.parse(text);
      com.google.gwt.xml.client.Element root = responseDocument.getDocumentElement();
      SafeHtml title=null;
      SafeHtml body=null;
      for(Node childNode = root.getFirstChild(); childNode!=null; childNode = childNode.getNextSibling()) {
        if (childNode.getNodeType()==Node.ELEMENT_NODE) {
          com.google.gwt.xml.client.Element element = (com.google.gwt.xml.client.Element)childNode;
          if (title==null) {
            title = GwtXmlUtil.getTextContent(element);
          } else {
            try {
              body = GwtXmlUtil.serialize(element.getChildNodes());
            } catch (IllegalArgumentException e) {
              error("Failure to load page", e);
            }
          }
        }

      }
      if (title!=null) {
        com.google.gwt.dom.client.Document.get().setTitle(title.asString());
        mBanner.setInnerSafeHtml(title);
      }
      if (body!=null) {
        mContentPanel.add(new HTMLPanel(body));
      }
    }

    @Override
    public void onError(Request request, Throwable exception) {
      hideBanner();
      error("The requested location is not available", exception);
    }

  }

  @UiTemplate("darwindialog.ui.xml")
  interface DarwinDialogBinder extends UiBinder<Widget, Darwin> { /*
                                                                   * gwt
                                                                   * generated
                                                                   */};

  private static DarwinDialogBinder darwinDialogBinder = GWT.create(DarwinDialogBinder.class);

  //  interface DarwinUiBinder extends UiBinder<Widget, Darwin> { /* Dynamic gwt */}

  private DivElement mMenu;

  private String mLocation;

  private String mUsername;

  private RootPanel mContentPanel;

  private ClickHandler mLinkClickHandler;

  @UiField
  Label dialogTitle;

  @UiField
  FlowPanel dialogContent;

  private ClickHandler mDialogCloseHandler;

  private LoginContent mLoginContent;

  private HandlerRegistration mLoginoutRegistration;

  private HandlerRegistration mUsernameRegistration;

  private Element mBanner;

  @Override
  public void onModuleLoad() {
    final String initToken = History.getToken();
    if (initToken.length() == 0) {
      History.newItem(Window.Location.getPath(), false);
    }


    final Document document = Document.get();
    mMenu = (DivElement) document.getElementById("menu");
    String newLocation = History.getToken();
    final Element usernameSpan = document.getElementById("username");
    if (usernameSpan != null) {
      mUsername = usernameSpan.getInnerText();
    } else {
      mUsername = null;
    }
    mContentPanel = RootPanel.get("content");

    requestRefreshMenu(newLocation);
    
    updateMenuElements();

    registerLoginPanel(document);

    History.addValueChangeHandler(new HistoryChangeHandler());

    mBanner = document.getElementById("banner");

    // This is not a page that already has it's content.
    if (asInlineLocation(newLocation)==null) {
      showBanner();
      navigateTo(newLocation, false, false);
    } else {
      mLocation = newLocation;
    }
  }

  /**
   * @category ui_elements
   */
  private void hideBanner() {
    // Remove the banner
    if (mBanner != null) {
      mBanner.setAttribute("style", "display:none");
    }
  }

  /**
   * @category ui_elements
   */
  private void showBanner() {
    if (mBanner != null) {
      mBanner.removeAttribute("style");
    }
  }

  /**
   * @category ui_elements
   */
  private void modalDialog(final String string) {
    final Button closeButton = new Button("Ok");
    closeButton.addClickHandler(mDialogCloseHandler);
    dialog("Message", new Label(string), closeButton);
  }

  /**
   * @category error_handling
   */
  public void error(final String message, final Throwable exception) {
    GWT.log("Error: " + message, exception);
    String completeMessage;
    if (exception == null) {
      completeMessage = message;
    } else {
      completeMessage = message + "<br />" + exception.getMessage();
    }
    modalDialog(completeMessage);
  }

  private void loginDialog() {
    mLoginContent = new LoginContent();
    if (mDialogCloseHandler == null) {
      mDialogCloseHandler = new DialogCloseHandler();
    }
    dialog("Log in", mLoginContent);
    final Clickable cancel = Clickable.wrapNoAttach(mLoginContent.cancel);

    // This must be after dialog, otherwise cancelButton will not be attached (and can not get a handler)
    cancel.addClickHandler(mDialogCloseHandler);

    final Clickable login = Clickable.wrapNoAttach(mLoginContent.login);
    login.addClickHandler(new LoginHandler());
    //    mLoginContent.redirect.setValue(Window.Location.getHref());
  }

  /**
   * @category ui_elements
   */
  private void updateDialogTitle(final String string) {
    dialogTitle.setText(string);
  }

  /**
   * @category ui_elements
   */
  private void dialog(final String title, final Widget... contents) {
    final Widget dialog = darwinDialogBinder.createAndBindUi(this);
    dialogTitle.setText(title);
    RootPanel.get();
    for (final Widget w : contents) {
      dialogContent.add(w);
    }
    mContentPanel.add(dialog);
  }

  private void closeDialogs() {
    for (int i = 0; i < mContentPanel.getWidgetCount(); ++i) {
      final Widget w = mContentPanel.getWidget(i);
      final String[] styleNames = w.getStyleName().split(" ");
      for (final String styleName : styleNames) {
        if ("dialog".equals(styleName)) {
          w.removeFromParent();
          --i; // Decrease so adding will move us to our current position.
          break;
        }
      }
    }
  }

  public void navigateTo(final String newLocation, final boolean addHistory, final boolean doRedirect) {
    if ((mLocation==null && newLocation!=null) || (mLocation!=null && !mLocation.equals(newLocation))) {
      if (mLocation!=null && mLocation.startsWith("/accounts/myaccount")) {
        mLocation = newLocation;
        updateLoginPanel();
      } else {
        mLocation = newLocation;
      }
      updateMenuTabs();
      
      if (mLocation.equals("/")|| mLocation.equals("") || mLocation==null) {
        hideBanner();
        setInboxPanel();
      } else if (mLocation.equals("/actions")) {
        hideBanner();
        setActionPanel();
      } else if (mLocation.equals("/processes")) {
        hideBanner();
        setProcessesPanel();
      } else if (mLocation.equals("/about")) {
        hideBanner();
        setAboutPanel();
      } else if (mLocation.equals("/presentations")) {
        hideBanner();
        setPresentationPanel();
      } else { 
        String location = asInlineLocation(mLocation);
        if (location!=null){
          mLocation = location;
          mContentPanel.clear();
          mContentPanel.add(new Label("Loading..."));
          RequestBuilder rBuilder;
          rBuilder = new RequestBuilder(RequestBuilder.GET, location);
          rBuilder.setHeader("Accept", "text/xml");
          rBuilder.setHeader("X-Darwin", "nochrome");
          try {
            rBuilder.sendRequest(null, new ContentPanelCallback());
          } catch (final RequestException e) {
            error("Could load requested content", e);
            closeDialogs();
          }
      
      
        } else {
          if (doRedirect) {
            // Load the page
            Window.Location.assign(newLocation);
          } else {
            hideBanner();
          }
        }
      }
      if (addHistory) {
        History.newItem(mLocation, false);
      }
    }
  }

  private void updateMenuTabs() {
    for(Element menuitem=mMenu.getFirstChildElement();menuitem!=null; menuitem=menuitem.getNextSiblingElement()) {
      updateLinkItem(menuitem);
    }
  }

  private static String asInlineLocation(String location) {
    for(String prefix:INLINEPREFIXES) {
      if (location.startsWith(prefix)) {
        return prefix;
      }
    }
    return null;
  }

  private void updateLinkItem(Element menuitem) {
    String href = menuitem.getAttribute("href");
    if (href!=null && href.length()>0) {
      if (href.startsWith("/#")) {
        href=href.substring(2);
      }
      if (href.equals(mLocation)) {
        menuitem.addClassName("active");
      } else {
        menuitem.removeClassName("active");
      }
    }
  }

  /**
   * Make the menu elements active and add an onClick Listener.
   */
  public void updateMenuElements() {
    if (mLinkClickHandler == null) {
      mLinkClickHandler = new LinkClickHandler();
    }
    for (Element item = mMenu.getFirstChildElement(); item != null; item = item.getNextSiblingElement()) {
      final Anchor l = Anchor.wrap(item);
      l.addClickHandler(mLinkClickHandler);
      updateLinkItem(item);
    }
  }

  private void setPresentationPanel() {
    mContentPanel.clear();
    mContentPanel.add(new PresentationPanel(mUsername));
  }

  private void setInboxPanel() {
    mContentPanel.clear();
    mContentPanel.add(new Label("Inbox Panel - work in progress"));
  }

  private void setProcessesPanel() {
    mContentPanel.clear();
    mContentPanel.add(new Label("Processes Panel - work in progress"));
  }

  private void setActionPanel() {
    mContentPanel.clear();
    mContentPanel.add(new Label("ActionPanel - work in progress"));
  }

  private void setAboutPanel() {
    mContentPanel.clear();
    mContentPanel.add(new AboutPanel());
//    showBanner();
//    GWT.runAsync(new RunAsyncCallback() {
//
//      @Override
//      public void onSuccess() {
//        hideBanner();
//        mContentPanel.clear();
//        mContentPanel.add(new AboutPanel());
//      }
//
//      @Override
//      public void onFailure(final Throwable pReason) {
//        hideBanner();
//        mContentPanel.clear();
//        mContentPanel.add(new Label("Could not load about page module"));
//      }
//    });
  }


  private void registerLoginPanel(final Document document) {
    Element logout = document.getElementById("logout");
    if (logout!=null) {
      final Clickable loginout = Clickable.wrap(logout);
      loginout.getElement().removeAttribute("href");
      mLoginoutRegistration = loginout.addClickHandler(new LoginoutClickHandler());
    } else {
      mLoginoutRegistration = null;
    }
    
    Element username = document.getElementById("username");
    if (username!=null) {
      mUsernameRegistration = Clickable.wrapNoAttach(username).addClickHandler(mLinkClickHandler);
    } else {
      mUsernameRegistration = null;
    }
  }

  private void unregisterLoginPanel() {
    if (mLoginoutRegistration!=null) {
      mLoginoutRegistration.removeHandler();
    }
    if (mUsernameRegistration!=null) {
      mUsernameRegistration.removeHandler();
    }
  }
    

  private void updateLoginPanel() {
    final Document document = Document.get();
    unregisterLoginPanel();

    final DivElement loginPanel = document.getElementById("login").cast();
    if (mUsername != null) {
      if (mLocation.startsWith("/accounts/myaccount")) {
        loginPanel.setInnerHTML("<a href=\"/accounts/myaccount\" class=\"active\" id=\"username\">" + mUsername + "</a><a id=\"logout\">logout</a>");
      } else {
        loginPanel.setInnerHTML("<a href=\"/accounts/myaccount\" id=\"username\">" + mUsername + "</a><a id=\"logout\">logout</a>");
      }
    } else {
      loginPanel.setInnerHTML("<a id=\"logout\">login</a>");
      mUsernameRegistration = null;
    }
    registerLoginPanel(document);
  }


  private void requestRefreshMenu(String location) {
    RequestBuilder rBuilder;
    rBuilder = new RequestBuilder(RequestBuilder.GET, "/common/menu.php?location=" + URL.encode(location));
    try {
      rBuilder.sendRequest(null, new MenuReceivedCallback());
    } catch (final RequestException e) {
      log("Could not update menu", e);
    }
  }

  private static void log(final String message, final Throwable throwable) {
    GWT.log(message, throwable);
  }

  private static void log(final String message) {
    GWT.log(message);
  }

}
