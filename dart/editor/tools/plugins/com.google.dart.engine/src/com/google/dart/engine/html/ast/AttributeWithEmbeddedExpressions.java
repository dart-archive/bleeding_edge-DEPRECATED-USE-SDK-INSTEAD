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
package com.google.dart.engine.html.ast;

import com.google.dart.engine.html.scanner.Token;

/**
 * Instances of the class {@code AttributeWithEmbeddedExpressions} represent an attribute whose
 * value contains one or more embedded expressions.
 */
public class AttributeWithEmbeddedExpressions extends XmlAttributeNode {
  /**
   * The expressions that are embedded in the attribute's value.
   */
  private EmbeddedExpression[] expressions;

  /**
   * Initialize a newly created attribute whose value contains one or more embedded expressions.
   * 
   * @param name the name of the attribute
   * @param equals the equals sign separating the name from the value
   * @param value the value of the attribute
   * @param expressions the expressions that are embedded in the value
   */
  public AttributeWithEmbeddedExpressions(Token name, Token equals, Token value,
      EmbeddedExpression[] expressions) {
    super(name, equals, value);
    this.expressions = expressions;
  }

  @Override
  public EmbeddedExpression[] getExpressions() {
    return expressions;
  }
}
