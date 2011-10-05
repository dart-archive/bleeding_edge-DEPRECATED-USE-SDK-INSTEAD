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
package com.google.dart.tools.ui.internal.workingsets;

import com.ibm.icu.text.Collator;

import org.eclipse.ui.IWorkingSet;

import java.util.Comparator;

/**
 * Comparator class to sort working sets, optionally keeping the default working set at the top.
 */
public class WorkingSetComparator implements Comparator<Object> {

  private Collator fCollator = Collator.getInstance();

  /**
   * Boolean value to determine whether to keep default working set on the top while sorting.
   */
  private boolean fIsOtherWorkingSetOnTop;

  /**
   * Creates new instance of the working set comparator.
   */
  public WorkingSetComparator() {
    fIsOtherWorkingSetOnTop = false;
  }

  /**
   * Creates a new instance of working set comparator and initializes the boolean field value to the
   * given value, which determines whether or not the default working set is kept on top while
   * sorting the working sets.
   * 
   * @param isOtherWorkingSetOnTop <code>true</code> if default working set is to be retained at the
   *          top, <code>false</code> otherwise
   */
  public WorkingSetComparator(boolean isOtherWorkingSetOnTop) {
    fIsOtherWorkingSetOnTop = isOtherWorkingSetOnTop;
  }

  /**
   * Returns <code>-1</code> if the first argument is the default working set, <code>1</code> if the
   * second argument is the default working set and if the boolean
   * <code>fIsOtherWorkingSetOnTop</code> is set, to keep the default working set on top while
   * sorting.
   * 
   * @see Comparator#compare(Object, Object)
   */
  @Override
  public int compare(Object o1, Object o2) {

    String name1 = null;
    String name2 = null;

    if (o1 instanceof IWorkingSet) {
      IWorkingSet workingSet = (IWorkingSet) o1;
      if (fIsOtherWorkingSetOnTop && IWorkingSetIDs.OTHERS.equals(workingSet.getId())) {
        return -1;
      }
      name1 = workingSet.getLabel();
    }

    if (o2 instanceof IWorkingSet) {
      IWorkingSet workingSet = (IWorkingSet) o2;
      if (fIsOtherWorkingSetOnTop && IWorkingSetIDs.OTHERS.equals(workingSet.getId())) {
        return 1;
      }
      name2 = workingSet.getLabel();
    }
    return fCollator.compare(name1, name2);
  }
}
