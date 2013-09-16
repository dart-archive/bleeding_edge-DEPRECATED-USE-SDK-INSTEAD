/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.common.ui.internal.search;

import com.google.dart.tools.search.ui.text.AbstractTextSearchResult;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.wst.common.ui.internal.search.basecode.IFileSearchContentProvider;

public class SearchResultTableContentProvider implements IStructuredContentProvider,
    IFileSearchContentProvider {

  private final Object[] EMPTY_ARR = new Object[0];

  private SearchResultPage fPage;
  private AbstractTextSearchResult fResult;

  public SearchResultTableContentProvider(SearchResultPage page) {
    fPage = page;
  }

  public void dispose() {
    // nothing to do
  }

  public Object[] getElements(Object inputElement) {
    if (inputElement instanceof SearchResult) {
      Object[] elements = ((SearchResult) inputElement).getElements();
      // cs : I've comment this code out for now.  We need to review the changes to the base class and react accordingly.
      // Even more importantly we need to discuss with the base guys to get API lined up for this (see bug 163177).
      /*
       * int tableLimit= SearchPreferencePage.getTableLimit(); if
       * (SearchPreferencePage.isTableLimited() && elements.length > tableLimit) { Object[]
       * shownElements= new Object[tableLimit]; System.arraycopy(elements, 0, shownElements, 0,
       * tableLimit); return shownElements; }
       */
      return elements;
    }
    return EMPTY_ARR;
  }

  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    if (newInput instanceof SearchResult) {
      fResult = (SearchResult) newInput;
    }
  }

  public void elementsChanged(Object[] updatedElements) {
    TableViewer viewer = getViewer();
    // cs : I've comment 'tableLimited' related code out for now.  We need to review the changes to the base class and react accordingly.
    // Even more importantly we need to discuss with the base guys to get API lined up for this (see bug 163177).
    //boolean tableLimited= SearchPreferencePage.isTableLimited();
    for (int i = 0; i < updatedElements.length; i++) {
      if (fResult.getMatchCount(updatedElements[i]) > 0) {
        if (viewer.testFindItem(updatedElements[i]) != null)
          viewer.update(updatedElements[i], null);
        else {
          //if (!tableLimited || viewer.getTable().getItemCount() < SearchPreferencePage.getTableLimit())
          viewer.add(updatedElements[i]);
        }
      } else
        viewer.remove(updatedElements[i]);
    }
  }

  private TableViewer getViewer() {
    return (TableViewer) fPage.getViewer();
  }

  public void clear() {
    getViewer().refresh();
  }
}
