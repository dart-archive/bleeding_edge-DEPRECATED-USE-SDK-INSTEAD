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

import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartParameterizedTypeNode;
import com.google.dart.compiler.ast.DartStringLiteral;
import com.google.dart.compiler.ast.DartTypeNode;
import com.google.dart.compiler.ast.DartTypeParameter;
import com.google.dart.compiler.ast.Modifiers;
import com.google.dart.tools.core.internal.completion.Mark;

import java.util.List;
import java.util.Stack;

public class ClassCompleter extends DartClass implements CompletionNode {
  static final long serialVersionUID = 1L;

  public static ClassCompleter from(DartClass type) {
    return CompletionUtil.init(
        new ClassCompleter(
            type.getName(),
            type.getNativeName(),
            type.getSuperclass(),
            type.getInterfaces(),
            type.getOpenBraceOffset(),
            type.getCloseBraceOffset(),
            type.getMembers(),
            type.getTypeParameters(),
            type.getDefaultClass(),
            type.isInterface(),
            type.getModifiers()),
        type);
  }

  private Stack<Mark> stack;

  public ClassCompleter(DartIdentifier name, DartStringLiteral nativeName, DartTypeNode superclass,
      List<DartTypeNode> interfaces, int openBraceOffset, int closeBraceOffset,
      List<DartNode> members, List<DartTypeParameter> typeParameters,
      DartParameterizedTypeNode defaultClass, boolean isInterface, Modifiers modifiers) {
    super(
        name,
        nativeName,
        superclass,
        interfaces,
        openBraceOffset,
        closeBraceOffset,
        members,
        typeParameters,
        defaultClass,
        isInterface,
        modifiers);
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
