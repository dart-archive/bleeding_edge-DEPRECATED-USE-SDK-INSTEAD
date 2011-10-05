/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.cache;

import com.google.dart.tools.core.internal.model.OpenableElementImpl;
import com.google.dart.tools.core.internal.model.info.DartElementInfo;
import com.google.dart.tools.core.internal.model.info.OpenableElementInfo;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.OpenableElement;

/**
 * Instances of the class <code>ElementCache</code> implement an LRU cache of
 * <code>DartElements</code>.
 */
public class ElementCache extends OverflowingLRUCache<OpenableElement, OpenableElementInfo> {
  private DartElement spaceLimitParent = null;

  /**
   * Initialize a newly created cache.
   * 
   * @param size the size limit of the cache
   */
  public ElementCache(int size) {
    super(size);
  }

  /**
   * Initialize a newly created cache.
   * 
   * @param size the size limit of the cache
   * @param overflow the size of the overflow
   */
  public ElementCache(int size, int overflow) {
    super(size, overflow);
  }

  /*
   * Ensures that there is enough room for adding the children of the given info. If the space limit
   * must be increased, record the parent that needed this space limit.
   */
  public void ensureSpaceLimit(Object info, DartElement parent) {
    // ensure the children can be put without closing other elements
    int childrenSize = ((DartElementInfo) info).getChildren().length;
    int spaceNeeded = 1 + (int) ((1 + this.loadFactor) * (childrenSize + this.overflow));
    if (this.spaceLimit < spaceNeeded) {
      // parent is being opened with more children than the space limit
      shrink(); // remove overflow
      setSpaceLimit(spaceNeeded);
      this.spaceLimitParent = parent;
    }
  }

  /*
   * If the given parent was the one that increased the space limit, reset the space limit to the
   * given default value.
   */
  public void resetSpaceLimit(int defaultLimit, DartElement parent) {
    if (parent.equals(this.spaceLimitParent)) {
      setSpaceLimit(defaultLimit);
      this.spaceLimitParent = null;
    }
  }

  /**
   * Return <code>true</code> if the element is successfully closed and removed from the cache.
   * <p>
   * NOTE: this triggers an external remove from the cache by closing the object.
   */
  @Override
  protected boolean close(LRUCacheEntry<OpenableElement, OpenableElementInfo> entry) {
    OpenableElementImpl element = (OpenableElementImpl) entry.key;
    try {
      if (!element.canBeRemovedFromCache()) {
        return false;
      } else {
        element.close();
        return true;
      }
    } catch (DartModelException exception) {
      return false;
    }
  }

  /*
   * Return a new instance of the receiver.
   */
  @Override
  protected LRUCache<OpenableElement, OpenableElementInfo> newInstance(int size, int newOverflow) {
    return new ElementCache(size, newOverflow);
  }
}
