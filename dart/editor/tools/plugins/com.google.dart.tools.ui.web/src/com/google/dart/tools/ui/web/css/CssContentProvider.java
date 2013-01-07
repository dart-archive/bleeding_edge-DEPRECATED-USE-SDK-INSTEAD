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

package com.google.dart.tools.ui.web.css;

import com.google.dart.tools.ui.web.css.model.CssProperty;
import com.google.dart.tools.ui.web.css.model.CssSection;
import com.google.dart.tools.ui.web.utils.Node;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * A content provider for CSS model elements.
 */
class CssContentProvider implements ITreeContentProvider {

  @Override
  public void dispose() {

  }

  @Override
  public Object[] getChildren(Object element) {
    Node node = (Node) element;

    if (node instanceof CssSection) {
      CssSection section = (CssSection) node;

      return section.getBody().getChildren().toArray();
    } else {
      return node.getChildren().toArray();
    }
  }

  @Override
  public Object[] getElements(Object element) {
    return getChildren(element);
  }

  @Override
  public Object getParent(Object element) {
    Node node = (Node) element;

    if (node instanceof CssProperty && node.getParent() != null) {
      return node.getParent().getParent();
    } else {
      return node.getParent();
    }
  }

  @Override
  public boolean hasChildren(Object element) {
    return getChildren(element).length > 0;
  }

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

  }

}
