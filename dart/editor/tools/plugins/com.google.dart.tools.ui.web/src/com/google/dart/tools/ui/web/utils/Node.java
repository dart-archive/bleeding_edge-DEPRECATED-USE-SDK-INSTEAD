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
package com.google.dart.tools.ui.web.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * The abstract superclass of model elements.
 */
public abstract class Node {
  private Node parent;
  private String label;
  private List<Node> children = new ArrayList<Node>();

  private Token startToken;
  private Token endToken;

  public Node() {

  }

  public Node(String label) {
    this.label = label;
  }

  public void addChild(Node child) {
    child.setParent(this);

    children.add(child);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }

    if (this == obj) {
      return true;
    }

    if (this.getClass().isAssignableFrom(obj.getClass())) {
      Node other = (Node) obj;

      return safeEquals(getId(), other.getId());
    } else {
      return false;
    }
  }

  public List<Node> getChildren() {
    return children;
  }

  public int getEndOffset() {
    if (endToken != null) {
      return endToken.getLocation() + endToken.getValue().length();
    } else if (startToken != null) {
      return startToken.getLocation() + startToken.getValue().length();
    } else {
      return -1;
    }
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

  public Node getParent() {
    return parent;
  }

  public int getStartOffset() {
    return startToken == null ? -1 : startToken.getLocation();
  }

  public Token getStartToken() {
    return startToken;
  }

  public void setEnd(Token t) {
    this.endToken = t;
  }

  public void setParent(Node parent) {
    this.parent = parent;
  }

  public void setStart(Token t) {
    this.startToken = t;
  }

  @Override
  public String toString() {
    return getLabel();
  }

  protected void setLabel(String label) {
    this.label = label;
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
