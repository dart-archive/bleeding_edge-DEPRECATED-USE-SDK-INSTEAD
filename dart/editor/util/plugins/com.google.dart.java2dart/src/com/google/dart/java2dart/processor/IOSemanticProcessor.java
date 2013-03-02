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

import com.google.common.base.Objects;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NullLiteral;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.java2dart.Context;
import com.google.dart.java2dart.util.JavaUtils;

import static com.google.dart.java2dart.util.ASTFactory.identifier;
import static com.google.dart.java2dart.util.ASTFactory.methodInvocation;
import static com.google.dart.java2dart.util.ASTFactory.namedExpression;
import static com.google.dart.java2dart.util.ASTFactory.propertyAccess;
import static com.google.dart.java2dart.util.TokenFactory.token;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import java.util.List;

/**
 * {@link SemanticProcessor} for Java IO.
 */
public class IOSemanticProcessor extends SemanticProcessor {
  public static final SemanticProcessor INSTANCE = new IOSemanticProcessor();

  @Override
  public void process(final Context context, final CompilationUnit unit) {
    unit.accept(new GeneralizingASTVisitor<Void>() {

      @Override
      public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
        super.visitInstanceCreationExpression(node);
        Object binding = context.getNodeBinding(node);
        if (binding instanceof IMethodBinding) {
          IMethodBinding methodBinding = (IMethodBinding) binding;
          ITypeBinding declaringClass = methodBinding.getDeclaringClass();
          List<Expression> args = node.getArgumentList().getArguments();
          // new URI()
          if (JavaUtils.getQualifiedName(declaringClass).equals("java.net.URI") && args.size() == 4
              && args.get(0) instanceof NullLiteral && args.get(1) instanceof NullLiteral
              && args.get(3) instanceof NullLiteral) {
            node.getConstructorName().setName(identifier("fromComponents"));
            Expression pathExpression = args.get(2);
            args.clear();
            args.add(namedExpression("path", pathExpression));
            return null;
          }
          // new File()
          if (isMethodInClass2(
              methodBinding,
              "<init>(java.io.File,java.lang.String)",
              "java.io.File")) {
            replaceNode(node, methodInvocation("newRelativeFile", args));
            return null;
          }
          if (isMethodInClass2(methodBinding, "<init>(java.net.URI)", "java.io.File")) {
            replaceNode(node, methodInvocation("newFileFromUri", args));
            return null;
          }
        }
        // done
        return null;
      }

      @Override
      public Void visitMethodInvocation(MethodInvocation node) {
        super.visitMethodInvocation(node);
//        List<Expression> args = node.getArgumentList().getArguments();
        SimpleIdentifier nameNode = node.getMethodName();
        if (isMethodInClass(node, "getScheme", "java.net.URI")) {
          replaceNode(node, propertyAccess(node.getTarget(), identifier("scheme")));
          return null;
        }
        if (isMethodInClass(node, "getPath", "java.net.URI")
            || isMethodInClass(node, "getSchemeSpecificPart", "java.net.URI")) {
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
        // remove URI.normalize()
        if (isMethodInClass2(node, "normalize()", "java.net.URI")) {
          replaceNode(node, node.getTarget());
          return null;
        }
        if (isMethodInClass(node, "getName", "java.io.File")) {
          replaceNode(node, propertyAccess(node.getTarget(), identifier("path")));
          return null;
        }
        if (isMethodInClass(node, "getPath", "java.io.File")
            || isMethodInClass(node, "getAbsolutePath", "java.io.File")) {
          nameNode.setToken(token("fullPathSync"));
          return null;
        }
        if (isMethodInClass2(node, "getAbsoluteFile()", "java.io.File")) {
          replaceNode(node, methodInvocation("getAbsoluteFile", node.getTarget()));
          return null;
        }
        if (isMethodInClass(node, "exists", "java.io.File")) {
          nameNode.setToken(token("existsSync"));
          return null;
        }
        if (isMethodInClass(node, "toURI", "java.io.File")) {
          replaceNode(node, methodInvocation("newUriFromFile", node.getTarget()));
          return null;
        }
        return null;
      }

      @Override
      public Void visitPropertyAccess(PropertyAccess node) {
        super.visitPropertyAccess(node);
        ITypeBinding typeBinding = context.getNodeTypeBinding(node.getTarget());
        String name = node.getPropertyName().getName();
        if (JavaUtils.isTypeNamed(typeBinding, "java.io.File")) {
          if (name.equals("separator")) {
            replaceNode(node, propertyAccess(identifier("JavaSystemIO"), "pathSeparator"));
            return null;
          }
          if (name.equals("separatorChar")) {
            replaceNode(node, propertyAccess(identifier("JavaSystemIO"), "pathSeparatorChar"));
            return null;
          }
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
        }
        return null;
      }

      private boolean isMethodInClass(MethodInvocation node, String reqName, String reqClassName) {
        String name = node.getMethodName().getName();
        return Objects.equal(name, reqName)
            && JavaUtils.isMethodInClass(context.getNodeBinding(node), reqClassName);
      }

      private boolean isMethodInClass2(IMethodBinding binding, String reqSignature,
          String reqClassName) {
        return JavaUtils.getMethodDeclarationSignature(binding).equals(reqSignature)
            && JavaUtils.isMethodInClass(binding, reqClassName);
      }

      private boolean isMethodInClass2(MethodInvocation node, String reqSignature,
          String reqClassName) {
        IMethodBinding binding = (IMethodBinding) context.getNodeBinding(node);
        return isMethodInClass2(binding, reqSignature, reqClassName);
      }
    });
  }
}
