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
package com.google.dart.tools.ui.web.xml;

import com.google.dart.tools.core.html.XmlNode;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import java.util.ArrayList;
import java.util.List;

/**
 * A content provider for Node model elements.
 */
public class XmlNodeContentProvider implements ITreeContentProvider {

  @Override
  public void dispose() {

  }

  @Override
  public Object[] getChildren(Object element) {
    XmlNode node = (XmlNode) element;

    return filterComments(node.getChildren());
  }

  @Override
  public Object[] getElements(Object element) {
    return getChildren(element);
  }

  @Override
  public Object getParent(Object element) {
    XmlNode node = (XmlNode) element;

    return node.getParent();
  }

  @Override
  public boolean hasChildren(Object element) {
    return getChildren(element).length > 0;
  }

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

  }

  private XmlNode[] filterComments(List<XmlNode> nodes) {
    boolean hasComments = false;

    int len = nodes.size();

    for (int i = 0; i < len; i++) {
      if (nodes.get(i).isComment()) {
        hasComments = true;
        break;
      }
    }

    if (!hasComments) {
      return nodes.toArray(new XmlNode[nodes.size()]);
    } else {
      List<XmlNode> copy = new ArrayList<XmlNode>();

      for (XmlNode node : nodes) {
        if (!node.isComment()) {
          copy.add(node);
        }
      }

      return copy.toArray(new XmlNode[copy.size()]);
    }
  }

}
