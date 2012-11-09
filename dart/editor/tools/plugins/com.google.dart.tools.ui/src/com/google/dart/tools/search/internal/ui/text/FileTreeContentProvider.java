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

import com.google.dart.tools.search.ui.ISearchQuery;
import com.google.dart.tools.search.ui.text.AbstractTextSearchResult;
import com.google.dart.tools.search.ui.text.FileTextSearchScope;
import com.google.dart.tools.search.ui.text.Match;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("restriction")
public class FileTreeContentProvider implements ITreeContentProvider, IFileSearchContentProvider {

  private final Object[] EMPTY_ARR = new Object[0];

  private AbstractTextSearchResult fResult;
  private FileSearchPage fPage;
  private AbstractTreeViewer fTreeViewer;
  private Map<Object, Set<Object>> fChildrenMap;

  private ArrayList<File> externalRoots;

  FileTreeContentProvider(FileSearchPage page, AbstractTreeViewer viewer) {
    fPage = page;
    fTreeViewer = viewer;
  }

  @Override
  public void clear() {
    initialize(fResult);
    fTreeViewer.refresh();
  }

  @Override
  public void dispose() {
    // nothing to do
  }

  @Override
  public synchronized void elementsChanged(Object[] updatedElements) {
    for (int i = 0; i < updatedElements.length; i++) {
      if (!(updatedElements[i] instanceof LineElement)) {
        // change events to elements are reported in file search
        if (fResult.getMatchCount(updatedElements[i]) > 0) {
          insert(updatedElements[i], true);
        } else {
          remove(updatedElements[i], true);
        }
      } else {
        // change events to line elements are reported in text search
        LineElement lineElement = (LineElement) updatedElements[i];
        int nMatches = lineElement.getNumberOfMatches(fResult);
        if (nMatches > 0) {
          if (hasChild(lineElement.getParent(), lineElement)) {
            fTreeViewer.update(new Object[] {lineElement, lineElement.getParent()}, null);
          } else {
            insert(lineElement, true);
          }
        } else {
          remove(lineElement, true);
        }
      }
    }
  }

  @Override
  public Object[] getChildren(Object parentElement) {
    Set<Object> children = fChildrenMap.get(parentElement);
    if (children == null) {
      return EMPTY_ARR;
    }
    return children.toArray();
  }

  @Override
  public Object[] getElements(Object inputElement) {
    Object[] children = getChildren(inputElement);
    int elementLimit = getElementLimit();
    if (elementLimit != -1 && elementLimit < children.length) {
      Object[] limitedChildren = new Object[elementLimit];
      System.arraycopy(children, 0, limitedChildren, 0, elementLimit);
      return limitedChildren;
    }
    return children;
  }

  @Override
  public Object getParent(Object element) {
    if (element instanceof IProject) {
      return null;
    }
    if (element instanceof File) {
      File file = (File) element;
      if (file.isDirectory()) {
        return null;
      }
      return getExternalRoot(file);
    }

    if (element instanceof IResource) {
      IResource resource = (IResource) element;
      return resource.getParent();
    }

    if (element instanceof LineElement) {
      return ((LineElement) element).getParent();
    }

    if (element instanceof FileMatch) {
      FileMatch match = (FileMatch) element;
      return match.getLineElement();
    }

    if (element instanceof FileResource<?>) {
      return ((FileResource<?>) element).getResource();
    }

    return null;
  }

  @Override
  public boolean hasChildren(Object element) {
    return getChildren(element).length > 0;
  }

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    if (newInput instanceof FileSearchResult) {
      initialize((FileSearchResult) newInput);
    }
  }

  private int getElementLimit() {
    return fPage.getElementLimit().intValue();
  }

  private Object getExternalRoot(File file) {
    while (file != null) {
      file = file.getParentFile();
      if (externalRoots.contains(file)) {
        break;
      }
    }
    return file;
  }

  private boolean hasChild(Object parent, Object child) {
    Set<Object> children = fChildrenMap.get(parent);
    return children != null && children.contains(child);
  }

  private boolean hasMatches(Object element) {
    if (element instanceof LineElement) {
      LineElement lineElement = (LineElement) element;
      return lineElement.getNumberOfMatches(fResult) > 0;
    }
    return fResult.getMatchCount(element) > 0;
  }

  private synchronized void initialize(AbstractTextSearchResult result) {
    fResult = result;
    fChildrenMap = new HashMap<Object, Set<Object>>();

    initializeExternalRoots(result);

    boolean showLineMatches = showLineMatches();

    if (result != null) {
      Object[] elements = result.getElements();
      for (int i = 0; i < elements.length; i++) {
        if (showLineMatches) {
          Match[] matches = result.getMatches(elements[i]);
          for (int j = 0; j < matches.length; j++) {
            insert(((FileResourceMatch) matches[j]).getLineElement(), false);
          }
        } else {
          insert(elements[i], false);
        }
      }
    }

  }

  private void initializeExternalRoots(AbstractTextSearchResult result) {

    externalRoots = new ArrayList<File>();

    ISearchQuery query = result.getQuery();
    if (query instanceof FileSearchQuery) {
      FileSearchQuery fileQuery = (FileSearchQuery) query;
      FileTextSearchScope searchScope = fileQuery.getSearchScope();
      for (File root : searchScope.getExternalRoots()) {
        externalRoots.add(root);
      }
    }
  }

  private void insert(Object child, boolean refreshViewer) {
    Object parent = getParent(child);
    while (parent != null) {
      if (insertChild(parent, child)) {
        if (refreshViewer) {
          fTreeViewer.add(parent, child);
        }
      } else {
        if (refreshViewer) {
          fTreeViewer.refresh(parent);
        }
        return;
      }
      child = parent;
      parent = getParent(child);
    }
    if (insertChild(fResult, child)) {
      if (refreshViewer) {
        fTreeViewer.add(fResult, child);
      }
    }
  }

  /**
   * Adds the child to the parent.
   * 
   * @param parent the parent
   * @param child the child
   * @return <code>true</code> if this set did not already contain the specified element
   */
  private boolean insertChild(Object parent, Object child) {
    Set<Object> children = fChildrenMap.get(parent);
    if (children == null) {
      children = new HashSet<Object>();
      fChildrenMap.put(parent, children);
    }
    return children.add(child);
  }

  private void remove(Object element, boolean refreshViewer) {
    // precondition here:  fResult.getMatchCount(child) <= 0

    if (hasChildren(element)) {
      if (refreshViewer) {
        fTreeViewer.refresh(element);
      }
    } else {
      if (!hasMatches(element)) {
        fChildrenMap.remove(element);
        Object parent = getParent(element);
        if (parent != null) {
          removeFromSiblings(element, parent);
          remove(parent, refreshViewer);
        } else {
          removeFromSiblings(element, fResult);
          if (refreshViewer) {
            fTreeViewer.refresh();
          }
        }
      } else {
        if (refreshViewer) {
          fTreeViewer.refresh(element);
        }
      }
    }
  }

  private void removeFromSiblings(Object element, Object parent) {
    Set<Object> siblings = fChildrenMap.get(parent);
    if (siblings != null) {
      siblings.remove(element);
    }
  }

  private boolean showLineMatches() {
    //TODO(pquitslund): line matches are not shown (by design)
    return false;
//    return !((FileSearchQuery) fResult.getQuery()).isFileNameSearch();
  }
}
