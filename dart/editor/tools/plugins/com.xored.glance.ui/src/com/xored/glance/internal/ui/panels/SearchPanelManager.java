/**
 * 
 */
package com.xored.glance.internal.ui.panels;

import com.xored.glance.internal.ui.GlancePlugin;
import com.xored.glance.internal.ui.preferences.IPreferenceConstants;
import com.xored.glance.ui.panels.ISearchPanel;

import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * @author Yuri Strot
 */
public class SearchPanelManager {

  private static SearchPanelManager instance;

  public static SearchPanelManager getInstance() {
    if (instance == null) {
      instance = new SearchPanelManager();
    }
    return instance;
  }

  private SearchPanelManager() {
  }

  public ISearchPanel getPanel(Control control) {
    if (showStatusLine()) {
      IWorkbenchWindow window = SearchStatusLine.getWindow(control);
      if (window != null) {
        return SearchStatusLine.getSearchLine(window);
      }
    }
    PopupSearchDialog dialog = new PopupSearchDialog(control);
    dialog.open();
    return dialog;
  }

  private boolean showStatusLine() {
    return GlancePlugin.getDefault().getPreferenceStore().getBoolean(
        IPreferenceConstants.PANEL_STATUS_LINE);
  }

}
