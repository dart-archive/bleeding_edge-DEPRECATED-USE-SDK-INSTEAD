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

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;

import java.util.List;

public class ElementTreeNode {
  private static final int SDK_LIBRARY_SORT = 3;
  private static final int LIBRARY_SORT = 2;
  private static final int COMPUNIT_SORT = 1;
  private static final int DEFAULT_SORT = 0;

  private DartElement modelElement;
  private ElementTreeNode parentNode;
  private List<ElementTreeNode> childNodes;

  ElementTreeNode(DartElement node, ElementTreeNode parent) {
    this.modelElement = node;
    this.parentNode = parent;
  }

  public List<ElementTreeNode> getChildNodes() {
    return childNodes;
  }

  public DartElement getModelElement() {
    return modelElement;
  }

  public ElementTreeNode getParentNode() {
    return parentNode;
  }

  public int getSortCategory() {
    if (modelElement instanceof DartLibrary) {
      String name = ((DartLibrary) modelElement).getDisplayName();
      if (name.indexOf(':') >= 0) {
        return SDK_LIBRARY_SORT;
      }
      return LIBRARY_SORT;
    } else if (modelElement instanceof CompilationUnit) {
      return COMPUNIT_SORT;
    } else {
      return DEFAULT_SORT;
    }
  }

  public boolean hasChildren() {
    return childNodes != null && childNodes.size() > 0;
  }

  public boolean isApp() {
    return parentNode == null;
  }

  public boolean isLeaf() {
    return childNodes != null && childNodes.size() == 0;
  }

  public boolean isLib() {
    return !isApp() && !isLeaf();
  }

  public void setChildNodes(List<ElementTreeNode> childNodes) {
    this.childNodes = childNodes;
  }
}
