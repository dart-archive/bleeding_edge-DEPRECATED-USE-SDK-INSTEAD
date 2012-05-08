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
package com.google.dart.tools.search.internal.ui.text;

import com.google.dart.tools.search.ui.text.AbstractTextSearchResult;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;

public class FileTableContentProvider implements IStructuredContentProvider,
    IFileSearchContentProvider {

  private final Object[] EMPTY_ARR = new Object[0];

  private FileSearchPage fPage;
  private AbstractTextSearchResult fResult;

  public FileTableContentProvider(FileSearchPage page) {
    fPage = page;
  }

  public void dispose() {
    // nothing to do
  }

  public Object[] getElements(Object inputElement) {
    if (inputElement instanceof FileSearchResult) {
      int elementLimit = getElementLimit();
      Object[] elements = ((FileSearchResult) inputElement).getElements();
      if (elementLimit != -1 && elements.length > elementLimit) {
        Object[] shownElements = new Object[elementLimit];
        System.arraycopy(elements, 0, shownElements, 0, elementLimit);
        return shownElements;
      }
      return elements;
    }
    return EMPTY_ARR;
  }

  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    if (newInput instanceof FileSearchResult) {
      fResult = (FileSearchResult) newInput;
    }
  }

  public void elementsChanged(Object[] updatedElements) {
    TableViewer viewer = getViewer();
    int elementLimit = getElementLimit();
    boolean tableLimited = elementLimit != -1;
    for (int i = 0; i < updatedElements.length; i++) {
      if (fResult.getMatchCount(updatedElements[i]) > 0) {
        if (viewer.testFindItem(updatedElements[i]) != null)
          viewer.update(updatedElements[i], null);
        else {
          if (!tableLimited || viewer.getTable().getItemCount() < elementLimit)
            viewer.add(updatedElements[i]);
        }
      } else
        viewer.remove(updatedElements[i]);
    }
  }

  private int getElementLimit() {
    return fPage.getElementLimit().intValue();
  }

  private TableViewer getViewer() {
    return (TableViewer) fPage.getViewer();
  }

  public void clear() {
    getViewer().refresh();
  }
}
