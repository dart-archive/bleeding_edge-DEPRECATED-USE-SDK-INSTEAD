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
package com.google.dart.tools.search.internal.ui;

import com.ibm.icu.text.Collator;

import org.eclipse.ui.IWorkingSet;

import java.util.Comparator;

@SuppressWarnings("rawtypes")
public class WorkingSetComparator implements Comparator {

  private Collator fCollator = Collator.getInstance();

  /*
   * @see Comparator#compare(Object, Object)
   */
  @Override
  public int compare(Object o1, Object o2) {
    String name1 = null;
    String name2 = null;

    if (o1 instanceof IWorkingSet) {
      name1 = ((IWorkingSet) o1).getLabel();
    }

    if (o2 instanceof IWorkingSet) {
      name2 = ((IWorkingSet) o2).getLabel();
    }

    return fCollator.compare(name1, name2);
  }
}
