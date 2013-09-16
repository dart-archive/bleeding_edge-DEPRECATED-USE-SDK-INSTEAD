/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Derived from org.eclipse.search.internal.ui.SortAction
 *******************************************************************************/
package org.eclipse.wst.common.ui.internal.search.basecode;

import org.eclipse.jface.action.Action;
import org.eclipse.wst.common.ui.internal.search.SearchResultPage;

public class SortAction extends Action {
  private int fSortOrder;
  private SearchResultPage fPage;

  public SortAction(String label, SearchResultPage page, int sortOrder) {
    super(label);
    fPage = page;
    fSortOrder = sortOrder;
  }

  public void run() {
    fPage.setSortOrder(fSortOrder);
  }

  public int getSortOrder() {
    return fSortOrder;
  }
}
