/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.common.ui.internal.search;

import com.google.dart.tools.search.ui.text.AbstractTextSearchResult;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.wst.common.ui.internal.search.basecode.IFileSearchContentProvider;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SearchResultTreeContentProvider implements ITreeContentProvider,
    IFileSearchContentProvider {

  private final Object[] EMPTY_ARR = new Object[0];

  private AbstractTextSearchResult fResult;
  private AbstractTreeViewer fTreeViewer;
  private Map fChildrenMap;

  SearchResultTreeContentProvider(AbstractTreeViewer viewer) {
    fTreeViewer = viewer;
  }

  public Object[] getElements(Object inputElement) {
    return getChildren(inputElement);
  }

  public void dispose() {
    // nothing to do
  }

  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    if (newInput instanceof SearchResult) {
      initialize((SearchResult) newInput);
    }
  }

  protected synchronized void initialize(AbstractTextSearchResult result) {
    fResult = result;
    fChildrenMap = new HashMap();
    if (result != null) {
      Object[] elements = result.getElements();
      for (int i = 0; i < elements.length; i++) {
        insert(elements[i], false);
      }
    }
  }

  protected void insert(Object child, boolean refreshViewer) {
    Object parent = getParent(child);
    while (parent != null) {
      if (insertChild(parent, child)) {
        if (refreshViewer)
          fTreeViewer.add(parent, child);
      } else {
        if (refreshViewer)
          fTreeViewer.refresh(parent);
        return;
      }
      child = parent;
      parent = getParent(child);
    }
    if (insertChild(fResult, child)) {
      if (refreshViewer)
        fTreeViewer.add(fResult, child);
    }
  }

  /**
   * returns true if the child already was a child of parent.
   * 
   * @param parent
   * @param child
   * @return Returns <code>trye</code> if the child was added
   */
  private boolean insertChild(Object parent, Object child) {
    Set children = (Set) fChildrenMap.get(parent);
    if (children == null) {
      children = new HashSet();
      fChildrenMap.put(parent, children);
    }
    return children.add(child);
  }

  protected void remove(Object element, boolean refreshViewer) {
    // precondition here:  fResult.getMatchCount(child) <= 0

    if (hasChildren(element)) {
      if (refreshViewer)
        fTreeViewer.refresh(element);
    } else {
      if (fResult.getMatchCount(element) == 0) {
        fChildrenMap.remove(element);
        Object parent = getParent(element);
        if (parent != null) {
          removeFromSiblings(element, parent);
          remove(parent, refreshViewer);
        } else {
          removeFromSiblings(element, fResult);
          if (refreshViewer)
            fTreeViewer.refresh();
        }
      } else {
        if (refreshViewer) {
          fTreeViewer.refresh(element);
        }
      }
    }
  }

  private void removeFromSiblings(Object element, Object parent) {
    Set siblings = (Set) fChildrenMap.get(parent);
    if (siblings != null) {
      siblings.remove(element);
    }
  }

  public Object[] getChildren(Object parentElement) {
    Set children = (Set) fChildrenMap.get(parentElement);
    if (children == null)
      return EMPTY_ARR;
    return children.toArray();
  }

  public boolean hasChildren(Object element) {
    return getChildren(element).length > 0;
  }

  public synchronized void elementsChanged(Object[] updatedElements) {
    for (int i = 0; i < updatedElements.length; i++) {
      if (fResult.getMatchCount(updatedElements[i]) > 0)
        insert(updatedElements[i], true);
      else
        remove(updatedElements[i], true);
    }
  }

  public void clear() {
    initialize(fResult);
    fTreeViewer.refresh();
  }

  public Object getParent(Object element) {
    if (element instanceof IProject)
      return null;
    if (element instanceof IResource) {
      IResource resource = (IResource) element;
      return resource.getParent();
    }
    return null;
  }
}
