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

import com.google.dart.engine.html.ast.HtmlScriptTagNode;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlNode;
import com.google.dart.engine.html.ast.XmlTagNode;

/**
 * The interface {@code XmlVisitor} defines the behavior of objects that can be used to visit an
 * {@link XmlNode} structure.
 * 
 * @coverage dart.engine.html
 */
public interface XmlVisitor<R> {
  public R visitHtmlScriptTagNode(HtmlScriptTagNode node);

  public R visitHtmlUnit(HtmlUnit htmlUnit);

  public R visitXmlAttributeNode(XmlAttributeNode xmlAttributeNode);

  public R visitXmlTagNode(XmlTagNode xmlTagNode);
}
