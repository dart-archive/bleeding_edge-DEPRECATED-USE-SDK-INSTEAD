/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.google.dart.tools.ui.web.utils;

import com.google.dart.tools.ui.web.DartWebPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TreeViewer;

/**
 * An action to collapse all the levels in a tree viewer.
 */
public class CollapseAllAction extends Action {
  private TreeViewer viewer;

  public CollapseAllAction(TreeViewer treeViewer) {
    super("Collapse All");

    this.viewer = treeViewer;

    setImageDescriptor(DartWebPlugin.getImageDescriptor("collapse_all.gif"));
  }

  @Override
  public void run() {
    try {
      viewer.getControl().setRedraw(false);
      viewer.collapseAll();
    } finally {
      viewer.getControl().setRedraw(true);
    }
  }

}
