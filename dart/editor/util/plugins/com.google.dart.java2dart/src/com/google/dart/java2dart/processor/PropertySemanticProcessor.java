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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.BlockFunctionBody;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionFunctionBody;
import com.google.dart.engine.ast.ExpressionStatement;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.RedirectingConstructorInvocation;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.SuperConstructorInvocation;
import com.google.dart.engine.ast.ThisExpression;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.java2dart.Context;
import com.google.dart.java2dart.util.Bindings;

import static com.google.dart.java2dart.util.AstFactory.assignmentExpression;
import static com.google.dart.java2dart.util.AstFactory.emptyFunctionBody;
import static com.google.dart.java2dart.util.AstFactory.fieldFormalParameter;
import static com.google.dart.java2dart.util.AstFactory.propertyAccess;
import static com.google.dart.java2dart.util.TokenFactory.token;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link SemanticProcessor} for converting <code>getX()</code> or <code>setX(x)</code> into getter
 * or setter.
 */
public class PropertySemanticProcessor extends SemanticProcessor {
  private static class FieldPropertyInfo {
    final ClassDeclaration clazz;
    final String name;
    MethodDeclaration getter;
    MethodDeclaration setter;
    VariableDeclaration getterField;
    VariableDeclaration setterField;
    VariableDeclaration field;
    SimpleIdentifier fieldName;
    List<SimpleIdentifier> fieldAssignmentReferences;

    public FieldPropertyInfo(ClassDeclaration clazz, String name) {
      this.clazz = clazz;
      this.name = name;
    }
  }

  public static final String KEY_ORIGINAL_PARAMETER = "original-formal-parameter";

  private static int getNumConstructors(ClassDeclaration classDeclaration) {
    int numConstructors = 0;
    for (ClassMember member : classDeclaration.getMembers()) {
      if (member instanceof ConstructorDeclaration) {
        numConstructors++;
      }
    }
    return numConstructors;
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

  private static boolean isValidFieldProperty(FieldPropertyInfo property) {
    // we need getter
    VariableDeclaration getField = property.getterField;
    if (property.getter == null || getField == null) {
      return false;
    }
    // setter should be valid
    VariableDeclaration setField = property.setterField;
    if (property.setter != null && setField == null) {
      return false;
    }
    // if there are both getter and setter, their fields should be the same
    if (getField != null && setField != null && getField != setField) {
      return false;
    }
    property.field = getField;
    property.fieldName = property.field.getName();
    // OK
    return true;
  }

  private static boolean isValidSetterType(TypeName type) {
    return type.getName().getName().equals("void");
  }

  /**
   * Checks if {@link CompilationUnit} declares field with name "onlyBasicGettersSetters", removes
   * it and return {@code true}.
   */
  private static boolean onlyBasicGettersSetters(CompilationUnit unit) {
    final AtomicBoolean result = new AtomicBoolean();
    unit.accept(new RecursiveAstVisitor<Void>() {
      @Override
      public Void visitVariableDeclaration(VariableDeclaration node) {
        if (node.getName().getName().equals("_onlyBasicGettersSetters")) {
          FieldDeclaration fieldDeclaration = (FieldDeclaration) node.getParent().getParent();
          ClassDeclaration clazz = (ClassDeclaration) fieldDeclaration.getParent();
          clazz.getMembers().remove(fieldDeclaration);
          result.set(true);
        }
        return null;
      }
    });
    return result.get();
  }

  /**
   * Removes the given {@link ClassMember} from its parent {@link ClassDeclaration}.
   */
  private static void removeClassMember(ClassMember member) {
    ClassDeclaration clazz = (ClassDeclaration) member.getParent();
    List<ClassMember> members = clazz.getMembers();
    members.remove(member);
  }

  public PropertySemanticProcessor(Context context) {
    super(context);
  }

  public void convertToField(FieldPropertyInfo property) {
    MethodDeclaration getter = property.getter;
    MethodDeclaration setter = property.setter;
    VariableDeclaration field = property.field;
    SimpleIdentifier fieldName = property.fieldName;
    int numConstructors = getNumConstructors(property.clazz);
    // analyze field assignments
    boolean canBeFinal;
    {
      getAssignmentReferencesInConstructors(property);
      // analyze constructor assignments
      List<SimpleIdentifier> references = property.fieldAssignmentReferences;
      canBeFinal = references != null && references.size() == numConstructors;
      // if there are no setter and we cannot make the field final, then keep getter/setter
      if (setter == null && !canBeFinal) {
        return;
      }
      // convert to "this.property" in constructors
      if (references != null) {
        convertToFieldFormalInitializers(property, references);
      }
    }
    // update field
    {
      context.renameIdentifier(fieldName, property.name);
      // mark "final" if no writes
      boolean readOnly = isReadOnlyField(fieldName);
      if (readOnly) {
        ((VariableDeclarationList) field.getParent()).setKeyword(token(Keyword.FINAL));
        if (canBeFinal && numConstructors != 0) {
          field.setInitializer(null);
        }
      }
    }
    // remove getter, update field
    if (getter != null) {
      removeClassMember(getter);
    }
    // remove setter
    if (setter != null) {
      removeClassMember(setter);
    }
    // now these are field references
    {
      IBinding fieldBinding = context.getNodeBinding(fieldName);
      replaceMethodReferencesWithFieldBindings(getter, fieldBinding);
      replaceMethodReferencesWithFieldBindings(setter, fieldBinding);
    }
  }

  public void convertToFields(CompilationUnit unit) {
    final Set<IBinding> overriddenMethods = getOverriddenMethods(unit);
    unit.accept(new RecursiveAstVisitor<Void>() {
      @Override
      public Void visitClassDeclaration(ClassDeclaration node) {
        List<FieldPropertyInfo> properties = getFieldProperties(node);
        removeOverriddenProperties(overriddenMethods, properties);
        for (FieldPropertyInfo property : properties) {
          if (!isValidFieldProperty(property)) {
            continue;
          }
          convertToField(property);
        }
        return null;
      }
    });
  }

  @Override
  public void process(CompilationUnit unit) {
    convertGettersSetters(unit);
    if (onlyBasicGettersSetters(unit)) {
      return;
    }
    convertToFields(unit);
  }

  /**
   * Converts every <code>getX()</code> into <code>get x</code> and <code>setX(v)</code> into
   * <code>set x(v)</code> with corresponding replacement of {@link MethodInvocation} into
   * {@link PropertyAccess} in references.
   */
  private void convertGettersSetters(CompilationUnit unit) {
    unit.accept(new RecursiveAstVisitor<Void>() {
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
  }

  private void convertToFieldFormalInitializers(FieldPropertyInfo property,
      List<SimpleIdentifier> references) {
    for (SimpleIdentifier reference : references) {
      // prepare constructor
      ConstructorDeclaration constructor = reference.getAncestor(ConstructorDeclaration.class);
      // convert "formal parameter" into "field initializing formal parameter"
      List<FormalParameter> parameters = constructor.getParameters().getParameters();
      for (FormalParameter parameter : parameters) {
        if (parameter instanceof SimpleFormalParameter) {
          SimpleFormalParameter simpleParameter = (SimpleFormalParameter) parameter;
          SimpleIdentifier parameterName = simpleParameter.getIdentifier();
          if (parameterName.getName().equals(property.name)) {
            FormalParameter ffp = fieldFormalParameter(null, null, parameterName);
            ffp.setProperty(KEY_ORIGINAL_PARAMETER, parameter);
            replaceNode(simpleParameter, ffp);
            break;
          }
        }
      }
      // remove assignment statement
      Statement statement = (Statement) reference.getParent().getParent().getParent();
      List<Statement> statements = removeBlockStatement(statement);
      if (statements.isEmpty()) {
        constructor.setBody(emptyFunctionBody());
      }
    }
  }

  /**
   * If the given {@link SimpleIdentifier} is the field reference and:
   * <ul>
   * <li>The field is assigned only in constructors.</li>
   * <li>The field is assigned only once in each constructor.</li>
   * <li>The assignment looks like <code>this.name = name;</code> statement.</li>
   * </ul>
   * Then returns all references of the field in these simple assignments. Otherwise returns
   * {@code null}.
   */
  private void getAssignmentReferencesInConstructors(FieldPropertyInfo property) {
    Set<ConstructorDeclaration> constructorsWithAssignments = Sets.newHashSet();
    List<SimpleIdentifier> assignReferences = Lists.newArrayList();
    List<SimpleIdentifier> references = context.getReferences(property.fieldName);
    for (SimpleIdentifier reference : references) {
      // not in setter context
      if (!reference.inSetterContext()) {
        continue;
      }
      // should be in constructor
      ConstructorDeclaration constructor = reference.getAncestor(ConstructorDeclaration.class);
      if (constructor == null) {
        if (reference.getAncestor(MethodDeclaration.class) == property.setter) {
          continue;
        }
        return;
      }
      // may be more than one assignment
      if (constructorsWithAssignments.contains(constructor)) {
        return;
      }
      constructorsWithAssignments.add(constructor);
      // should be "this.name"
      if (!(reference.getParent() instanceof PropertyAccess)) {
        return;
      }
      PropertyAccess propertyAccess = (PropertyAccess) reference.getParent();
      if (!(propertyAccess.getTarget() instanceof ThisExpression)) {
        return;
      }
      // should be assignment
      if (!(propertyAccess.getParent() instanceof AssignmentExpression)) {
        return;
      }
      AssignmentExpression assignment = (AssignmentExpression) propertyAccess.getParent();
      // should be "this.name = _name"
      if (assignment.getRightHandSide() instanceof SimpleIdentifier) {
        SimpleIdentifier right = (SimpleIdentifier) assignment.getRightHandSide();
        if (isReferencedInSuperConstructorInvocation(right)) {
          continue;
        }
        String rightName = right.getName();
        String leftName = reference.getName();
        if (!leftName.equals("_" + rightName)) {
          return;
        }
      } else {
        return;
      }
      // should be simple, just in statement
      if (!(assignment.getParent() instanceof ExpressionStatement)) {
        return;
      }
      // should be just "="
      if (assignment.getOperator().getType() != TokenType.EQ) {
        return;
      }
      // OK, valid assignment reference
      assignReferences.add(reference);
    }
    // OK, no other assignments found
    property.fieldAssignmentReferences = assignReferences;
  }

  private List<FieldPropertyInfo> getFieldProperties(final ClassDeclaration classDeclaration) {
    final Map<String, FieldPropertyInfo> properties = Maps.newHashMap();
    classDeclaration.accept(new RecursiveAstVisitor<Void>() {
      @Override
      public Void visitMethodDeclaration(MethodDeclaration node) {
        // we need getter or setter
        if (!node.isGetter() && !node.isSetter()) {
          return null;
        }
        // prepare property
        String propertyName = node.getName().getName();
        FieldPropertyInfo property = getProperty(propertyName);
        // getter
        if (node.isGetter()) {
          processGetter(property, node);
        }
        // setter
        if (node.isSetter()) {
          processSetter(property, node);
        }
        // done
        return null;
      }

      private FieldPropertyInfo getProperty(String name) {
        FieldPropertyInfo property = properties.get(name);
        if (property == null) {
          property = new FieldPropertyInfo(classDeclaration, name);
          properties.put(name, property);
        }
        return property;
      }

      private boolean isFieldWithPropertyName(String fieldName, String propertyName) {
        String prefix = "_" + propertyName;
        if (!fieldName.startsWith(prefix)) {
          return false;
        }
        String rest = fieldName.substring(prefix.length());
        if (!StringUtils.isNumericSpace(rest)) {
          return false;
        }
        return true;
      }

      private void processGetter(FieldPropertyInfo property, MethodDeclaration node) {
        property.getter = node;
        if (node.getBody() instanceof ExpressionFunctionBody) {
          ExpressionFunctionBody body = (ExpressionFunctionBody) node.getBody();
          Expression expression = body.getExpression();
          if (expression instanceof SimpleIdentifier) {
            SimpleIdentifier identifier = (SimpleIdentifier) expression;
            String fieldName = identifier.getName();
            // may be not a local field
            VariableDeclaration field = classDeclaration.getField(fieldName);
            if (field == null) {
              return;
            }
            // maybe field doesn't have the same name as the property
            if (!isFieldWithPropertyName(fieldName, property.name)) {
              return;
            }
            // OK, remember the field
            property.getterField = field;
          }
        }
      }

      private void processSetter(FieldPropertyInfo property, MethodDeclaration node) {
        property.setter = node;
        // block body
        if (!(node.getBody() instanceof BlockFunctionBody)) {
          return;
        }
        // single statement
        BlockFunctionBody body = (BlockFunctionBody) node.getBody();
        List<Statement> statements = body.getBlock().getStatements();
        if (statements.size() != 1) {
          return;
        }
        Statement statement = statements.get(0);
        // prepare expression
        if (!(statement instanceof ExpressionStatement)) {
          return;
        }
        Expression expression = ((ExpressionStatement) statement).getExpression();
        // should be assignment
        if (!(expression instanceof AssignmentExpression)) {
          return;
        }
        AssignmentExpression assignment = (AssignmentExpression) expression;
        // simple assignment
        if (assignment.getOperator().getType() != TokenType.EQ) {
          return;
        }
        // RHS should be just parameter name
        Expression rhs = assignment.getRightHandSide();
        if (!(rhs instanceof SimpleIdentifier)) {
          return;
        }
        SimpleIdentifier rhsName = (SimpleIdentifier) rhs;
        List<FormalParameter> parameters = node.getParameters().getParameters();
        String parameterName = ((SimpleFormalParameter) parameters.get(0)).getIdentifier().getName();
        if (!rhsName.getName().equals(parameterName)) {
          return;
        }
        // LHS
        Expression lhs = assignment.getLeftHandSide();
        if (lhs instanceof PropertyAccess) {
          PropertyAccess access = (PropertyAccess) lhs;
          if (access.getTarget() instanceof ThisExpression) {
            String fieldName = access.getPropertyName().getName();
            // may be not a local field
            VariableDeclaration field = classDeclaration.getField(fieldName);
            if (field == null) {
              return;
            }
            // may be field has not the same name as the property
            if (!isFieldWithPropertyName(fieldName, property.name)) {
              return;
            }
            // OK, remember the field
            property.setterField = field;
          }
        }
      }
    });
    return Lists.newArrayList(properties.values());
  }

  private Set<IBinding> getOverriddenMethods(CompilationUnit unit) {
    final Set<IBinding> overriddenMethods = Sets.newHashSet();
    unit.accept(new RecursiveAstVisitor<Void>() {
      @Override
      public Void visitMethodDeclaration(MethodDeclaration node) {
        IMethodBinding binding = (IMethodBinding) context.getNodeBinding(node);
        if (binding != null) {
          IMethodBinding superBinding = Bindings.findOverriddenMethod(binding, true);
          if (superBinding != null) {
            overriddenMethods.add(superBinding);
          }
        }
        return null;
      }
    });
    return overriddenMethods;
  }

  private boolean isOverridden(Set<IBinding> overriddenMethods, MethodDeclaration method) {
    if (method == null) {
      return false;
    }
    IBinding binding = context.getNodeBinding(method);
    return overriddenMethods.contains(binding);
  }

  private boolean isReadOnlyField(SimpleIdentifier fieldName) {
    List<SimpleIdentifier> references = context.getReferences(fieldName);
    for (SimpleIdentifier reference : references) {
      if (reference.inSetterContext()) {
        return false;
      }
    }
    return true;
  }

  /**
   * @return {@code true} if the given parameter is referenced
   *         {@link RedirectingConstructorInvocation} or {@link SuperConstructorInvocation}.
   */
  private boolean isReferencedInSuperConstructorInvocation(SimpleIdentifier parameter) {
    List<SimpleIdentifier> references = context.getReferences(parameter);
    for (SimpleIdentifier reference : references) {
      if (reference.getAncestor(SuperConstructorInvocation.class) != null) {
        return true;
      }
    }
    return false;
  }

  /**
   * Removes given {@link Statement} from its {@link Block}.
   */
  private NodeList<Statement> removeBlockStatement(Statement statement) {
    NodeList<Statement> statements = ((Block) statement.getParent()).getStatements();
    statements.remove(statement);
    context.clearNodes(statement);
    return statements;
  }

  private void removeOverriddenProperties(Set<IBinding> overriddenMethods,
      List<FieldPropertyInfo> properties) {
    for (Iterator<FieldPropertyInfo> I = properties.iterator(); I.hasNext();) {
      FieldPropertyInfo property = I.next();
      if (isOverridden(overriddenMethods, property.getter)
          || isOverridden(overriddenMethods, property.setter)) {
        I.remove();
      }
    }
  }

  private void replaceMethodReferencesWithFieldBindings(MethodDeclaration method,
      IBinding fieldBinding) {
    if (method == null) {
      return;
    }
    SimpleIdentifier name = method.getName();
    List<SimpleIdentifier> references = context.getReferences(name);
    references = ImmutableList.copyOf(references);
    for (SimpleIdentifier reference : references) {
      context.putReference(reference, fieldBinding, null);
    }
  }
}
