/**
 * 
 */
package com.xored.glance.ui.panels;

import com.xored.glance.internal.ui.search.SearchRule;

/**
 * @author Yuri Strot
 */
public interface ISearchPanelListener {

  public void ruleChanged(SearchRule rule);

  public void findNext();

  public void findPrevious();

  public void close();

  public void indexCanceled();

}
