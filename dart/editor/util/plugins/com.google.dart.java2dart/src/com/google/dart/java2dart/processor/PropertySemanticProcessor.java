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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.BlockFunctionBody;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionFunctionBody;
import com.google.dart.engine.ast.ExpressionStatement;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.ThisExpression;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.java2dart.Context;
import com.google.dart.java2dart.util.Bindings;

import static com.google.dart.java2dart.util.ASTFactory.assignmentExpression;
import static com.google.dart.java2dart.util.ASTFactory.propertyAccess;
import static com.google.dart.java2dart.util.TokenFactory.token;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link SemanticProcessor} for converting <code>getX()</code> or <code>setX(x)</code> into getter
 * or setter.
 */
public class PropertySemanticProcessor extends SemanticProcessor {
  private static class FieldPropertyInfo {
    final String name;
    MethodDeclaration getter;
    MethodDeclaration setter;
    String getterField;
    String setterField;

    public FieldPropertyInfo(String name) {
      this.name = name;
    }
  }

  private static boolean hasPrefix(String name, String prefix) {
    // should start with prefix
    if (!name.startsWith(prefix)) {
      return false;
    }
    // there should be one more character
    int prefixLen = prefix.length();
    if (name.length() < prefixLen + 1) {
      return false;
    }
    // next character should be upper case (i.e. property name)
    char nextChar = name.charAt(prefixLen);
    return Character.isUpperCase(nextChar);
  }

  private static boolean isValidSetterType(TypeName type) {
    return type.getName().getName().equals("void");
  }

  public PropertySemanticProcessor(Context context) {
    super(context);
  }

  @Override
  public void process(final CompilationUnit unit) {
    unit.accept(new GeneralizingASTVisitor<Void>() {
      @Override
      public Void visitFieldDeclaration(FieldDeclaration node) {
        if (context.getPrivateClassMembers().contains(node)) {
          for (VariableDeclaration field : node.getFields().getVariables()) {
            SimpleIdentifier name = field.getName();
            context.renameIdentifier(name, "_" + name.getName());
          }
        }
        return super.visitFieldDeclaration(node);
      }

      @Override
      public Void visitMethodDeclaration(MethodDeclaration node) {
        if (node.getName() instanceof SimpleIdentifier && node.getParameters() != null) {
          SimpleIdentifier nameNode = node.getName();
          String name = context.getIdentifierOriginalName(nameNode);
          List<FormalParameter> parameters = node.getParameters().getParameters();
          // getter
          if (parameters.isEmpty() && (hasPrefix(name, "get") || hasPrefix(name, "is"))) {
            if (!context.canMakeProperty(nameNode)) {
              return null;
            }
            String propertyName = StringUtils.uncapitalize(StringUtils.removeStart(name, "get"));
            // rename references
            context.renameIdentifier(nameNode, propertyName);
            // replace MethodInvocation with PropertyAccess
            for (SimpleIdentifier reference : context.getReferences(nameNode)) {
              if (reference.getParent() instanceof MethodInvocation) {
                MethodInvocation invocation = (MethodInvocation) reference.getParent();
                if (invocation.getMethodName() == reference) {
                  Expression invocationTarget = invocation.getTarget();
                  // prepare replacement
                  Expression replacement;
                  if (invocationTarget != null) {
                    replacement = propertyAccess(invocationTarget, reference);
                  } else {
                    replacement = reference;
                  }
                  // do replace
                  replaceNode(invocation, replacement);
                }
              }
            }
            // convert method to getter
            node.setPropertyKeyword(token(Keyword.GET));
            node.setParameters(null);
          }
          // setter
          if (hasPrefix(name, "set") && parameters.size() == 1
              && isValidSetterType(node.getReturnType())) {
            if (!context.canMakeProperty(nameNode)) {
              return null;
            }
            String propertyName = StringUtils.uncapitalize(StringUtils.removeStart(name, "set"));
            // rename references
            context.renameIdentifier(nameNode, propertyName);
            // replace MethodInvocation with AssignmentExpression
            for (SimpleIdentifier reference : context.getReferences(nameNode)) {
              if (reference.getParent() instanceof MethodInvocation) {
                MethodInvocation invocation = (MethodInvocation) reference.getParent();
                if (invocation.getMethodName() == reference) {
                  Expression invocationTarget = invocation.getTarget();
                  List<Expression> arguments = invocation.getArgumentList().getArguments();
                  // prepare assignment target
                  Expression assignmentTarget;
                  if (invocationTarget != null) {
                    assignmentTarget = propertyAccess(invocationTarget, reference);
                  } else {
                    assignmentTarget = reference;
                  }
                  // do replace
                  replaceNode(
                      invocation,
                      assignmentExpression(assignmentTarget, TokenType.EQ, arguments.get(0)));
                }
              }
            }
            // convert method to setter
            node.setPropertyKeyword(token(Keyword.SET));
          }
        }
        return super.visitMethodDeclaration(node);
      }
    });
    // remember all overridden and overriding methods
    final Set<IMethodBinding> ignoredMethods = Sets.newHashSet();
    unit.accept(new GeneralizingASTVisitor<Void>() {
      @Override
      public Void visitMethodDeclaration(MethodDeclaration node) {
        IMethodBinding binding = (IMethodBinding) context.getNodeBinding(node);
        if (binding != null) {
          IMethodBinding superBinding = Bindings.findOverriddenMethod(binding, true);
          if (superBinding != null) {
            ignoredMethods.add(binding);
            ignoredMethods.add(superBinding);
          }
        }
        return null;
      }
    });
    // try to convert properties into fields
    unit.accept(new GeneralizingASTVisitor<Void>() {
      private final Map<String, FieldPropertyInfo> properties = Maps.newHashMap();

      @Override
      public Void visitClassDeclaration(ClassDeclaration node) {
        properties.clear();
        super.visitClassDeclaration(node);
        // simplify field properties
        for (FieldPropertyInfo property : properties.values()) {
          MethodDeclaration getter = property.getter;
          MethodDeclaration setter = property.setter;
          // no getter or not field
          if (getter == null || property.getterField == null) {
            continue;
          }
          // setter is not field
          if (setter != null && !StringUtils.equals(property.getterField, property.setterField)) {
            continue;
          }
          // remove getter, update field
          if (getter != null) {
            ClassDeclaration clazz = (ClassDeclaration) getter.getParent();
            List<ClassMember> members = clazz.getMembers();
            members.remove(getter);
            for (ClassMember member : members) {
              if (member instanceof FieldDeclaration) {
                FieldDeclaration fieldDeclaration = (FieldDeclaration) member;
                List<VariableDeclaration> variables = fieldDeclaration.getFields().getVariables();
                for (VariableDeclaration field : variables) {
                  SimpleIdentifier fieldName = field.getName();
                  if (fieldName.getName().equals(property.getterField)) {
                    context.renameIdentifier(fieldName, property.name);
                    // mark "final" is no writes
                    {
                      boolean readOnly = true;
                      List<SimpleIdentifier> references = context.getReferences(fieldName);
                      for (SimpleIdentifier reference : references) {
                        readOnly &= reference.inGetterContext();
                      }
                      if (readOnly) {
                        fieldDeclaration.getFields().setKeyword(token(Keyword.FINAL));
                      }
                    }
                    // now these are field references
                    {
                      IBinding fieldBinding = context.getNodeBinding(fieldName);
                      replaceMethodReferencesWithFieldBindings(getter, fieldBinding);
                      replaceMethodReferencesWithFieldBindings(setter, fieldBinding);
                    }
                    // done
                    break;
                  }
                }
              }
            }
          }
          // remove setter
          if (setter != null) {
            ClassDeclaration clazz = (ClassDeclaration) setter.getParent();
            List<ClassMember> members = clazz.getMembers();
            members.remove(setter);
          }
        }
        // done
        return null;
      }

      @Override
      public Void visitMethodDeclaration(MethodDeclaration node) {
        // don't remove method if it overrides
        {
          IMethodBinding binding = (IMethodBinding) context.getNodeBinding(node);
          if (binding == null) {
            return null;
          }
          if (ignoredMethods.contains(binding)) {
            return null;
          }
        }
        // getter
        if (node.isGetter()) {
          String name = node.getName().getName();
          FieldPropertyInfo property = getProperty(name);
          property.getter = node;
          if (node.getBody() instanceof ExpressionFunctionBody) {
            ExpressionFunctionBody body = (ExpressionFunctionBody) node.getBody();
            Expression expression = body.getExpression();
            if (expression instanceof SimpleIdentifier) {
              SimpleIdentifier identifier = (SimpleIdentifier) expression;
              property.getterField = identifier.getName();
            }
          }
        }
        // setter
        if (node.isSetter()) {
          String name = node.getName().getName();
          FieldPropertyInfo property = getProperty(name);
          property.setter = node;
          // block body
          if (!(node.getBody() instanceof BlockFunctionBody)) {
            return null;
          }
          // single statement
          BlockFunctionBody body = (BlockFunctionBody) node.getBody();
          List<Statement> statements = body.getBlock().getStatements();
          if (statements.size() != 1) {
            return null;
          }
          Statement statement = statements.get(0);
          // prepare expression
          if (!(statement instanceof ExpressionStatement)) {
            return null;
          }
          Expression expression = ((ExpressionStatement) statement).getExpression();
          // should be assignment
          if (!(expression instanceof AssignmentExpression)) {
            return null;
          }
          AssignmentExpression assignment = (AssignmentExpression) expression;
          // simple assignment
          if (assignment.getOperator().getType() != TokenType.EQ) {
            return null;
          }
          // RHS should be just parameter name
          Expression rhs = assignment.getRightHandSide();
          if (!(rhs instanceof SimpleIdentifier)) {
            return null;
          }
          SimpleIdentifier rhsName = (SimpleIdentifier) rhs;
          String parameterName = ((SimpleFormalParameter) node.getParameters().getParameters().get(
              0)).getIdentifier().getName();
          if (!rhsName.getName().equals(parameterName)) {
            return null;
          }
          // LHS
          Expression lhs = assignment.getLeftHandSide();
          if (lhs instanceof PropertyAccess) {
            PropertyAccess access = (PropertyAccess) lhs;
            if (access.getTarget() instanceof ThisExpression) {
              property.setterField = access.getPropertyName().getName();
            }
          }
        }
        return null;
      }

      private FieldPropertyInfo getProperty(String name) {
        FieldPropertyInfo property = properties.get(name);
        if (property == null) {
          property = new FieldPropertyInfo(name);
          properties.put(name, property);
        }
        return property;
      }

      private void replaceMethodReferencesWithFieldBindings(MethodDeclaration method,
          IBinding fieldBinding) {
        if (method == null) {
          return;
        }
        for (SimpleIdentifier reference : context.getReferences(method.getName())) {
          context.putReference(reference, fieldBinding, null);
        }
      }
    });
  }
}
