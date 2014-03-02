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
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.java2dart.Context;
import com.google.dart.java2dart.util.JavaUtils;

import static com.google.dart.java2dart.util.AstFactory.constructorName;
import static com.google.dart.java2dart.util.AstFactory.propertyAccess;
import static com.google.dart.java2dart.util.AstFactory.typeName;
import static com.google.dart.java2dart.util.TokenFactory.token;

import org.eclipse.jdt.core.dom.ITypeBinding;

import java.util.List;

/**
 * {@link SemanticProcessor} for <code>java.util.Date</code> object.
 */
public class DateSemanticProcessor extends SemanticProcessor {
  public DateSemanticProcessor(Context context) {
    super(context);
  }

  @Override
  public void process(CompilationUnit unit) {
    unit.accept(new GeneralizingAstVisitor<Void>() {
      @Override
      public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
        super.visitInstanceCreationExpression(node);
        ITypeBinding typeBinding = context.getNodeTypeBinding(node);
        List<Expression> args = node.getArgumentList().getArguments();
        if (JavaUtils.isTypeNamed(typeBinding, "java.util.Date")) {
          // replace instance creation for new java.util.Date() to new DateTime.now()
          if (args.isEmpty()) {
            node.setConstructorName(constructorName(typeName("DateTime"), "now"));
          }
          // replace instance creation of
          //   new java.util.Date(milliseconds)
          // with factory constructor new
          //   DateTime.fromMillisecondsSinceEpoch(milliseconds)
          if (args.size() == 1) {
            node.setConstructorName(constructorName(
                typeName("DateTime"),
                "fromMillisecondsSinceEpoch"));
          }
        }
        return null;
      }

      @Override
      public Void visitMethodInvocation(MethodInvocation node) {
        super.visitMethodInvocation(node);
        // convert .getTime() method invocation to millisecondsSinceEpoch property.
        if (isMethodInClass2(node, "getTime()", "java.util.Date")) {
          replaceNode(node, propertyAccess(node.getTarget(), "millisecondsSinceEpoch"));
          return null;
        }
        // some other .getX()
        if (isMethodInClass(node, "java.util.Date")) {
          String name = node.getMethodName().getName();
          if (name.startsWith("get")) {
            name = name.substring("get".length()).toLowerCase();
            replaceNode(node, propertyAccess(node.getTarget(), name));
            return null;
          }
        }
        //
        return null;
      }

      @Override
      public Void visitTypeName(TypeName node) {
        super.visitTypeName(node);
        ITypeBinding binding = context.getNodeTypeBinding(node);
        SimpleIdentifier nameNode = (SimpleIdentifier) node.getName();
        // Date -> DateTime
        if (JavaUtils.isTypeNamed(binding, "java.util.Date")) {
          nameNode.setToken(token("DateTime"));
        }
        return null;
      }
    });
  }
}
