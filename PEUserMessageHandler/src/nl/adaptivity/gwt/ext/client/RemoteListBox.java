package nl.adaptivity.gwt.ext.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.*;
import com.google.gwt.http.client.RequestBuilder.Method;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.XMLParser;


public class RemoteListBox extends ControllingListBox implements RequestCallback {


  private static class ListElement {

    final String value;

    final String text;

    public ListElement(final String pValue, final String pText) {
      value = pValue;
      text = pText;
    }

  }

  public static class UpdateEvent extends GwtEvent<UpdateEventHandler> {

    private static Type<UpdateEventHandler> TYPE;

    private final Throwable aException;

    public UpdateEvent(final Throwable pException) {
      aException = pException;
    }

    public static Type<UpdateEventHandler> getType() {
      if (TYPE == null) {
        TYPE = new Type<UpdateEventHandler>();
      }
      return TYPE;
    }

    @Override
    protected void dispatch(final UpdateEventHandler pHandler) {
      pHandler.onUpdateRemoteList(this);
    }

    @Override
    public Type<UpdateEventHandler> getAssociatedType() {
      return getType();
    }

    public boolean isException() {
      return aException != null;
    }

    public Throwable getException() {
      return aException;
    }

  }

  public interface UpdateEventHandler extends EventHandler {

    void onUpdateRemoteList(UpdateEvent pUpdateEvent);

  }

  private Method aMethod = RequestBuilder.GET;

  private String aUrl;

  private String aRootElement;

  private String aListElement;

  private String aTextElement;

  private String aValueElement;

  private String aNameSpace;

  private boolean aStarted = false;

  public RemoteListBox(final String pUrl) {
    aUrl = pUrl;
  }

  public void start() {
    if ((aRootElement == null) || (aListElement == null) || (aTextElement == null) || (aValueElement == null)) {
      throw new IllegalStateException("Can not start updating from a remote system when the proper properties for parsing the result are not set");
    }
    requestUpdate();
    aStarted = true;
  }

  public void update() {
    if (aStarted) {
      requestUpdate();
    }
  }

  public void stop() {
    aStarted = false;
  }

  private void requestUpdate() {
    final RequestBuilder rBuilder = new RequestBuilder(aMethod, getUrl());

    try {
      rBuilder.sendRequest(null, this);
    } catch (final RequestException e) {
      fireUpdateError(e);
    }
  }

  private void fireUpdateError(final Throwable pException) {
    fireEvent(new UpdateEvent(pException));
    GWT.log("Error: " + pException.getMessage(), pException);
  }

  public void setMethod(final Method method) {
    aMethod = method;
  }

  public Method getMethod() {
    return aMethod;
  }

  public void setUrl(final String url) {
    aUrl = url;
  }

  public String getUrl() {
    return aUrl;
  }

  public String getRootElement() {
    return aRootElement;
  }

  public void setRootElement(final String pRootElement) {
    aRootElement = pRootElement;
  }

  public String getListElement() {
    return aListElement;
  }

  public void setListElement(final String pListElement) {
    aListElement = pListElement;
  }

  public String getTextElement() {
    return aTextElement;
  }

  public void setTextElement(final String pTextElement) {
    aTextElement = pTextElement;
  }

  public String getValueElement() {
    return aValueElement;
  }

  public void setValueElement(final String pValueElement) {
    aValueElement = pValueElement;
  }

  public void setNameSpace(final String pNameSpace) {
    aNameSpace = pNameSpace;
  }

  public String getNameSpace() {
    return aNameSpace;
  }

  public HandlerRegistration addUpdateEventHandler(final UpdateEventHandler pHandler) {
    return addHandler(pHandler, UpdateEvent.getType());
  }

  @Override
  public void onError(final Request pRequest, final Throwable pException) {
    fireUpdateError(pException);
  }

  @Override
  public void onResponseReceived(final Request pRequest, final Response pResponse) {
    if (Response.SC_OK == pResponse.getStatusCode()) {
      updateList(asListElements(pResponse.getText()));
    } else if (pResponse.getStatusCode() >= 400) {
      fireUpdateError(new RemoteListException(pResponse.getStatusCode(), pResponse.getStatusText() + "[" + pRequest.toString() + "]"));
    }
  }

  private void updateList(final List<ListElement> pListElements) {
    final int selectedIndex = getSelectedIndex();
    final String selected = selectedIndex >= 0 ? getValue(selectedIndex) : null;
    clear();

    int newSelected = -1;
    int i = 0;
    for (final ListElement ref : pListElements) {
      addItem(ref.text, ref.value);
      if (ref.value.equals(selected)) {
        newSelected = i;
      }
      ++i;
    }
    if (newSelected >= 0) {
      setSelectedIndex(newSelected);
    }
  }

  public void update(final com.google.gwt.dom.client.Document pResults) {
    updateList(asListElements(pResults));
  }


  private List<ListElement> asListElements(final com.google.gwt.dom.client.Document myResponse) {

    final ArrayList<ListElement> result = new ArrayList<ListElement>();

    final com.google.gwt.dom.client.Node root = myResponse.getFirstChild();
    if (localName(root.getNodeName()).equals(aRootElement)) {
      com.google.gwt.dom.client.Node child = root.getFirstChild();
      while (child != null) {
        if (aListElement.equals(localName(child.getNodeName()))) {
          final String text = XMLUtil.getParamText(child, aTextElement);
          final String value = XMLUtil.getParamText(child, aValueElement);

          if ((text != null) && (value != null)) {
            result.add(new ListElement(value, text));
          }
        }
        child = child.getNextSibling();
      }
      return result;
    }

    return new ArrayList<ListElement>(0);
  }


  private String localName(final String pNodeName) {
    final int i = pNodeName.indexOf(':');
    if (i < 0) {
      return pNodeName;
    }
    return pNodeName.substring(i + 1);
  }

  private List<ListElement> asListElements(final String pText) {
    final Document myResponse;

    myResponse = XMLParser.parse(pText);
    final ArrayList<ListElement> result = new ArrayList<ListElement>();

    final Node root = myResponse.getFirstChild();
    if (localName(root.getNodeName()).equals(aRootElement)) {
      Node child = root.getFirstChild();
      while (child != null) {
        if (aListElement.equals(localName(child.getNodeName()))) {
          final String text = XMLUtil.getParamText(child, aTextElement);
          final String value = XMLUtil.getParamText(child, aValueElement);

          if ((text != null) && (value != null)) {
            result.add(new ListElement(value, text));
          }
        }
        child = child.getNextSibling();
      }
      return result;
    }

    return new ArrayList<ListElement>(0);
  }

}
