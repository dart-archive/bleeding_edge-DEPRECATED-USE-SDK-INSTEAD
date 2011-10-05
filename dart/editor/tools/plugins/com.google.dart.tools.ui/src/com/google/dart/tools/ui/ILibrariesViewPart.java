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

import com.google.dart.tools.ui.internal.libraryview.LibraryExplorerPart;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IViewPart;

/**
 * This interface is used to refer to the {@link LibraryExplorerPart} in an abstract way.
 */
public interface ILibrariesViewPart extends IViewPart {
  /**
   * Returns the TreeViewer shown in the Library view.
   * 
   * @return the tree viewer used in the Library view
   */
  TreeViewer getTreeViewer();

  /**
   * Returns whether this Library view's selection automatically tracks the active editor.
   * 
   * @return <code>true</code> if linking is enabled, <code>false</code> if not
   */
  boolean isLinkingEnabled();

  /**
   * Selects and reveals the given element in this Library view. The tree will be expanded as needed
   * to show the element.
   * 
   * @param element the element to be revealed
   */
  void selectAndReveal(Object element);

  /**
   * Sets whether this Library view's selection automatically tracks the active editor.
   * 
   * @param enabled <code>true</code> to enable, <code>false</code> to disable
   */
  void setLinkingEnabled(boolean enabled);
}
