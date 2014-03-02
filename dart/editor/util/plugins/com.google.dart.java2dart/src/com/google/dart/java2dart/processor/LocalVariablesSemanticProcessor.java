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

import com.google.common.collect.Sets;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.DeclaredIdentifier;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ForEachStatement;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.java2dart.Context;
import com.google.dart.java2dart.SyntaxTranslator;

import static com.google.dart.java2dart.util.AstFactory.identifier;
import static com.google.dart.java2dart.util.AstFactory.propertyAccess;
import static com.google.dart.java2dart.util.AstFactory.thisExpression;

import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;

import java.util.Set;

/**
 * {@link SemanticProcessor} for correcting Java vs. Dart local variables semantic differences.
 */
public class LocalVariablesSemanticProcessor extends SemanticProcessor {

  private class ClassSensetiveVisitor extends RecursiveAstVisitor<Void> {
    private Set<String> hierarchyNames;
    private Set<String> methodNames;

    @Override
    public Void visitClassDeclaration(ClassDeclaration node) {
      hierarchyNames = null;
      try {
        return super.visitClassDeclaration(node);
      } finally {
        hierarchyNames = null;
      }
    }

    @Override
    public Void visitForEachStatement(ForEachStatement node) {
      DeclaredIdentifier loopVariable = node.getLoopVariable();
      if (loopVariable != null) {
        SimpleIdentifier nameNode = loopVariable.getIdentifier();
        String variableName = nameNode.getName();
        if (Context.FORBIDDEN_NAMES.contains(variableName)) {
          ensureHierarchyNames(node);
          ensureMethodNames(node);
          String newName = generateUniqueVariableName(variableName);
          context.renameIdentifier(nameNode, newName);
        }
      }
      return super.visitForEachStatement(node);
    }

    @Override
    public Void visitMethodDeclaration(MethodDeclaration node) {
      methodNames = null;
      try {
        return super.visitMethodDeclaration(node);
      } finally {
        methodNames = null;
      }
    }

    void ensureHierarchyNames(AstNode node) {
      if (hierarchyNames != null) {
        return;
      }
      hierarchyNames = context.getSuperMembersNames(node);
    }

    void ensureMethodNames(AstNode node) {
      methodNames = Sets.newHashSet();
      MethodDeclaration method = node.getAncestor(MethodDeclaration.class);
      if (method != null) {
        method.accept(new RecursiveAstVisitor<Void>() {
          @Override
          public Void visitVariableDeclaration(VariableDeclaration node) {
            methodNames.add(node.getName().getName());
            return super.visitVariableDeclaration(node);
          }
        });
      }
    }

    /**
     * @return the new name for variable which does not conflict with name of any member in super
     *         classes - {@link #hierarchyNames}.
     */
    String generateUniqueVariableName(String name) {
      int index = 2;
      while (true) {
        String newName = name + index;
        if (!hierarchyNames.contains(newName) && !methodNames.contains(newName)
            && !Context.FORBIDDEN_NAMES.contains(newName)) {
          methodNames.add(newName);
          return newName;
        }
        index++;
      }
    }
  }

  private static AstNode getExecutableNode(AstNode node) {
    // method
    MethodDeclaration method = node.getAncestor(MethodDeclaration.class);
    if (method != null) {
      return method;
    }
    // constructor
    ConstructorDeclaration constructor = node.getAncestor(ConstructorDeclaration.class);
    if (constructor != null) {
      return constructor;
    }
    // no
    return null;
  }

  private static boolean isMethodInvocationName(AstNode node) {
    AstNode parent = node.getParent();
    return parent instanceof MethodInvocation
        && ((MethodInvocation) parent).getMethodName() == node;
  }

  private static boolean isQualifiedName(SimpleIdentifier node) {
    AstNode parent = node.getParent();
    if (parent instanceof PropertyAccess) {
      return ((PropertyAccess) parent).getPropertyName() == node;
    }
    if (parent instanceof PrefixedIdentifier) {
      return ((PrefixedIdentifier) parent).getIdentifier() == node;
    }
    if (parent instanceof MethodInvocation) {
      MethodInvocation invocation = (MethodInvocation) parent;
      return invocation.getTarget() != null && invocation.getMethodName() == node;
    }
    return false;
  }

  public LocalVariablesSemanticProcessor(Context context) {
    super(context);
  }

  @Override
  public void process(CompilationUnit unit) {
    qualifyShadowedMembers(unit);
    ensureNameNotReferencedInInitializer(unit);
  }

  private void ensureNameNotReferencedInInitializer(CompilationUnit unit) {
    unit.accept(new ClassSensetiveVisitor() {
      @Override
      public Void visitVariableDeclaration(VariableDeclaration node) {
        String name = node.getName().getName();
        Expression initializer = node.getInitializer();
        if (initializer != null) {
          initializer.accept(this);
        }
        if (Context.FORBIDDEN_NAMES.contains(name)) {
          ensureHierarchyNames(node);
          ensureMethodNames(node);
          String newName = generateUniqueVariableName(name);
          context.renameIdentifier(node.getName(), newName);
        }
        return null;
      }
    });
  }

  private Expression getMemberQualifier(AstNode node) {
    IBinding binding = context.getNodeBinding(node);
    if (!Modifier.isStatic(binding.getModifiers())) {
      return thisExpression();
    }
    if (binding instanceof IVariableBinding) {
      IVariableBinding variableBinding = (IVariableBinding) binding;
      String className = variableBinding.getDeclaringClass().getName();
      return identifier(className);
    }
    if (binding instanceof IMethodBinding) {
      IMethodBinding methodBinding = (IMethodBinding) binding;
      String className = methodBinding.getDeclaringClass().getName();
      return identifier(className);
    }
    throw new IllegalArgumentException("Field or method expected: " + binding);
  }

  private void qualifyShadowedMembers(CompilationUnit unit) {
    unit.accept(new RecursiveAstVisitor<Void>() {
      @Override
      public Void visitConstructorDeclaration(ConstructorDeclaration node) {
        qualifyShadowedNodes(node, node.getBody());
        return null;
      }

      @Override
      public Void visitMethodDeclaration(MethodDeclaration node) {
        qualifyShadowedNodes(node, node.getBody());
        return null;
      }

      private void qualifyShadowedNodes(AstNode executableNode, AstNode body) {
        if (body == null) {
          return;
        }
        // prepare names of local variables defined in the same block
        final Set<String> definedVars = Sets.newHashSet();
        executableNode.accept(new GeneralizingAstVisitor<Void>() {
          @Override
          public Void visitCatchClause(CatchClause node) {
            String name = node.getExceptionParameter().getName();
            definedVars.add(name);
            return super.visitCatchClause(node);
          }

          @Override
          public Void visitFormalParameter(FormalParameter node) {
            String name = node.getIdentifier().getName();
            definedVars.add(name);
            return null;
          }

          @Override
          public Void visitVariableDeclaration(VariableDeclaration node) {
            String name = node.getName().getName();
            definedVars.add(name);
            return null;
          }
        });
        // check all field references that could be shadowed
        body.accept(new RecursiveAstVisitor<Void>() {
          @Override
          public Void visitSimpleIdentifier(SimpleIdentifier node) {
            // check if shadowed
            String name = node.getName();
            if (!definedVars.contains(name)) {
              return null;
            }
            // may be qualified
            if (isQualifiedName(node)) {
              return null;
            }
            // should be field or accessor
            if (!context.isFieldBinding(node) && !context.isMethodBinding(node)) {
              return null;
            }
            // replace shadowed unqualified reference with qualified one 
            AstNode parent = node.getParent();
            Expression qualifier = getMemberQualifier(node);
            if (isMethodInvocationName(node)) {
              MethodInvocation invocation = (MethodInvocation) parent;
              invocation.setTarget(qualifier);
            } else {
              SyntaxTranslator.replaceNode(parent, node, propertyAccess(qualifier, node));
            }
            // done
            return null;
          }
        });
      }
    });
  }
}
