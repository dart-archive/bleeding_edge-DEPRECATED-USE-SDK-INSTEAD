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

package com.google.dart.java2dart.engine;

import com.google.common.base.Objects;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.java2dart.Context;
import com.google.dart.java2dart.processor.SemanticProcessor;
import com.google.dart.java2dart.util.JavaUtils;

import static com.google.dart.java2dart.util.ASTFactory.typeName;

import java.util.Iterator;

/**
 * {@link SemanticProcessor} for Engine.
 */
public class EngineSemanticProcessor extends SemanticProcessor {
  public static final SemanticProcessor INSTANCE = new EngineSemanticProcessor();

  @Override
  public void process(final Context context, final CompilationUnit unit) {
    NodeList<CompilationUnitMember> declarations = unit.getDeclarations();
    // remove NodeList, it is declared in enginelib.dart
    for (Iterator<CompilationUnitMember> iter = declarations.iterator(); iter.hasNext();) {
      CompilationUnitMember member = iter.next();
      if (member instanceof ClassDeclaration) {
        ClassDeclaration classDeclaration = (ClassDeclaration) member;
        String name = classDeclaration.getName().getName();
        if (name.equals("NodeList") || name.equals("NodeLocator")
            || name.equals("NodeFoundException")) {
          iter.remove();
        }
      }
    }
    // process nodes
    unit.accept(new GeneralizingASTVisitor<Void>() {
      @Override
      public Void visitMethodDeclaration(MethodDeclaration node) {
        String name = node.getName().getName();
        if ("accept".equals(name) && node.getParameters().getParameters().size() == 1) {
          node.setReturnType(null);
          FormalParameter formalParameter = node.getParameters().getParameters().get(0);
          ((SimpleFormalParameter) formalParameter).getType().setTypeArguments(null);
        }
        return super.visitMethodDeclaration(node);
      }

      @Override
      public Void visitMethodInvocation(MethodInvocation node) {
        if (isMethodInClass(node, "toArray", "com.google.dart.engine.utilities.collection.IntList")) {
          replaceNode(node, node.getTarget());
          return null;
        }
        return super.visitMethodInvocation(node);
      }

      @Override
      public Void visitTypeName(TypeName node) {
        if (node.getName() instanceof SimpleIdentifier) {
          SimpleIdentifier nameNode = (SimpleIdentifier) node.getName();
          String name = nameNode.getName();
          if ("IntList".equals(name)) {
            replaceNode(node, typeName("List", typeName("int")));
            return null;
          }
        }
        return super.visitTypeName(node);
      }

      private boolean isMethodInClass(MethodInvocation node, String reqName, String reqClassName) {
        String name = node.getMethodName().getName();
        return Objects.equal(name, reqName)
            && JavaUtils.isMethodInClass(context.getNodeBinding(node), reqClassName);
      }
    });
  }
}
