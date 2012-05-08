/*
 * Copyright (c) 2012, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.search2.internal.ui;

import com.google.dart.tools.search.ui.text.AbstractTextSearchResult;
import com.google.dart.tools.search.ui.text.AbstractTextSearchViewPage;
import com.google.dart.tools.search.ui.text.MatchFilter;

import java.util.ArrayList;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;

import org.eclipse.ui.texteditor.IUpdate;

public class MatchFilterAction extends Action implements IUpdate {

  private MatchFilter fFilter;
  private AbstractTextSearchViewPage fPage;

  public MatchFilterAction(AbstractTextSearchViewPage page, MatchFilter filter) {
    super(filter.getActionLabel(), IAction.AS_CHECK_BOX);
    fPage = page;
    fFilter = filter;
    setId("MatchFilterAction." + filter.getID()); //$NON-NLS-1$
    setChecked(isActiveMatchFilter());
  }

  public void run() {
    AbstractTextSearchResult input = fPage.getInput();
    if (input == null) {
      return;
    }
    ArrayList<MatchFilter> newFilters = new ArrayList<MatchFilter>();
    MatchFilter[] activeMatchFilters = input.getActiveMatchFilters();
    if (activeMatchFilters == null) {
      return;
    }

    for (int i = 0; i < activeMatchFilters.length; i++) {
      if (!activeMatchFilters[i].equals(fFilter)) {
        newFilters.add(activeMatchFilters[i]);
      }
    }
    boolean newState = isChecked();
    if (newState) {
      newFilters.add(fFilter);
    }
    input.setActiveMatchFilters(newFilters.toArray(new MatchFilter[newFilters.size()]));
  }

  public MatchFilter getFilter() {
    return fFilter;
  }

  private boolean isActiveMatchFilter() {
    AbstractTextSearchResult input = fPage.getInput();
    if (input != null) {
      MatchFilter[] activeMatchFilters = input.getActiveMatchFilters();
      for (int i = 0; i < activeMatchFilters.length; i++) {
        if (fFilter.equals(activeMatchFilters[i])) {
          return true;
        }
      }
    }
    return false;
  }

  public void update() {
    setChecked(isActiveMatchFilter());
  }
}
