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
import com.google.dart.compiler.ast.DartField;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.Modifiers;
import com.google.dart.tools.core.internal.completion.Mark;

import java.util.Stack;

public class FieldCompleter extends DartField implements CompletionNode {
  static final long serialVersionUID = 1L;

  public static FieldCompleter from(DartField field) {
    return CompletionUtil.init(
        new FieldCompleter(
            field.getName(),
            field.getModifiers(),
            field.getAccessor(),
            field.getValue()),
        field);
  }

  private Stack<Mark> stack;

  public FieldCompleter(DartIdentifier name, Modifiers modifiers, DartMethodDefinition accessor,
      DartExpression value) {
    super(name, modifiers, accessor, value);
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
