package nl.adaptivity.gwt.ext.client;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.ListBox;
import nl.adaptivity.gwt.base.client.IWidgetController;

import java.util.ArrayList;
import java.util.Collection;


/**
 * A ListBox extension that controls widget enablement based on whether an item
 * is selected.
 * 
 * @author Paul de Vrieze
 */
public class ControllingListBox extends ListBox implements ChangeHandler, IWidgetController {

  private final Collection<FocusWidget> aWidgetsToEnable;

  public ControllingListBox() {
    setVisibleItemCount(10);
    addChangeHandler(this);
    aWidgetsToEnable = new ArrayList<FocusWidget>();
  }

  @Override
  public void onChange(final ChangeEvent event) {
    final boolean enabled = getSelectedIndex() >= 0;
    for (final FocusWidget widget : aWidgetsToEnable) {
      widget.setEnabled(enabled);
    }
  }

  /*
   * (non-Javadoc)
   * @see
   * nl.adaptivity.gwt.ext.WidgetController#addControlledWidget(com.google.gwt
   * .user.client.ui.FocusWidget)
   */
  @Override
  public void addControlledWidget(final FocusWidget widget) {
    widget.setEnabled(getSelectedIndex() >= 0);
    aWidgetsToEnable.add(widget);
  }

  /*
   * (non-Javadoc)
   * @see
   * nl.adaptivity.gwt.ext.WidgetController#removeControlledWidget(com.google
   * .gwt.user.client.ui.FocusWidget)
   */
  @Override
  public boolean removeControlledWidget(final FocusWidget widget) {
    return aWidgetsToEnable.remove(widget);
  }

  @Override
  public void setSelectedIndex(final int index) {
    super.setSelectedIndex(index);
    final boolean enabled = index >= 0;
    for (final FocusWidget widget : aWidgetsToEnable) {
      widget.setEnabled(enabled);
    }
  }

}
