package nl.adaptivity.gwt.ext.client;

import java.util.ArrayList;
import java.util.Collection;

import nl.adaptivity.gwt.base.client.IWidgetController;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.ListBox;

/**
 * A ListBox extension that controls widget enablement based on whether an item is selected.
 * @author Paul de Vrieze
 *
 */
public class ControllingListBox extends ListBox implements ChangeHandler, IWidgetController {
  
  private Collection<FocusWidget> aWidgetsToEnable;

  public ControllingListBox() {
    setVisibleItemCount(10);
    addChangeHandler(this);
    aWidgetsToEnable = new ArrayList<FocusWidget>();
  }

  @Override
  public void onChange(ChangeEvent pEvent) {
    boolean enabled = getSelectedIndex()>=0; 
    for (FocusWidget widget: aWidgetsToEnable) {
      widget.setEnabled(enabled);
    }
  }
  
  /* (non-Javadoc)
   * @see nl.adaptivity.gwt.ext.WidgetController#addControlledWidget(com.google.gwt.user.client.ui.FocusWidget)
   */
  public void addControlledWidget(FocusWidget pWidget) {
    pWidget.setEnabled(getSelectedIndex()>=0);
    aWidgetsToEnable.add(pWidget);
  }
  
  /* (non-Javadoc)
   * @see nl.adaptivity.gwt.ext.WidgetController#removeControlledWidget(com.google.gwt.user.client.ui.FocusWidget)
   */
  public boolean removeControlledWidget(FocusWidget pWidget) {
    return aWidgetsToEnable.remove(pWidget);
  }

}
