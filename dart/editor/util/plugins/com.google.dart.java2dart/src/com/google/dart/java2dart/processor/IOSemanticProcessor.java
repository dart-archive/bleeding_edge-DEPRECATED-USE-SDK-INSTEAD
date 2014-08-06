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
import com.google.dart.engine.ast.NullLiteral;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.java2dart.Context;
import com.google.dart.java2dart.util.JavaUtils;

import static com.google.dart.java2dart.util.AstFactory.identifier;
import static com.google.dart.java2dart.util.AstFactory.methodInvocation;
import static com.google.dart.java2dart.util.AstFactory.namedExpression;
import static com.google.dart.java2dart.util.AstFactory.propertyAccess;
import static com.google.dart.java2dart.util.TokenFactory.token;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import java.util.List;

/**
 * {@link SemanticProcessor} for Java IO.
 */
public class IOSemanticProcessor extends SemanticProcessor {
  public IOSemanticProcessor(Context context) {
    super(context);
  }

  @Override
  public void process(final CompilationUnit unit) {
    unit.accept(new GeneralizingAstVisitor<Void>() {

      @Override
      public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
        super.visitInstanceCreationExpression(node);
        Object binding = context.getNodeBinding(node);
        if (binding instanceof IMethodBinding) {
          IMethodBinding methodBinding = (IMethodBinding) binding;
          ITypeBinding declaringClass = methodBinding.getDeclaringClass();
          List<Expression> args = node.getArgumentList().getArguments();
          // new URI()
          if (JavaUtils.getQualifiedName(declaringClass).equals("java.net.URI")) {
            if (args.size() == 1) {
              replaceNode(node, methodInvocation(identifier("parseUriWithException"), args));
            }
            if (args.size() == 4 && args.get(0) instanceof NullLiteral
                && args.get(1) instanceof NullLiteral && args.get(3) instanceof NullLiteral) {
              Expression pathExpression = args.get(2);
              args.clear();
              args.add(namedExpression("path", pathExpression));
              return null;
            }
          }
          // new File(parent, child)
          if (isMethodInClass2(
              methodBinding,
              "<init>(java.io.File,java.lang.String)",
              "java.io.File")) {
            node.getConstructorName().setName(identifier("relative"));
            return null;
          }
          if (isMethodInClass2(methodBinding, "<init>(java.net.URI)", "java.io.File")) {
            node.getConstructorName().setName(identifier("fromUri"));
            return null;
          }
        }
        // done
        return null;
      }

      @Override
      public Void visitMethodInvocation(MethodInvocation node) {
        super.visitMethodInvocation(node);
        Expression target = node.getTarget();
        List<Expression> args = node.getArgumentList().getArguments();
        SimpleIdentifier nameNode = node.getMethodName();
        // System.out.print[ln](x) -> print(x)
        if (isMethodInClass(node, "print", "java.io.PrintStream")
            || isMethodInClass(node, "println", "java.io.PrintStream")) {
          if (target instanceof PropertyAccess) {
            PropertyAccess propertyAccess = (PropertyAccess) target;
            SimpleIdentifier propertyName = propertyAccess.getPropertyName();
            Expression propertyTarget = propertyAccess.getTarget();
            if (propertyName.getName().equals("out") && propertyTarget instanceof SimpleIdentifier) {
              if (((SimpleIdentifier) propertyTarget).getName().equals("System")) {
                replaceNode(node, methodInvocation("print", args));
                return null;
              }
            }
          }
        }
        // java.net.URI
        if (isMethodInClass2(node, "create(java.lang.String)", "java.net.URI")) {
          replaceNode(node, methodInvocation(identifier("parseUriWithException"), args));
          return null;
        }
        if (isMethodInClass(node, "getScheme", "java.net.URI")) {
          replaceNode(node, propertyAccess(node.getTarget(), identifier("scheme")));
          return null;
        }
        if (isMethodInClass(node, "getPath", "java.net.URI")
            || isMethodInClass(node, "getSchemeSpecificPart", "java.net.URI")
            || isMethodInClass(node, "getRawSchemeSpecificPart", "java.net.URI")) {
          replaceNode(node, propertyAccess(node.getTarget(), identifier("path")));
          return null;
        }
        if (isMethodInClass2(node, "isAbsolute()", "java.net.URI")) {
          replaceNode(node, propertyAccess(node.getTarget(), nameNode));
          return null;
        }
        if (isMethodInClass2(node, "resolve(java.net.URI)", "java.net.URI")) {
          nameNode.setToken(token("resolveUri"));
          return null;
        }
        if (isMethodInClass2(node, "normalize()", "java.net.URI")) {
          replaceNode(node, node.getTarget());
          return null;
        }
        return null;
      }

      @Override
      public Void visitPropertyAccess(PropertyAccess node) {
        super.visitPropertyAccess(node);
        Expression target = node.getTarget();
        Object targetBinding = context.getNodeBinding(target);
        if (JavaUtils.isTypeNamed(targetBinding, "java.io.File")) {
          replaceNode(target, identifier("JavaFile"));
        }
        return null;
      }

      @Override
      public Void visitTypeName(TypeName node) {
        super.visitTypeName(node);
        Object binding = context.getNodeTypeBinding(node);
        if (node.getName() instanceof SimpleIdentifier) {
          SimpleIdentifier nameNode = (SimpleIdentifier) node.getName();
          if (JavaUtils.isTypeNamed(binding, "java.net.URI")) {
            nameNode.setToken(token("Uri"));
            return null;
          }
          if (JavaUtils.isTypeNamed(binding, "java.io.File")) {
            nameNode.setToken(token("JavaFile"));
            return null;
          }
          if (JavaUtils.isTypeNamed(binding, "java.io.IOException")) {
            nameNode.setToken(token("JavaIOException"));
            return null;
          }
        }
        return null;
      }
    });
  }
}
