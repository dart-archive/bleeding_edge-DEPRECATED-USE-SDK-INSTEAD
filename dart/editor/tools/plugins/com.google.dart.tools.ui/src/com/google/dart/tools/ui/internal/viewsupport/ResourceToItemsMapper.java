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
package com.google.dart.tools.ui.internal.viewsupport;

import org.eclipse.core.resources.IResource;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

/**
 * Helper class for updating error markers and other decorators that work on resources. Items are
 * mapped to their element's underlying resource. Method <code>resourceChanged</code> updates all
 * items that are affected from the changed elements.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ResourceToItemsMapper {

  public static interface IContentViewerAccessor {
    public void doUpdateItem(Widget item);
  }

  private static final int NUMBER_LIST_REUSE = 10;

  /**
   * Method that decides which elements can have error markers Returns null if an element can not
   * have error markers.
   * 
   * @param element The input element
   * @return Returns the corresponding resource or null
   */
  private static IResource getCorrespondingResource(Object element) {
    if (element instanceof IResource) {
      return (IResource) element;
    }
    return null;
  }

  // map from resource to item
  private HashMap fResourceToItem;

  private Stack fReuseLists;

  private IContentViewerAccessor fContentViewerAccess;

  public ResourceToItemsMapper(IContentViewerAccessor viewer) {
    fResourceToItem = new HashMap();
    fReuseLists = new Stack();

    fContentViewerAccess = viewer;
  }

  /**
   * Adds a new item to the map.
   * 
   * @param element Element to map
   * @param item The item used for the element
   */
  public void addToMap(Object element, Item item) {
    IResource resource = getCorrespondingResource(element);
    if (resource != null) {
      Object existingMapping = fResourceToItem.get(resource);
      if (existingMapping == null) {
        fResourceToItem.put(resource, item);
      } else if (existingMapping instanceof Item) {
        if (existingMapping != item) {
          List list = getNewList();
          list.add(existingMapping);
          list.add(item);
          fResourceToItem.put(resource, list);
        }
      } else { // List
        List list = (List) existingMapping;
        if (!list.contains(item)) {
          list.add(item);
        }
      }
    }
  }

  /**
   * Clears the map.
   */
  public void clearMap() {
    fResourceToItem.clear();
  }

  /**
   * Tests if the map is empty
   * 
   * @return Returns if there are mappings
   */
  public boolean isEmpty() {
    return fResourceToItem.isEmpty();
  }

  /**
   * Removes an element from the map.
   * 
   * @param element The data element
   * @param item The table or tree item
   */
  public void removeFromMap(Object element, Item item) {
    IResource resource = getCorrespondingResource(element);
    if (resource != null) {
      Object existingMapping = fResourceToItem.get(resource);
      if (existingMapping == null) {
        return;
      } else if (existingMapping instanceof Item) {
        fResourceToItem.remove(resource);
      } else { // List
        List list = (List) existingMapping;
        list.remove(item);
        if (list.isEmpty()) {
          fResourceToItem.remove(list);
          releaseList(list);
        }
      }
    }
  }

  /**
   * Must be called from the UI thread.
   * 
   * @param changedResource Changed resource
   */
  public void resourceChanged(IResource changedResource) {
    Object obj = fResourceToItem.get(changedResource);
    if (obj == null) {
      // not mapped
    } else if (obj instanceof Item) {
      updateItem((Item) obj);
    } else { // List of Items
      List list = (List) obj;
      for (int k = 0; k < list.size(); k++) {
        updateItem((Item) list.get(k));
      }
    }
  }

  private List getNewList() {
    if (!fReuseLists.isEmpty()) {
      return (List) fReuseLists.pop();
    }
    return new ArrayList(2);
  }

  private void releaseList(List list) {
    if (fReuseLists.size() < NUMBER_LIST_REUSE) {
      fReuseLists.push(list);
    }
  }

  private void updateItem(Item item) {
    if (!item.isDisposed()) {
      fContentViewerAccess.doUpdateItem(item);
    }
  }

}
