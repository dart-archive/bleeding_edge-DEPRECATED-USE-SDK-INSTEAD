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
package com.google.dart.tools.ui.internal.search;

import com.google.dart.tools.search.ui.text.AbstractTextSearchResult;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Table;

import java.util.HashSet;
import java.util.Set;

/**
 * Table-based Dart content provider.
 */
public class DartSearchTableContentProvider extends DartSearchContentProvider {

  public DartSearchTableContentProvider(DartSearchResultPage page) {
    super(page);
  }

  @Override
  public void clear() {
    getPage().getViewer().refresh();
  }

  @Override
  public void elementsChanged(Object[] updatedElements) {
    if (getSearchResult() == null) {
      return;
    }

    int addLimit = getAddLimit();

    TableViewer viewer = (TableViewer) getPage().getViewer();
    Set<Object> updated = new HashSet<Object>();
    Set<Object> added = new HashSet<Object>();
    Set<Object> removed = new HashSet<Object>();
    for (int i = 0; i < updatedElements.length; i++) {
      if (getPage().getDisplayedMatchCount(updatedElements[i]) > 0) {
        if (viewer.testFindItem(updatedElements[i]) != null) {
          updated.add(updatedElements[i]);
        } else {
          if (addLimit > 0) {
            added.add(updatedElements[i]);
            addLimit--;
          }
        }
      } else {
        removed.add(updatedElements[i]);
      }
    }

    viewer.add(added.toArray());
    viewer.update(updated.toArray(), new String[] {SearchLabelProvider.PROPERTY_MATCH_COUNT});
    viewer.remove(removed.toArray());
  }

  @Override
  public Object[] getElements(Object inputElement) {
    if (inputElement instanceof AbstractTextSearchResult) {
      Set<Object> filteredElements = new HashSet<Object>();
      Object[] rawElements = ((AbstractTextSearchResult) inputElement).getElements();
      int limit = getPage().getElementLimit().intValue();
      for (int i = 0; i < rawElements.length; i++) {
        if (getPage().getDisplayedMatchCount(rawElements[i]) > 0) {
          filteredElements.add(rawElements[i]);
          if (limit != -1 && limit < filteredElements.size()) {
            break;
          }
        }
      }
      return filteredElements.toArray();
    }
    return EMPTY_ARRAY;
  }

  private int getAddLimit() {
    int limit = getPage().getElementLimit().intValue();
    if (limit != -1) {
      Table table = (Table) getPage().getViewer().getControl();
      int itemCount = table.getItemCount();
      if (itemCount >= limit) {
        return 0;
      }
      return limit - itemCount;
    }
    return Integer.MAX_VALUE;
  }

}
