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
package com.google.dart.tools.core.internal.completion.ast;

import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartIfStatement;
import com.google.dart.compiler.ast.DartStatement;
import com.google.dart.tools.core.internal.completion.Mark;

import java.util.Stack;

public class IfCompleter extends DartIfStatement implements CompletionNode {
  static final long serialVersionUID = 1L;

  public static IfCompleter from(DartIfStatement ifStmt) {
    return CompletionUtil.init(new IfCompleter(
        ifStmt.getCondition(),
        ifStmt.getThenStatement(),
        ifStmt.getElseStatement()), ifStmt);
  }

  private Stack<Mark> stack;

  public IfCompleter(DartExpression condition, DartStatement thenStmt, DartStatement elseStmt) {
    super(condition, thenStmt, elseStmt);
  }

  @Override
  public Stack<Mark> getCompletionParsingContext() {
    return stack;
  }

  @Override
  public void setCompletionParsingContext(Stack<Mark> stack) {
    this.stack = stack;
  }
}
