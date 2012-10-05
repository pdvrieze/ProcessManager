package nl.adaptivity.gwt.base.client;

import java.util.ArrayList;
import java.util.Collection;

import nl.adaptivity.gwt.base.client.MyFormPanel.ResetEvent;
import nl.adaptivity.gwt.base.client.MyFormPanel.ResetHandler;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.Widget;


/**
 * An extended upload that can have a change handler.
 * 
 * @author Paul de Vrieze
 */
public class MyFileUpload extends FileUpload implements IWidgetController, ChangeHandler, ResetHandler {

  private Collection<FocusWidget> aWidgetsToEnable;

  private HandlerRegistration aResetHandler;

  private HandlerRegistration aChangeHandler;

  public MyFileUpload() {
    super();
  }

  private HandlerRegistration registerResetHandler() {
    Widget parent = getParent();
    while ((parent != null) && (!((parent instanceof MyFormPanel) || (parent instanceof FormPanel)))) {
      parent = parent.getParent();
    }
    if (parent != null) {
      if (parent instanceof MyFormPanel) {
        final MyFormPanel form = (MyFormPanel) parent;
        return form.addResetHandler(this);

      } else if (parent instanceof FormPanel) {
        // Figure out if we can do something in this case
        //        FormPanel form = (FormPanel) parent;

      }
    }
    return null;
  }

  public MyFileUpload(final Element pElement) {
    super(pElement);
  }

  public HandlerRegistration registerChangeHandler(final ChangeHandler pHandler) {
    return addDomHandler(pHandler, ChangeEvent.getType());
  }

  @Override
  public void addControlledWidget(final FocusWidget pWidget) {
    if (aWidgetsToEnable == null) {
      aWidgetsToEnable = new ArrayList<FocusWidget>();
      aChangeHandler = registerChangeHandler(this);
      aResetHandler = registerResetHandler();
    }
    aWidgetsToEnable.add(pWidget);
    pWidget.setEnabled(getFilename().length() > 0);
  }

  @Override
  public boolean removeControlledWidget(final FocusWidget pWidget) {
    final boolean result = aWidgetsToEnable.remove(pWidget);
    if (result) {
      if (aWidgetsToEnable.size() == 0) {
        aWidgetsToEnable = null;
        aResetHandler.removeHandler();
        aChangeHandler.removeHandler();
      }
    }
    return result;
  }

  @Override
  public void onChange(final ChangeEvent pEvent) {
    refreshEnablement();
  }

  @Override
  public void onReset(final ResetEvent pResetEvent) {

    refreshEnablement();
  }

  private void refreshEnablement() {
    final boolean enabled = getFilename().length() > 0;
    for (final FocusWidget widget : aWidgetsToEnable) {
      widget.setEnabled(enabled);
    }
  }

}
