/**
 * 
 */
package com.xored.glance.ui.panels;

import com.xored.glance.internal.ui.search.SearchRule;

/**
 * @author Yuri Strot
 */
public interface ISearchPanelListener {

  public void clearStatus();

  public void close();

  public void findNext();

  public void findPrevious();

  public void indexCanceled();

  public void ruleChanged(SearchRule rule);

  public void sourceSelectionChanged();

}
