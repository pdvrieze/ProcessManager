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

    private final Throwable mException;

    public UpdateEvent(final Throwable exception) {
      mException = exception;
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
      return mException != null;
    }

    public Throwable getException() {
      return mException;
    }

  }

  public interface UpdateEventHandler extends EventHandler {

    void onUpdateRemoteList(UpdateEvent updateEvent);

  }

  private Method mMethod = RequestBuilder.GET;

  private String mUrl;

  private String mRootElement;

  private String mListElement;

  private String mTextElement;

  private String mValueElement;

  private String mNameSpace;

  private boolean mStarted = false;

  public RemoteListBox(final String url) {
    mUrl = url;
  }

  public void start() {
    if ((mRootElement == null) || (mListElement == null) || (mTextElement == null) || (mValueElement == null)) {
      throw new IllegalStateException("Can not start updating from a remote system when the proper properties for parsing the result are not set");
    }
    requestUpdate();
    mStarted = true;
  }

  public void update() {
    if (mStarted) {
      requestUpdate();
    }
  }

  public void stop() {
    mStarted = false;
  }

  private void requestUpdate() {
    final RequestBuilder rBuilder = new RequestBuilder(mMethod, getUrl());

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
    mMethod = method;
  }

  public Method getMethod() {
    return mMethod;
  }

  public void setUrl(final String url) {
    mUrl = url;
  }

  public String getUrl() {
    return mUrl;
  }

  public String getRootElement() {
    return mRootElement;
  }

  public void setRootElement(final String rootElement) {
    mRootElement = rootElement;
  }

  public String getListElement() {
    return mListElement;
  }

  public void setListElement(final String listElement) {
    mListElement = listElement;
  }

  public String getTextElement() {
    return mTextElement;
  }

  public void setTextElement(final String textElement) {
    mTextElement = textElement;
  }

  public String getValueElement() {
    return mValueElement;
  }

  public void setValueElement(final String valueElement) {
    mValueElement = valueElement;
  }

  public void setNameSpace(final String nameSpace) {
    mNameSpace = nameSpace;
  }

  public String getNameSpace() {
    return mNameSpace;
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
    if (localName(root.getNodeName()).equals(mRootElement)) {
      com.google.gwt.dom.client.Node child = root.getFirstChild();
      while (child != null) {
        if (mListElement.equals(localName(child.getNodeName()))) {
          final String text = XMLUtil.getParamText(child, mTextElement);
          final String value = XMLUtil.getParamText(child, mValueElement);

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
    if (localName(root.getNodeName()).equals(mRootElement)) {
      Node child = root.getFirstChild();
      while (child != null) {
        if (mListElement.equals(localName(child.getNodeName()))) {
          final String textElemContent = XMLUtil.getParamText(child, mTextElement);
          final String valueElemContent = XMLUtil.getParamText(child, mValueElement);

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
