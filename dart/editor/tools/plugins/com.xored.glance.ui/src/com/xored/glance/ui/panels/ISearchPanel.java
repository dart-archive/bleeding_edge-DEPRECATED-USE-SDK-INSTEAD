/**
 * 
 */
package com.xored.glance.ui.panels;

import org.eclipse.swt.widgets.Control;

import com.xored.glance.internal.ui.search.ISearchListener;
import com.xored.glance.internal.ui.search.SearchRule;

/**
 * @author Yuri Strot
 */
public interface ISearchPanel extends ISearchListener {

  public static int INDEXING_STATE_DISABLE = 0;

  public static int INDEXING_STATE_INITIAL = 1;

  public static int INDEXING_STATE_IN_PROGRESS = 2;

  public static int INDEXING_STATE_FINISHED = 3;

  public void addPanelListener(ISearchPanelListener listener);

  public void removePanelListener(ISearchPanelListener listener);

  public void setEnabled(boolean enabled);

  public boolean isApplicable(Control control);

  public Control getControl();

  public void setIndexingState(int state);

  public void updateIndexingPercent(double percent);

  public void newTask(String name);

  /**
   * Set focus to search panel with some initial text
   */
  public void setFocus(String text);

  public SearchRule getRule();

  public void closePanel();

  public void findNext();

  public void findPrevious();

  public void clearHistory();
}
