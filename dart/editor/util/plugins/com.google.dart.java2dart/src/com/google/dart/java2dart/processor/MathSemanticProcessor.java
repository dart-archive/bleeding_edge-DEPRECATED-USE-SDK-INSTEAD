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

package com.google.dart.java2dart.processor;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.java2dart.Context;

import static com.google.dart.java2dart.util.AstFactory.methodInvocation;

import java.util.List;

/**
 * {@link SemanticProcessor} for Java <code>java.lang.Math</code>.
 */
public class MathSemanticProcessor extends SemanticProcessor {
  public MathSemanticProcessor(Context context) {
    super(context);
  }

  @Override
  public void process(CompilationUnit unit) {
    unit.accept(new GeneralizingAstVisitor<Void>() {
      @Override
      public Void visitMethodInvocation(MethodInvocation node) {
        super.visitMethodInvocation(node);
        List<Expression> args = node.getArgumentList().getArguments();
        // replace Math.abs(value) with value.abs()
        if (args.size() == 1 && isMethodInClass(node, "abs", "java.lang.Math")) {
          replaceNode(node, methodInvocation(args.get(0), "abs"));
        }
        // done
        return null;
      }
    });
  }
}
