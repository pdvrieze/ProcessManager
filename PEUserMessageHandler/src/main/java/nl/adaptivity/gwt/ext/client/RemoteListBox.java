package nl.adaptivity.gwt.ext.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.*;
import com.google.gwt.http.client.RequestBuilder.Method;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.XMLParser;

import java.util.ArrayList;
import java.util.List;


public class RemoteListBox extends ControllingListBox implements RequestCallback {


  private static class ListElement {

    final String mValue;

    final String mText;

    public ListElement(final String value, final String text) {
      mValue = value;
      mText = text;
    }

  }

  public static class UpdateEvent extends GwtEvent<UpdateEventHandler> {

    private static Type<UpdateEventHandler> TYPE;

    private final Throwable aException;

    public UpdateEvent(final Throwable exception) {
      aException = exception;
    }

    public static Type<UpdateEventHandler> getType() {
      if (TYPE == null) {
        TYPE = new Type<UpdateEventHandler>();
      }
      return TYPE;
    }

    @Override
    protected void dispatch(final UpdateEventHandler handler) {
      handler.onUpdateRemoteList(this);
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

    void onUpdateRemoteList(UpdateEvent updateEvent);

  }

  private Method aMethod = RequestBuilder.GET;

  private String aUrl;

  private String aRootElement;

  private String aListElement;

  private String aTextElement;

  private String aValueElement;

  private String aNameSpace;

  private boolean aStarted = false;

  public RemoteListBox(final String url) {
    aUrl = url;
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

  private void fireUpdateError(final Throwable exception) {
    fireEvent(new UpdateEvent(exception));
    GWT.log("Error: " + exception.getMessage(), exception);
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

  public void setRootElement(final String rootElement) {
    aRootElement = rootElement;
  }

  public String getListElement() {
    return aListElement;
  }

  public void setListElement(final String listElement) {
    aListElement = listElement;
  }

  public String getTextElement() {
    return aTextElement;
  }

  public void setTextElement(final String textElement) {
    aTextElement = textElement;
  }

  public String getValueElement() {
    return aValueElement;
  }

  public void setValueElement(final String valueElement) {
    aValueElement = valueElement;
  }

  public void setNameSpace(final String nameSpace) {
    aNameSpace = nameSpace;
  }

  public String getNameSpace() {
    return aNameSpace;
  }

  public HandlerRegistration addUpdateEventHandler(final UpdateEventHandler handler) {
    return addHandler(handler, UpdateEvent.getType());
  }

  @Override
  public void onError(final Request request, final Throwable exception) {
    fireUpdateError(exception);
  }

  @Override
  public void onResponseReceived(final Request request, final Response response) {
    if (Response.SC_OK == response.getStatusCode()) {
      updateList(asListElements(response.getText()));
    } else if (response.getStatusCode() >= 400) {
      fireUpdateError(new RemoteListException(response.getStatusCode(), response.getStatusText() + "[" + request.toString() + "]"));
    }
  }

  private void updateList(final List<ListElement> listElements) {
    final int selectedIndex = getSelectedIndex();
    final String selected = selectedIndex >= 0 ? getValue(selectedIndex) : null;
    clear();

    int newSelected = -1;
    int i = 0;
    for (final ListElement ref : listElements) {
      addItem(ref.mText, ref.mValue);
      if (ref.mValue.equals(selected)) {
        newSelected = i;
      }
      ++i;
    }
    if (newSelected >= 0) {
      setSelectedIndex(newSelected);
    }
  }

  public void update(final com.google.gwt.dom.client.Document results) {
    updateList(asListElements(results));
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


  private String localName(final String nodeName) {
    final int i = nodeName.indexOf(':');
    if (i < 0) {
      return nodeName;
    }
    return nodeName.substring(i + 1);
  }

  private List<ListElement> asListElements(final String sourceXml) {
    final Document myResponse;

    myResponse = XMLParser.parse(sourceXml);
    final ArrayList<ListElement> result = new ArrayList<ListElement>();

    final Node root = myResponse.getFirstChild();
    if (localName(root.getNodeName()).equals(aRootElement)) {
      Node child = root.getFirstChild();
      while (child != null) {
        if (aListElement.equals(localName(child.getNodeName()))) {
          final String textElemContent = XMLUtil.getParamText(child, aTextElement);
          final String valueElemContent = XMLUtil.getParamText(child, aValueElement);

          if ((textElemContent != null) && (valueElemContent != null)) {
            result.add(new ListElement(valueElemContent, textElemContent));
          }
        }
        child = child.getNextSibling();
      }
      return result;
    }

    return new ArrayList<ListElement>(0);
  }

}
