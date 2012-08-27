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

import com.google.dart.tools.ui.internal.filesview.FilesViewerComparator;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

/**
 * Sorts dart elements in the CNF project explorer.
 */
public class DartNavigatorViewerSorter extends ViewerSorter {

  private final FilesViewerComparator comparator = new FilesViewerComparator();

  @Override
  public int compare(Viewer viewer, Object e1, Object e2) {
    return comparator.compare(viewer, e1, e2);
  }

}
