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
package com.google.dart.tools.ui;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

/**
 * Sorter for JavaScript elements. Ordered by element category, then by element name. Package
 * fragment roots are sorted as ordered on the classpath.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public class DartElementSorter extends ViewerSorter {

  private final DartElementComparator comparator;

  /**
   * Constructor.
   */
  public DartElementSorter() {
    super(null); // delay initialization of collator
    comparator = new DartElementComparator();
  }

  /*
   * @see ViewerSorter#category
   */
  @Override
  public int category(Object element) {
    return comparator.category(element);
  }

  /*
   * @see ViewerSorter#compare
   */
  @Override
  public int compare(Viewer viewer, Object e1, Object e2) {
    return comparator.compare(viewer, e1, e2);
  }

  /**
   * Overrides {@link org.eclipse.jface.viewers.ViewerSorter#getCollator()}.
   * 
   * @deprecated The method is not intended to be used by clients.
   */
  @Deprecated
  @Override
  public final java.text.Collator getCollator() {
    // kept in for API compatibility
    if (collator == null) {
      collator = java.text.Collator.getInstance();
    }
    return collator;
  }
}
