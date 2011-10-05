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

import com.google.dart.core.IPackageFragmentRoot;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * Filters out all elements which libraries
 */
public class LibraryFilter extends ViewerFilter {

  /**
   * Returns the result of this filter, when applied to the given inputs.
   * 
   * @return Returns true if element should be included in filtered set
   */
  @Override
  public boolean select(Viewer viewer, Object parent, Object element) {
    if (element instanceof IPackageFragmentRoot) {
      return !((IPackageFragmentRoot) element).isArchive();
    }
    return true;
  }
}
