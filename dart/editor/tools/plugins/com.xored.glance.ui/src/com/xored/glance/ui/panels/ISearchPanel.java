/**
 * 
 */
package com.xored.glance.ui.panels;

import com.xored.glance.internal.ui.search.ISearchListener;
import com.xored.glance.internal.ui.search.SearchRule;

import org.eclipse.swt.widgets.Control;

/**
 * @author Yuri Strot
 */
public interface ISearchPanel extends ISearchListener {

  public static int INDEXING_STATE_DISABLE = 0;

  public static int INDEXING_STATE_INITIAL = 1;

  public static int INDEXING_STATE_IN_PROGRESS = 2;

  public static int INDEXING_STATE_FINISHED = 3;

  public void addPanelListener(ISearchPanelListener listener);

  public void clearHistory();

  public void clearStatus();

  public void closePanel();

  public void findNext();

  public void findPrevious();

  public Control getControl();

  public SearchRule getRule();

  public boolean isApplicable(Control control);

  public void newTask(String name);

  public void removePanelListener(ISearchPanelListener listener);

  public void selectAll();

  public void setEnabled(boolean enabled);

  /**
   * Set focus to search panel with some initial text
   */
  public void setFocus(String text);

  public void setIndexingState(int state);

  public void updateIndexingPercent(double percent);
}
