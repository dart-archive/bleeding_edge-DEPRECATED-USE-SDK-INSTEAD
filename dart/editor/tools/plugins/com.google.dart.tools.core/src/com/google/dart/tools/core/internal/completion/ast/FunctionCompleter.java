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

import com.google.dart.compiler.ast.DartBlock;
import com.google.dart.compiler.ast.DartFunction;
import com.google.dart.compiler.ast.DartParameter;
import com.google.dart.compiler.ast.DartTypeNode;
import com.google.dart.tools.core.internal.completion.Mark;

import java.util.List;
import java.util.Stack;

public class FunctionCompleter extends DartFunction implements CompletionNode {
  static final long serialVersionUID = 1L;

  public static FunctionCompleter from(DartFunction p) {
    return CompletionUtil.init(
        new FunctionCompleter(
            p.getParameters(),
            p.getParametersOptionalOpen(),
            p.getParametersOptionalClose(),
            p.getParametersCloseParen(),
            p.getBody(),
            p.getReturnTypeNode()),
        p);
  }

  private Stack<Mark> stack;

  public FunctionCompleter(List<DartParameter> params, int parametersOptionalOpen,
      int parametersOptionalClose, int parametersCloseParen, DartBlock body,
      DartTypeNode returnTypeNode) {
    super(
        params,
        parametersOptionalOpen,
        parametersOptionalClose,
        parametersCloseParen,
        body,
        returnTypeNode);
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
