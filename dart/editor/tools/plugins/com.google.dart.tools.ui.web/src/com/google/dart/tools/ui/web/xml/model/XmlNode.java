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
package com.google.dart.tools.ui.web.xml.model;

import com.google.dart.tools.ui.web.utils.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * An xml model element. A node is parented by another node, and has child nodes and zero or more
 * owned XmlAttribute properties.
 */
public class XmlNode extends Node {
  private List<XmlAttribute> attributes = new ArrayList<XmlAttribute>();
  private String name;
  private String contents;

  public XmlNode(String name) {
    this.name = name;
  }

  public List<XmlAttribute> getAttributes() {
    return attributes;
  }

  public String getAttributeString(String name) {
    for (XmlAttribute attr : attributes) {
      if (name.equals(attr.getName())) {
        return attr.getValue();
      }
    }

    return null;
  }

  public String getContents() {
    return contents;
  }

  @Override
  public String getId() {
    if (getParent() == null) {
      return getLabel();
    } else {
      return getParent().getId() + "." + getLabel() + getChildPos();
    }
  }

  @Override
  public String getLabel() {
    return name;
  }

  private int getChildPos() {
    List<Node> children = getParent().getChildren();

    for (int i = 0; i < children.size(); i++) {
      if (children.get(i) == this) {
        return i;
      }
    }

    return -1;
  }

}
