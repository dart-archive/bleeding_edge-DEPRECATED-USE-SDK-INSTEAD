/*
 * Copyright 2012, the Dart project authors.
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
package com.google.dart.engine.ast;

import com.google.dart.engine.scanner.Token;

import java.util.List;

/**
 * Instances of the class <code>SwitchDefault</code> represent the default case in a switch
 * statement.
 * 
 * <pre>
 * switchDefault ::=
 *     {@link SimpleIdentifier label}? ('case' {@link Expression expression} ':')* 'default' ':' {@link Statement statement}*
 * </pre>
 */
public class SwitchDefault extends SwitchMember {
  /**
   * Initialize a newly created switch default.
   */
  public SwitchDefault() {
  }

  /**
   * Initialize a newly created switch default.
   * 
   * @param label the label associated with the switch member
   * @param keyword the token representing the 'case' or 'default' keyword
   * @param colon the colon separating the keyword or the expression from the statements
   * @param statements the statements that will be executed if this switch member is selected
   */
  public SwitchDefault(Label label, Token keyword, Token colon, List<Statement> statements) {
    super(label, keyword, colon, statements);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitSwitchDefault(this);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(getLabel(), visitor);
    getStatements().accept(visitor);
  }
}
