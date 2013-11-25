/*
 * Copyright 2013, the Dart project authors.
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

import com.google.dart.engine.html.ast.HtmlScriptTagNode;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlTagNode;

/**
 * Instances of the class {@code SimpleXmlVisitor} implement an AST visitor that will do nothing
 * when visiting an AST node. It is intended to be a superclass for classes that use the visitor
 * pattern primarily as a dispatch mechanism (and hence don't need to recursively visit a whole
 * structure) and that only need to visit a small number of node types.
 */
public class SimpleXmlVisitor<R> implements XmlVisitor<R> {
  @Override
  public R visitHtmlScriptTagNode(HtmlScriptTagNode node) {
    return null;
  }

  @Override
  public R visitHtmlUnit(HtmlUnit htmlUnit) {
    return null;
  }

  @Override
  public R visitXmlAttributeNode(XmlAttributeNode xmlAttributeNode) {
    return null;
  }

  @Override
  public R visitXmlTagNode(XmlTagNode xmlTagNode) {
    return null;
  }
}
