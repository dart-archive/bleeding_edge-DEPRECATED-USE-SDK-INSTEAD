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
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.java2dart.Context;

import static com.google.dart.java2dart.util.AstFactory.instanceCreationExpression;
import static com.google.dart.java2dart.util.AstFactory.string;
import static com.google.dart.java2dart.util.AstFactory.typeName;

import org.eclipse.jdt.core.dom.ITypeBinding;

/**
 * {@link SemanticProcessor} for <code>java.util.logging.Logger</code> object.
 */
public class LoggerSemanticProcessor extends SemanticProcessor {
  public LoggerSemanticProcessor(Context context) {
    super(context);
  }

  @Override
  public void process(CompilationUnit unit) {
    unit.accept(new GeneralizingAstVisitor<Void>() {
      @Override
      public Void visitMethodInvocation(MethodInvocation node) {
        super.visitMethodInvocation(node);
        // getLogger() => new Logger()
        if (isMethodInClass(node, "getLogger", "java.util.logging.Logger")) {
          if (node.getArgumentList().getArguments().size() == 1) {
            Expression arg = node.getArgumentList().getArguments().get(0);
            replaceNode(node, instanceCreationExpression(Keyword.NEW, typeName("Logger"), arg));
            replaceClassNameArgument(arg);
          }
          return null;
        }
        // done
        return null;
      }
    });
  }

  /**
   * Replace static calls to ClassName.class.getName() with the string literal "ClassName". This
   * only works for statically accessed calls. For dynamic accesses we change nothing.
   */
  private void replaceClassNameArgument(Expression arg) {
    if (arg instanceof MethodInvocation) {
      MethodInvocation invocation = (MethodInvocation) arg;
      if (isMethodInClass(invocation, "getName", "java.lang.Class")) {
        Expression target = invocation.getTarget();
        ITypeBinding typeBinding = context.getNodeTypeBinding(target);
        if (typeBinding != null) {
          ITypeBinding[] typeArguments = typeBinding.getTypeArguments();
          if (typeArguments.length == 1) {
            String typeName = typeArguments[0].getName();
            replaceNode(arg, string(typeName));
          }
        }
      }
    }
  }
}
