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
package com.google.dart.eclipse.ui.internal.navigator;

import com.google.dart.tools.core.model.DartLibrary;

import org.eclipse.jface.viewers.ViewerSorter;

import java.util.Comparator;

/**
 * Sorts dart elements in the CNF project explorer.
 */
public class DartNavigatorViewerSorter extends ViewerSorter {

  @Override
  public int category(Object element) {
    return (element instanceof DartLibrary) ? 2 : 1;
  }

  @Override
  protected Comparator<?> getComparator() {
    return String.CASE_INSENSITIVE_ORDER;
  }

}
