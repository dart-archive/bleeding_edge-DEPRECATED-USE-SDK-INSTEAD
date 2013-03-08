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
package com.google.dart.engine.html.ast.visitor;

import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlTagNode;

/**
 * Instances of the class {@code RecursiveXmlVisitor} implement an XML visitor that will recursively
 * visit all of the nodes in an XML structure. For example, using an instance of this class to visit
 * a {@link XmlTagNode} will also cause all of the contained {@link XmlAttributeNode}s and
 * {@link XmlTagNode}s to be visited.
 * <p>
 * Subclasses that override a visit method must either invoke the overridden visit method or must
 * explicitly ask the visited node to visit its children. Failure to do so will cause the children
 * of the visited node to not be visited.
 * 
 * @coverage dart.engine.html
 */
public class RecursiveXmlVisitor<R> implements XmlVisitor<R> {

  @Override
  public R visitHtmlUnit(HtmlUnit node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitXmlAttributeNode(XmlAttributeNode node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitXmlTagNode(XmlTagNode node) {
    node.visitChildren(this);
    return null;
  }
}
