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

package com.google.dart.tools.ui.internal.text.editor;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.visitor.ElementLocator;
import com.google.dart.engine.element.Element;

import java.util.List;

/**
 * Lightweight container of {@link ASTNode} and its name.
 */
public class LightNodeElement {
  private final LightNodeElement parent;
  private final ASTNode node;
  private final int nameOffset;
  private final int nameLength;
  private final String name;
  public final List<LightNodeElement> children = Lists.newArrayList();

  LightNodeElement(LightNodeElement parent, ASTNode node, ASTNode nameNode, String name) {
    Preconditions.checkNotNull(node);
    Preconditions.checkNotNull(nameNode);
    this.parent = parent;
    this.node = node;
    this.name = name;
    this.nameOffset = nameNode.getOffset();
    this.nameLength = nameNode.getLength();
    if (parent != null) {
      parent.children.add(this);
    }
  }

  /**
   * @return <code>true</code> underlying {@link ASTNode} contains given "offset".
   */
  public boolean contains(int offset) {
    return node.getOffset() <= offset && offset <= node.getEnd();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof LightNodeElement) {
      LightNodeElement other = (LightNodeElement) obj;
      return Objects.equal(other.parent, parent) && other.name.equals(name);
    }
    return false;
  }

  public List<LightNodeElement> getChildren() {
    return children;
  }

  /**
   * @return the resolved {@link Element} for wrapped {@link ASTNode}. May be <code>null</code>.
   */
  public Element getElement() {
    return ElementLocator.locate(node);
  }

  public String getName() {
    return name;
  }

  public int getNameLength() {
    return nameLength;
  }

  public int getNameOffset() {
    return nameOffset;
  }

  public ASTNode getNode() {
    return node;
  }

  public LightNodeElement getParent() {
    return parent;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(parent, name);
  }

  public boolean isPrivate() {
    return name.startsWith("_");
  }
}
