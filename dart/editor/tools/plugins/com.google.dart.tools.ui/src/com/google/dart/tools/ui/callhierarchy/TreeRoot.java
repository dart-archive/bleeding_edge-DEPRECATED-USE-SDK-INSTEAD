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
package com.google.dart.tools.ui.callhierarchy;

import com.google.dart.tools.ui.internal.callhierarchy.MethodWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TreeRoot {
  public static final Object EMPTY_ROOT = new Object();
  private MethodWrapper[] roots;

  public TreeRoot(MethodWrapper[] roots) {
    this.roots = roots;
  }

  /**
   * Adds the new roots to the list.
   * 
   * @param moreRoots the roots to add
   */
  void addRoots(MethodWrapper[] moreRoots) {
    List<MethodWrapper> newRoots = new ArrayList<MethodWrapper>();
    newRoots.addAll(Arrays.asList(roots));
    newRoots.addAll(Arrays.asList(moreRoots));
    roots = newRoots.toArray(new MethodWrapper[newRoots.size()]);
  }

  MethodWrapper[] getRoots() {
    return roots;
  }
}
