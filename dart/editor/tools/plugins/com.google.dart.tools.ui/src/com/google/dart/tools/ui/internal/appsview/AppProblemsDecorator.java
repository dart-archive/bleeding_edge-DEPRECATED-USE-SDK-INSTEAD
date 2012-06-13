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
package com.google.dart.tools.ui.internal.appsview;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.ProblemsLabelDecorator;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public class AppProblemsDecorator extends ProblemsLabelDecorator {

  @Override
  protected int computeAdornmentFlags(Object obj) {
    if (obj instanceof ElementTreeNode) {
      ElementTreeNode node = (ElementTreeNode) obj;
      try {
        return findAdornmentFlags(node);
      } catch (CoreException ex) {
        DartToolsPlugin.log(ex);
      }
    }
    return super.computeAdornmentFlags(obj);
  }

  private int findAdornmentFlags(ElementTreeNode node) throws CoreException {
    if (node.isLeaf()) {
      IResource resource = node.getModelElement().getResource();
      return getErrorTicksFromMarkers(resource, IResource.DEPTH_ONE, null);
    } else if (node.isApp()) {
      int mark = 0;
      for (ElementTreeNode child : AppsViewContentProvider.collectLibraries(node)) {
        mark = Math.max(mark, findAdornmentFlags(child));
      }
      return mark;
    } else if (node.isLib()) {
      int mark = 0;
      for (ElementTreeNode child : AppsViewContentProvider.collectFiles(node)) {
        mark = Math.max(mark, findAdornmentFlags(child));
      }
      return mark;
    } else {
      return 0;
    }
  }
}
