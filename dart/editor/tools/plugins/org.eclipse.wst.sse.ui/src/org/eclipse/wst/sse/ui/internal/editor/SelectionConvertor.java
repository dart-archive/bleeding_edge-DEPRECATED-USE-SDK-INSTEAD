/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/

package org.eclipse.wst.sse.ui.internal.editor;

import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;

import java.util.ArrayList;
import java.util.List;

/**
 * This class/interface is part of an experimental API that is still under development and expected
 * to change significantly before reaching stability. It is being made available at this early stage
 * to solicit feedback from pioneering adopters on the understanding that any code that uses this
 * API will almost certainly be broken (repeatedly) as the API evolves.
 */
public class SelectionConvertor {
  /**
   * @param model
   * @param start
   * @param end
   * @return the most specific mapping of this text selection to implementors of IndexedRegion
   */
  public Object[] getElements(IStructuredModel model, int start, int end) {
    Object[] localSelectedStructures = null;
    if (model != null) {
      IndexedRegion region = model.getIndexedRegion(start);
      if (region != null) {
        if (end <= region.getEndOffset()) {
          // single selection
          localSelectedStructures = new Object[1];
          localSelectedStructures[0] = region;
        } else {
          // multiple selection
          int maxLength = model.getStructuredDocument().getLength();
          List structures = new ArrayList(2);
          while (region != null && region.getEndOffset() <= end
              && region.getEndOffset() < maxLength) {
            structures.add(region);
            region = model.getIndexedRegion(region.getEndOffset() + 1);
          }
          localSelectedStructures = structures.toArray();
        }
      }
    }
    if (localSelectedStructures == null) {
      localSelectedStructures = new Object[0];
    }
    return localSelectedStructures;
  }
}
