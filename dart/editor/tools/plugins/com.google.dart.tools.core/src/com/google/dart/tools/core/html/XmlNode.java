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
package com.google.dart.tools.core.html;

import java.util.ArrayList;
import java.util.List;

/**
 * An xml model element. A node is parented by another node, and has child nodes and zero or more
 * owned XmlAttribute properties.
 */
public class XmlNode {
  private XmlNode parent;
  private String label;
  private List<XmlNode> children = new ArrayList<XmlNode>();

  private Token startToken;
  private Token endToken;

  private String contents;

  public XmlNode() {

  }

  public XmlNode(String label) {
    this.label = label;
  }

  public void addChild(XmlNode child) {
    child.setParent(this);

    children.add(child);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }

    if (this.getClass().isAssignableFrom(obj.getClass())) {
      XmlNode other = (XmlNode) obj;

      return safeEquals(getId(), other.getId());
    } else {
      return false;
    }
  }

  public List<XmlNode> getChildren() {
    return children;
  }

  public String getContents() {
    return contents;
  }

  public Token getEndToken() {
    return endToken;
  }

  public String getId() {
    if (getParent() == null) {
      return getLabel();
    } else {
      return getParent().getId() + "." + getLabel() + getChildPos();
    }
  }

  public String getLabel() {
    return label;
  }

  public XmlNode getParent() {
    return parent;
  }

  public Token getStartToken() {
    return startToken;
  }

  @Override
  public int hashCode() {
    return getId().hashCode();
  }

  public void setEnd(Token t) {
    this.endToken = t;
  }

  public void setParent(XmlNode parent) {
    this.parent = parent;
  }

  public void setStart(Token t) {
    this.startToken = t;
  }

  @Override
  public String toString() {
    return getLabel();
  }

  protected void setContents(String value) {
    contents = value;
  }

  private int getChildPos() {
    List<XmlNode> children = getParent().getChildren();

    for (int i = 0; i < children.size(); i++) {
      if (children.get(i) == this) {
        return i;
      }
    }

    return -1;
  }

  private boolean safeEquals(String id, String id2) {
    if (id == id2) {
      return true;
    }

    if (id == null) {
      return false;
    }

    return id.equals(id2);
  }

}
