/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.viewsupport;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * filter with a live cycle
 */
public abstract class DartViewerFilter extends ViewerFilter {

  private int fCount = 0;

  /*
   * Overrides method from ViewerFilter
   */
  @Override
  public Object[] filter(Viewer viewer, Object parent, Object[] elements) {
    try {
      filteringStart();
      return super.filter(viewer, parent, elements);
    } finally {
      filteringEnd();
    }
  }

  public final void filteringEnd() {
    fCount--;
    if (fCount == 0) {
      freeFilter();
    }
  }

  public final void filteringStart() {
    if (fCount == 0) {
      initFilter();
    }
    fCount++;
  }

  protected abstract void freeFilter();

  /**
   * To be overridden by implement
   */
  protected abstract void initFilter();

}
