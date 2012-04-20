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

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.search.ui.text.AbstractTextSearchResult;
import com.google.dart.tools.ui.StandardDartElementContentProvider;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Tree-based Dart content provider.
 */
public class LevelTreeContentProvider extends DartSearchContentProvider implements
    ITreeContentProvider {

  static class FastDartElementProvider extends StandardDartElementContentProvider {
    @Override
    public Object getParent(Object element) {
      Object parent = internalGetParent(element);
      if (parent == null && element instanceof IAdaptable) {
        IAdaptable adaptable = (IAdaptable) element;
        Object dartElement = adaptable.getAdapter(DartElement.class);
        if (dartElement != null) {
          parent = internalGetParent(dartElement);
        } else {
          Object resource = adaptable.getAdapter(IResource.class);
          if (resource != null) {
            parent = internalGetParent(resource);
          }
        }
      }
      return parent;
    }
  }

  public static final int LEVEL_FILE = 2;

  public static final int LEVEL_PACKAGE = 3;
  public static final int LEVEL_PROJECT = 4;
  public static final int LEVEL_TYPE = 1;
  private static final int[][] DART_ELEMENT_TYPES = {
      {DartElement.TYPE}, {DartElement.LIBRARY}, {DartElement.DART_PROJECT, DartElement.LIBRARY},
      {DartElement.DART_MODEL}};

  private static final int MAX_LEVEL = DART_ELEMENT_TYPES.length - 1;
  private static final int[][] RESOURCE_TYPES = {
      {}, {IResource.FILE}, {IResource.FOLDER}, {IResource.PROJECT}, {IResource.ROOT}};

  private Map<Object, Set<Object>> childrenMap;
  private StandardDartElementContentProvider contentProvider;
  private int currentLevel;

  public LevelTreeContentProvider(DartSearchResultPage page, int level) {
    super(page);
    currentLevel = level;
    contentProvider = new FastDartElementProvider();
  }

  @Override
  public void clear() {
    initialize(getSearchResult());
    getPage().getViewer().refresh();
  }

  @Override
  public synchronized void elementsChanged(Object[] updatedElements) {
    if (getSearchResult() == null) {
      return;
    }

    AbstractTreeViewer viewer = (AbstractTreeViewer) getPage().getViewer();

    Set<Object> toRemove = new HashSet<Object>();
    Set<Object> toUpdate = new HashSet<Object>();
    Map<Object, Set<Object>> toAdd = new HashMap<Object, Set<Object>>();
    for (int i = 0; i < updatedElements.length; i++) {
      if (getPage().getDisplayedMatchCount(updatedElements[i]) > 0) {
        insert(toAdd, toUpdate, updatedElements[i]);
      } else {
        remove(toRemove, toUpdate, updatedElements[i]);
      }
    }

    viewer.remove(toRemove.toArray());
    for (Iterator<Object> iter = toAdd.keySet().iterator(); iter.hasNext();) {
      Object parent = iter.next();
      Set<Object> children = toAdd.get(parent);
      viewer.add(parent, children.toArray());
    }
    for (Iterator<Object> elementsToUpdate = toUpdate.iterator(); elementsToUpdate.hasNext();) {
      viewer.refresh(elementsToUpdate.next());
    }

  }

  @Override
  public Object[] getChildren(Object parentElement) {
    Set<Object> children = childrenMap.get(parentElement);
    if (children == null) {
      return EMPTY_ARRAY;
    }
    int limit = getPage().getElementLimit().intValue();
    if (limit != -1 && limit < children.size()) {
      Object[] limitedArray = new Object[limit];
      Iterator<Object> iterator = children.iterator();
      for (int i = 0; i < limit; i++) {
        limitedArray[i] = iterator.next();
      }
      return limitedArray;
    }

    return children.toArray();
  }

  @Override
  public Object[] getElements(Object inputElement) {
    return getChildren(inputElement);
  }

  @Override
  public Object getParent(Object child) {
    Object possibleParent = internalGetParent(child);
    if (possibleParent instanceof DartElement) {
      DartElement dartElement = (DartElement) possibleParent;
      for (int j = currentLevel; j < MAX_LEVEL + 1; j++) {
        for (int i = 0; i < DART_ELEMENT_TYPES[j].length; i++) {
          if (dartElement.getElementType() == DART_ELEMENT_TYPES[j][i]) {
            return null;
          }
        }
      }
    } else if (possibleParent instanceof IResource) {
      IResource resource = (IResource) possibleParent;
      for (int j = currentLevel; j < MAX_LEVEL + 1; j++) {
        for (int i = 0; i < RESOURCE_TYPES[j].length; i++) {
          if (resource.getType() == RESOURCE_TYPES[j][i]) {
            return null;
          }
        }
      }
    }
    if (currentLevel != LEVEL_FILE && child instanceof Type) {
      Type type = (Type) child;
      if (possibleParent instanceof CompilationUnit) {
        possibleParent = type.getLibrary();
      }
    }
    return possibleParent;
  }

  @Override
  public boolean hasChildren(Object element) {
    Set<Object> children = childrenMap.get(element);
    return children != null && !children.isEmpty();
  }

  public void setLevel(int level) {
    currentLevel = level;
    initialize(getSearchResult());
    getPage().getViewer().refresh();
  }

  @Override
  protected synchronized void initialize(AbstractTextSearchResult result) {
    super.initialize(result);
    childrenMap = new HashMap<Object, Set<Object>>();
    if (result != null) {
      Object[] elements = result.getElements();
      for (int i = 0; i < elements.length; i++) {
        if (getPage().getDisplayedMatchCount(elements[i]) > 0) {
          insert(null, null, elements[i]);
        }
      }
    }
  }

  protected void insert(Map<Object, Set<Object>> toAdd, Set<Object> toUpdate, Object child) {
    Object parent = getParent(child);
    while (parent != null) {
      if (insertChild(parent, child)) {
        if (toAdd != null) {
          insertInto(parent, child, toAdd);
        }
      } else {
        if (toUpdate != null) {
          toUpdate.add(parent);
        }
        return;
      }
      child = parent;
      parent = getParent(child);
    }
    if (insertChild(getSearchResult(), child)) {
      if (toAdd != null) {
        insertInto(getSearchResult(), child, toAdd);
      }
    }
  }

  protected void remove(Set<Object> toRemove, Set<Object> toUpdate, Object element) {
    // precondition here:  fResult.getMatchCount(child) <= 0

    if (hasChildren(element)) {
      if (toUpdate != null) {
        toUpdate.add(element);
      }
    } else {
      if (getPage().getDisplayedMatchCount(element) == 0) {
        childrenMap.remove(element);
        Object parent = getParent(element);
        if (parent != null) {
          if (removeFromSiblings(element, parent)) {
            remove(toRemove, toUpdate, parent);
          }
        } else {
          if (removeFromSiblings(element, getSearchResult())) {
            if (toRemove != null) {
              toRemove.add(element);
            }
          }
        }
      } else {
        if (toUpdate != null) {
          toUpdate.add(element);
        }
      }
    }
  }

  private boolean insertChild(Object parent, Object child) {
    return insertInto(parent, child, childrenMap);
  }

  private boolean insertInto(Object parent, Object child, Map<Object, Set<Object>> map) {
    Set<Object> children = map.get(parent);
    if (children == null) {
      children = new HashSet<Object>();
      map.put(parent, children);
    }
    return children.add(child);
  }

  private Object internalGetParent(Object child) {
    return contentProvider.getParent(child);
  }

  /**
   * Tries to remove the given element from the list of stored siblings.
   * 
   * @param element potential child
   * @param parent potential parent
   * @return returns true if it really was a remove (i.e. element was a child of parent).
   */
  private boolean removeFromSiblings(Object element, Object parent) {
    Set<Object> siblings = childrenMap.get(parent);
    if (siblings != null) {
      return siblings.remove(element);
    } else {
      return false;
    }
  }

}
