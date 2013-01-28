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
package com.google.dart.engine.internal.resolver;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.AdjacentStrings;
import com.google.dart.engine.ast.ArgumentDefinitionTest;
import com.google.dart.engine.ast.AsExpression;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.BooleanLiteral;
import com.google.dart.engine.ast.CascadeExpression;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.ConditionalExpression;
import com.google.dart.engine.ast.DefaultFormalParameter;
import com.google.dart.engine.ast.DoubleLiteral;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionFunctionBody;
import com.google.dart.engine.ast.FieldFormalParameter;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.FunctionBody;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.FunctionExpressionInvocation;
import com.google.dart.engine.ast.FunctionTypedFormalParameter;
import com.google.dart.engine.ast.IndexExpression;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.IntegerLiteral;
import com.google.dart.engine.ast.IsExpression;
import com.google.dart.engine.ast.ListLiteral;
import com.google.dart.engine.ast.MapLiteral;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NamedExpression;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.NullLiteral;
import com.google.dart.engine.ast.ParenthesizedExpression;
import com.google.dart.engine.ast.PostfixExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.StringInterpolation;
import com.google.dart.engine.ast.SuperExpression;
import com.google.dart.engine.ast.ThisExpression;
import com.google.dart.engine.ast.ThrowExpression;
import com.google.dart.engine.ast.TypeArgumentList;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.visitor.SimpleASTVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.TypeAliasElement;
import com.google.dart.engine.element.TypeVariableElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.internal.element.ParameterElementImpl;
import com.google.dart.engine.internal.element.VariableElementImpl;
import com.google.dart.engine.internal.type.FunctionTypeImpl;
import com.google.dart.engine.internal.type.InterfaceTypeImpl;
import com.google.dart.engine.internal.type.VoidTypeImpl;
import com.google.dart.engine.resolver.ResolverErrorCode;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Instances of the class {@code StaticTypeAnalyzer} perform two type-related tasks. First, they
 * compute the static type of every expression. Second, they look for any static type errors or
 * warnings that might need to be generated. The requirements for the type analyzer are:
 * <ol>
 * <li>Every element that refers to types should be fully populated.
 * <li>Every node representing an expression should be resolved to the Type of the expression.</li>
 * </ol>
 */
public class StaticTypeAnalyzer extends SimpleASTVisitor<Void> {
  /**
   * The resolver driving this participant.
   */
  private ResolverVisitor resolver;

  /**
   * The object providing access to the types defined by the language.
   */
  private TypeProvider typeProvider;

  /**
   * The type representing the class containing the nodes being analyzed, or {@code null} if the
   * nodes are not within a class.
   */
  private InterfaceType thisType;

  /**
   * Initialize a newly created type analyzer.
   * 
   * @param resolver the resolver driving this participant
   */
  public StaticTypeAnalyzer(ResolverVisitor resolver) {
    this.resolver = resolver;
    typeProvider = resolver.getTypeProvider();
  }

  /**
   * Set the type of the class being analyzed to the given type.
   * 
   * @param thisType the type representing the class containing the nodes being analyzed
   */
  public void setThisType(InterfaceType thisType) {
    this.thisType = thisType;
  }

  /**
   * The Dart Language Specification, 12.5: <blockquote>The static type of a string literal is
   * {@code String}.</blockquote>
   */
  @Override
  public Void visitAdjacentStrings(AdjacentStrings node) {
    return recordType(node, typeProvider.getStringType());
  }

  /**
   * The Dart Language Specification, 12.33: <blockquote>The static type of an argument definition
   * test is {@code bool}.</blockquote>
   */
  @Override
  public Void visitArgumentDefinitionTest(ArgumentDefinitionTest node) {
    return recordType(node, typeProvider.getBoolType());
  }

  /**
   * The Dart Language Specification, 12.32: <blockquote>... the cast expression <i>e as T</i> ...
   * <p>
   * It is a static warning if <i>T</i> does not denote a type available in the current lexical
   * scope.
   * <p>
   * The static type of a cast expression <i>e as T</i> is <i>T</i>.</blockquote>
   */
  @Override
  public Void visitAsExpression(AsExpression node) {
    // TODO(brianwilkerson) Decide how to represent an undefined type
    return recordType(node, getType(node.getType()));
  }

  /**
   * The Dart Language Specification, 12.18: <blockquote> ... an assignment <i>a</i> of the form
   * <i>v = e</i> ...
   * <p>
   * It is a static type warning if the static type of <i>e</i> may not be assigned to the static
   * type of <i>v</i>.
   * <p>
   * The static type of the expression <i>v = e</i> is the static type of <i>e</i>.
   * <p>
   * ... an assignment of the form <i>C.v = e</i> ...
   * <p>
   * It is a static type warning if the static type of <i>e</i> may not be assigned to the static
   * type of <i>C.v</i>.
   * <p>
   * The static type of the expression <i>C.v = e</i> is the static type of <i>e</i>.
   * <p>
   * ... an assignment of the form <i>e<sub>1</sub>.v = e<sub>2</sub></i> ...
   * <p>
   * Let <i>T</i> be the static type of <i>e<sub>1</sub></i>. It is a static type warning if
   * <i>T</i> does not have an accessible instance setter named <i>v=</i>. It is a static type
   * warning if the static type of <i>e<sub>2</sub></i> may not be assigned to <i>T</i>.
   * <p>
   * The static type of the expression <i>e<sub>1</sub>.v = e<sub>2</sub></i> is the static type of
   * <i>e<sub>2</sub></i>.
   * <p>
   * ... an assignment of the form <i>e<sub>1</sub>[e<sub>2</sub>] = e<sub>3</sub></i> ...
   * <p>
   * The static type of the expression <i>e<sub>1</sub>[e<sub>2</sub>] = e<sub>3</sub></i> is the
   * static type of <i>e<sub>3</sub></i>.
   * <p>
   * A compound assignment of the form <i>v op= e</i> is equivalent to <i>v = v op e</i>. A compound
   * assignment of the form <i>C.v op= e</i> is equivalent to <i>C.v = C.v op e</i>. A compound
   * assignment of the form <i>e<sub>1</sub>.v op= e<sub>2</sub></i> is equivalent to <i>((x) => x.v
   * = x.v op e<sub>2</sub>)(e<sub>1</sub>)</i> where <i>x</i> is a variable that is not used in
   * <i>e<sub>2</sub></i>. A compound assignment of the form <i>e<sub>1</sub>[e<sub>2</sub>] op=
   * e<sub>3</sub></i> is equivalent to <i>((a, i) => a[i] = a[i] op e<sub>3</sub>)(e<sub>1</sub>,
   * e<sub>2</sub>)</i> where <i>a</i> and <i>i</i> are a variables that are not used in
   * <i>e<sub>3</sub></i>. </blockquote>
   */
  @Override
  public Void visitAssignmentExpression(AssignmentExpression node) {
    TokenType operator = node.getOperator().getType();
    if (operator != TokenType.EQ) {
      return recordReturnType(node, node.getElement());
    }
    Type leftType = getType(node.getLeftHandSide());
    Type rightType = getType(node.getRightHandSide());
    if (!rightType.isAssignableTo(leftType)) {
//      // TODO(brianwilkerson) Report this error
//      resolver.reportError(ResolverErrorCode.?, node.getRightHandSide());
    }
    return recordType(node, rightType);
  }

  /**
   * The Dart Language Specification, 12.20: <blockquote>The static type of a logical boolean
   * expression is {@code bool}.</blockquote>
   * <p>
   * The Dart Language Specification, 12.21:<blockquote>A bitwise expression of the form
   * <i>e<sub>1</sub> op e<sub>2</sub></i> is equivalent to the method invocation
   * <i>e<sub>1</sub>.op(e<sub>2</sub>)</i>. A bitwise expression of the form <i>super op
   * e<sub>2</sub></i> is equivalent to the method invocation
   * <i>super.op(e<sub>2</sub>)</i>.</blockquote>
   * <p>
   * The Dart Language Specification, 12.22: <blockquote>The static type of an equality expression
   * is {@code bool}.</blockquote>
   * <p>
   * The Dart Language Specification, 12.23: <blockquote>A relational expression of the form
   * <i>e<sub>1</sub> op e<sub>2</sub></i> is equivalent to the method invocation
   * <i>e<sub>1</sub>.op(e<sub>2</sub>)</i>. A relational expression of the form <i>super op
   * e<sub>2</sub></i> is equivalent to the method invocation
   * <i>super.op(e<sub>2</sub>)</i>.</blockquote>
   * <p>
   * The Dart Language Specification, 12.24: <blockquote>A shift expression of the form
   * <i>e<sub>1</sub> op e<sub>2</sub></i> is equivalent to the method invocation
   * <i>e<sub>1</sub>.op(e<sub>2</sub>)</i>. A shift expression of the form <i>super op
   * e<sub>2</sub></i> is equivalent to the method invocation
   * <i>super.op(e<sub>2</sub>)</i>.</blockquote>
   * <p>
   * The Dart Language Specification, 12.25: <blockquote>An additive expression of the form
   * <i>e<sub>1</sub> op e<sub>2</sub></i> is equivalent to the method invocation
   * <i>e<sub>1</sub>.op(e<sub>2</sub>)</i>. An additive expression of the form <i>super op
   * e<sub>2</sub></i> is equivalent to the method invocation
   * <i>super.op(e<sub>2</sub>)</i>.</blockquote>
   * <p>
   * The Dart Language Specification, 12.26: <blockquote>A multiplicative expression of the form
   * <i>e<sub>1</sub> op e<sub>2</sub></i> is equivalent to the method invocation
   * <i>e<sub>1</sub>.op(e<sub>2</sub>)</i>. A multiplicative expression of the form <i>super op
   * e<sub>2</sub></i> is equivalent to the method invocation
   * <i>super.op(e<sub>2</sub>)</i>.</blockquote>
   */
  @Override
  public Void visitBinaryExpression(BinaryExpression node) {
    TokenType operator = node.getOperator().getType();
    switch (operator) {
      case AMPERSAND_AMPERSAND:
      case BAR_BAR:
      case EQ_EQ:
      case BANG_EQ:
        return recordType(node, typeProvider.getBoolType());
    }
    return recordReturnType(node, node.getElement());
  }

  /**
   * The Dart Language Specification, 12.4: <blockquote>The static type of a boolean literal is
   * {@code bool}.</blockquote>
   */
  @Override
  public Void visitBooleanLiteral(BooleanLiteral node) {
    return recordType(node, typeProvider.getBoolType());
  }

  /**
   * The Dart Language Specification, 12.15.2: <blockquote>A cascaded method invocation expression
   * of the form <i>e..suffix</i> is equivalent to the expression <i>(t) {t.suffix; return
   * t;}(e)</i>.</blockquote>
   */
  @Override
  public Void visitCascadeExpression(CascadeExpression node) {
    return recordType(node, getType(node.getTarget()));
  }

  @Override
  public Void visitCatchClause(CatchClause node) {
    SimpleIdentifier exception = node.getExceptionParameter();
    if (exception != null) {
      // If an 'on' clause is provided the type of the exception parameter is the type in the 'on'
      // clause. Otherwise, the type of the exception parameter is 'Object'.
      TypeName exceptionTypeName = node.getExceptionType();
      Type exceptionType;
      if (exceptionTypeName == null) {
        exceptionType = typeProvider.getObjectType();
      } else {
        exceptionType = getType(exceptionTypeName);
      }
      recordType(exception, exceptionType);
      Element element = exception.getElement();
      if (element instanceof VariableElementImpl) {
        ((VariableElementImpl) element).setType(exceptionType);
      } else {
        // TODO(brianwilkerson) Report the internal error
      }
    }
    SimpleIdentifier stackTrace = node.getStackTraceParameter();
    if (stackTrace != null) {
      recordType(stackTrace, typeProvider.getStackTraceType());
    }
    return null;
  }

  /**
   * The Dart Language Specification, 12.19: <blockquote> ... a conditional expression <i>c</i> of
   * the form <i>e<sub>1</sub> ? e<sub>2</sub> : e<sub>3</sub></i> ...
   * <p>
   * It is a static type warning if the type of e<sub>1</sub> may not be assigned to {@code bool}.
   * <p>
   * The static type of <i>c</i> is the least upper bound of the static type of <i>e<sub>2</sub></i>
   * and the static type of <i>e<sub>3</sub></i>.</blockquote>
   */
  @Override
  public Void visitConditionalExpression(ConditionalExpression node) {
    Type conditionType = getType(node.getCondition());
    if (conditionType != null && !conditionType.isAssignableTo(typeProvider.getBoolType())) {
      resolver.reportError(ResolverErrorCode.NON_BOOLEAN_CONDITION, node.getCondition());
    }
    // Return the least-upper-bound of the then and else expressions.
    Type thenType = getType(node.getThenExpression());
    Type elseType = getType(node.getElseExpression());
    if (thenType == null) {
      return recordType(node, typeProvider.getDynamicType());
    }
    Type resultType = thenType.getLeastUpperBound(elseType);
    return recordType(node, resultType);
  }

  @Override
  public Void visitDefaultFormalParameter(DefaultFormalParameter node) {
//    Expression defaultValue = node.getDefaultValue();
//    if (defaultValue != null) {
//      Type valueType = getType(defaultValue);
//      Type parameterType = getType(node.getParameter());
//      if (!valueType.isAssignableTo(parameterType)) {
    // TODO(brianwilkerson) Determine whether this is really an error. I can't find in the spec
    // anything that says it is, but a side comment from Gilad states that it should be a static
    // warning.
//        resolver.reportError(ResolverErrorCode.?, defaultValue);
//      }
//    }
    return null;
  }

  /**
   * The Dart Language Specification, 12.3: <blockquote>The static type of a literal double is
   * {@code double}.</blockquote>
   */
  @Override
  public Void visitDoubleLiteral(DoubleLiteral node) {
    return recordType(node, typeProvider.getDoubleType());
  }

  @Override
  public Void visitFieldFormalParameter(FieldFormalParameter node) {
    Type declaredType;
    TypeName typeName = node.getType();
    if (typeName == null) {
      declaredType = typeProvider.getDynamicType();
    } else {
      declaredType = getType(typeName);
    }
    Element element = node.getIdentifier().getElement();
    if (element instanceof ParameterElement) {
      ((ParameterElementImpl) element).setType(declaredType);
    } else {
      // TODO(brianwilkerson) Report the internal error.
    }
    return null;
  }

  /**
   * The Dart Language Specification, 12.9: <blockquote>The static type of a function literal of the
   * form <i>(T<sub>1</sub> a<sub>1</sub>, &hellip;, T<sub>n</sub> a<sub>n</sub>, [T<sub>n+1</sub>
   * x<sub>n+1</sub> = d1, &hellip;, T<sub>n+k</sub> x<sub>n+k</sub> = dk]) => e</i> is
   * <i>(T<sub>1</sub>, &hellip;, Tn, [T<sub>n+1</sub> x<sub>n+1</sub>, &hellip;, T<sub>n+k</sub>
   * x<sub>n+k</sub>]) &rarr; T<sub>0</sub></i>, where <i>T<sub>0</sub></i> is the static type of
   * <i>e</i>. In any case where <i>T<sub>i</sub>, 1 &lt;= i &lt;= n</i>, is not specified, it is
   * considered to have been specified as dynamic.
   * <p>
   * The static type of a function literal of the form <i>(T<sub>1</sub> a<sub>1</sub>, &hellip;,
   * T<sub>n</sub> a<sub>n</sub>, {T<sub>n+1</sub> x<sub>n+1</sub> : d1, &hellip;, T<sub>n+k</sub>
   * x<sub>n+k</sub> : dk}) => e</i> is <i>(T<sub>1</sub>, &hellip;, T<sub>n</sub>, {T<sub>n+1</sub>
   * x<sub>n+1</sub>, &hellip;, T<sub>n+k</sub> x<sub>n+k</sub>}) &rarr; T<sub>0</sub></i>, where
   * <i>T<sub>0</sub></i> is the static type of <i>e</i>. In any case where <i>T<sub>i</sub>, 1
   * &lt;= i &lt;= n</i>, is not specified, it is considered to have been specified as dynamic.
   * <p>
   * The static type of a function literal of the form <i>(T<sub>1</sub> a<sub>1</sub>, &hellip;,
   * T<sub>n</sub> a<sub>n</sub>, [T<sub>n+1</sub> x<sub>n+1</sub> = d1, &hellip;, T<sub>n+k</sub>
   * x<sub>n+k</sub> = dk]) {s}</i> is <i>(T<sub>1</sub>, &hellip;, T<sub>n</sub>, [T<sub>n+1</sub>
   * x<sub>n+1</sub>, &hellip;, T<sub>n+k</sub> x<sub>n+k</sub>]) &rarr; dynamic</i>. In any case
   * where <i>T<sub>i</sub>, 1 &lt;= i &lt;= n</i>, is not specified, it is considered to have been
   * specified as dynamic.
   * <p>
   * The static type of a function literal of the form <i>(T<sub>1</sub> a<sub>1</sub>, &hellip;,
   * T<sub>n</sub> a<sub>n</sub>, {T<sub>n+1</sub> x<sub>n+1</sub> : d1, &hellip;, T<sub>n+k</sub>
   * x<sub>n+k</sub> : dk}) {s}</i> is <i>(T<sub>1</sub>, &hellip;, T<sub>n</sub>, {T<sub>n+1</sub>
   * x<sub>n+1</sub>, &hellip;, T<sub>n+k</sub> x<sub>n+k</sub>}) &rarr; dynamic</i>. In any case
   * where <i>T<sub>i</sub>, 1 &lt;= i &lt;= n</i>, is not specified, it is considered to have been
   * specified as dynamic.</blockquote>
   */
  @Override
  public Void visitFunctionExpression(FunctionExpression node) {
    return recordType(node, createFunctionType(computeReturnType(node), node.getParameters()));
  }

  /**
   * The Dart Language Specification, 12.14.4: <blockquote>A function expression invocation <i>i</i>
   * has the form <i>e<sub>f</sub>(a<sub>1</sub>, &hellip;, a<sub>n</sub>, x<sub>n+1</sub>:
   * a<sub>n+1</sub>, &hellip;, x<sub>n+k</sub>: a<sub>n+k</sub>)</i>, where <i>e<sub>f</sub></i> is
   * an expression.
   * <p>
   * It is a static type warning if the static type <i>F</i> of <i>e<sub>f</sub></i> may not be
   * assigned to a function type.
   * <p>
   * If <i>F</i> is not a function type, the static type of <i>i</i> is dynamic. Otherwise the
   * static type of <i>i</i> is the declared return type of <i>F</i>.</blockquote>
   */
  @Override
  public Void visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    return recordReturnType(node, node.getElement());
  }

  @Override
  public Void visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) {
    Element element = node.getIdentifier().getElement();
    if (!(element instanceof ParameterElementImpl)) {
      // TODO(brianwilkerson) Report the internal error
      return null;
    }
    Type returnType;
    TypeName returnTypeNode = node.getReturnType();
    if (returnTypeNode == null) {
      returnType = typeProvider.getDynamicType();
    } else {
      returnType = getType(returnTypeNode);
    }
    ParameterElementImpl parameter = (ParameterElementImpl) element;
    parameter.setType(createFunctionType(returnType, node.getParameters()));
    return null;
  }

  /**
   * The Dart Language Specification, 12.29: <blockquote>An assignable expression of the form
   * <i>e<sub>1</sub>[e<sub>2</sub>]</i> is evaluated as a method invocation of the operator method
   * <i>[]</i> on <i>e<sub>1</sub></i> with argument <i>e<sub>2</sub></i>.</blockquote>
   */
  @Override
  public Void visitIndexExpression(IndexExpression node) {
    return recordReturnType(node, node.getElement());
  }

  /**
   * The Dart Language Specification, 12.11.1: <blockquote>The static type of a new expression of
   * either the form <i>new T.id(a<sub>1</sub>, &hellip;, a<sub>n</sub>)</i> or the form <i>new
   * T(a<sub>1</sub>, &hellip;, a<sub>n</sub>)</i> is <i>T</i>.</blockquote>
   * <p>
   * The Dart Language Specification, 12.11.2: <blockquote>The static type of a constant object
   * expression of either the form <i>const T.id(a<sub>1</sub>, &hellip;, a<sub>n</sub>)</i> or the
   * form <i>const T(a<sub>1</sub>, &hellip;, a<sub>n</sub>)</i> is <i>T</i>. </blockquote>
   */
  @Override
  public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
    return recordReturnType(node, node.getElement());
  }

  /**
   * The Dart Language Specification, 12.3: <blockquote>The static type of an integer literal is
   * {@code int}.</blockquote>
   */
  @Override
  public Void visitIntegerLiteral(IntegerLiteral node) {
    return recordType(node, typeProvider.getIntType());
  }

  /**
   * The Dart Language Specification, 12.31: <blockquote>It is a static warning if <i>T</i> does not
   * denote a type available in the current lexical scope.
   * <p>
   * The static type of an is-expression is {@code bool}.</blockquote>
   */
  @Override
  public Void visitIsExpression(IsExpression node) {
    // TODO(brianwilkerson) Decide how to represent an undefined type
    return recordType(node, typeProvider.getBoolType());
  }

  /**
   * The Dart Language Specification, 12.6: <blockquote>The static type of a list literal of the
   * form <i><b>const</b> &lt;E&gt;[e<sub>1</sub>, &hellip;, e<sub>n</sub>]</i> or the form
   * <i>&lt;E&gt;[e<sub>1</sub>, &hellip;, e<sub>n</sub>]</i> is {@code List&lt;E&gt;}. The static
   * type a list literal of the form <i><b>const</b> [e<sub>1</sub>, &hellip;, e<sub>n</sub>]</i> or
   * the form <i>[e<sub>1</sub>, &hellip;, e<sub>n</sub>]</i> is {@code List&lt;dynamic&gt;}
   * .</blockquote>
   */
  @Override
  public Void visitListLiteral(ListLiteral node) {
    TypeArgumentList typeArguments = node.getTypeArguments();
    if (typeArguments != null) {
      NodeList<TypeName> arguments = typeArguments.getArguments();
      if (arguments != null && arguments.size() == 1) {
        TypeName argumentType = arguments.get(0);
        // Get the type defined by the type name and use it as a substitution for the listType's
        // type parameter.
        return recordType(
            node,
            typeProvider.getListType().substitute(new Type[] {getType(argumentType)}));
      }
    }
    return recordType(
        node,
        typeProvider.getListType().substitute(new Type[] {typeProvider.getDynamicType()}));
  }

  /**
   * The Dart Language Specification, 12.7: <blockquote>The static type of a map literal of the form
   * <i><b>const</b> &lt;String, V&gt; {k<sub>1</sub>:e<sub>1</sub>, &hellip;,
   * k<sub>n</sub>:e<sub>n</sub>}</i> or the form <i>&lt;String, V&gt; {k<sub>1</sub>:e<sub>1</sub>,
   * &hellip;, k<sub>n</sub>:e<sub>n</sub>}</i> is {@code Map&lt;String, V&gt;}. The static type a
   * map literal of the form <i><b>const</b> {k<sub>1</sub>:e<sub>1</sub>, &hellip;,
   * k<sub>n</sub>:e<sub>n</sub>}</i> or the form <i>{k<sub>1</sub>:e<sub>1</sub>, &hellip;,
   * k<sub>n</sub>:e<sub>n</sub>}</i> is {@code Map&lt;String, dynamic&gt;}.
   * <p>
   * It is a compile-time error if the first type argument to a map literal is not
   * <i>String</i>.</blockquote>
   */
  @Override
  public Void visitMapLiteral(MapLiteral node) {
    TypeArgumentList typeArguments = node.getTypeArguments();
    if (typeArguments != null) {
      NodeList<TypeName> arguments = typeArguments.getArguments();
      if (arguments != null && arguments.size() == 2) {
        TypeName keyType = arguments.get(0);
        if (!keyType.equals(typeProvider.getStringType())) {
          // TODO(brianwilkerson) Report the error.
//          resolver.reportError(ResolverErrorCode.?, keyType);
        }
        TypeName valueType = arguments.get(1);
        // Get the type defined by the type name and use it as a substitution for the mapType's
        // type parameter.
        return recordType(
            node,
            typeProvider.getMapType().substitute(
                new Type[] {typeProvider.getStringType(), getType(valueType)}));
      }
    }
    return recordType(
        node,
        typeProvider.getMapType().substitute(
            new Type[] {typeProvider.getStringType(), typeProvider.getDynamicType()}));
  }

  /**
   * The Dart Language Specification, 12.15.1: <blockquote>An ordinary method invocation <i>i</i>
   * has the form <i>o.m(a<sub>1</sub>, &hellip;, a<sub>n</sub>, x<sub>n+1</sub>: a<sub>n+1</sub>,
   * &hellip;, x<sub>n+k</sub>: a<sub>n+k</sub>)</i>.
   * <p>
   * Let <i>T</i> be the static type of <i>o</i>. It is a static type warning if <i>T</i> does not
   * have an accessible instance member named <i>m</i>. If <i>T.m</i> exists, it is a static warning
   * if the type <i>F</i> of <i>T.m</i> may not be assigned to a function type.
   * <p>
   * If <i>T.m</i> does not exist, or if <i>F</i> is not a function type, the static type of
   * <i>i</i> is dynamic. Otherwise the static type of <i>i</i> is the declared return type of
   * <i>F</i>.</blockquote>
   * <p>
   * The Dart Language Specification, 11.15.3: <blockquote>A static method invocation <i>i</i> has
   * the form <i>C.m(a<sub>1</sub>, &hellip;, a<sub>n</sub>, x<sub>n+1</sub>: a<sub>n+1</sub>,
   * &hellip;, x<sub>n+k</sub>: a<sub>n+k</sub>)</i>.
   * <p>
   * It is a static type warning if the type <i>F</i> of <i>C.m</i> may not be assigned to a
   * function type.
   * <p>
   * If <i>F</i> is not a function type, or if <i>C.m</i> does not exist, the static type of i is
   * dynamic. Otherwise the static type of <i>i</i> is the declared return type of
   * <i>F</i>.</blockquote>
   * <p>
   * The Dart Language Specification, 11.15.4: <blockquote>A super method invocation <i>i</i> has
   * the form <i>super.m(a<sub>1</sub>, &hellip;, a<sub>n</sub>, x<sub>n+1</sub>: a<sub>n+1</sub>,
   * &hellip;, x<sub>n+k</sub>: a<sub>n+k</sub>)</i>.
   * <p>
   * It is a static type warning if <i>S</i> does not have an accessible instance member named m. If
   * <i>S.m</i> exists, it is a static warning if the type <i>F</i> of <i>S.m</i> may not be
   * assigned to a function type.
   * <p>
   * If <i>S.m</i> does not exist, or if <i>F</i> is not a function type, the static type of
   * <i>i</i> is dynamic. Otherwise the static type of <i>i</i> is the declared return type of
   * <i>F</i>.</blockquote>
   */
  @Override
  public Void visitMethodInvocation(MethodInvocation node) {
    return recordReturnType(node, node.getMethodName().getElement());
  }

  @Override
  public Void visitNamedExpression(NamedExpression node) {
    return recordType(node, getType(node.getExpression()));
  }

  /**
   * The Dart Language Specification, 12.2: <blockquote>The static type of {@code null} is bottom.
   * </blockquote>
   */
  @Override
  public Void visitNullLiteral(NullLiteral node) {
    return recordType(node, typeProvider.getBottomType());
  }

  @Override
  public Void visitParenthesizedExpression(ParenthesizedExpression node) {
    return recordType(node, getType(node.getExpression()));
  }

  /**
   * The Dart Language Specification, 12.28: <blockquote>A postfix expression of the form
   * <i>v++</i>, where <i>v</i> is an identifier, is equivalent to <i>(){var r = v; v = r + 1;
   * return r}()</i>.
   * <p>
   * A postfix expression of the form <i>C.v++</i> is equivalent to <i>(){var r = C.v; C.v = r + 1;
   * return r}()</i>.
   * <p>
   * A postfix expression of the form <i>e1.v++</i> is equivalent to <i>(x){var r = x.v; x.v = r +
   * 1; return r}(e1)</i>.
   * <p>
   * A postfix expression of the form <i>e1[e2]++</i> is equivalent to <i>(a, i){var r = a[i]; a[i]
   * = r + 1; return r}(e1, e2)</i>
   * <p>
   * A postfix expression of the form <i>v--</i>, where <i>v</i> is an identifier, is equivalent to
   * <i>(){var r = v; v = r - 1; return r}()</i>.
   * <p>
   * A postfix expression of the form <i>C.v--</i> is equivalent to <i>(){var r = C.v; C.v = r - 1;
   * return r}()</i>.
   * <p>
   * A postfix expression of the form <i>e1.v--</i> is equivalent to <i>(x){var r = x.v; x.v = r -
   * 1; return r}(e1)</i>.
   * <p>
   * A postfix expression of the form <i>e1[e2]--</i> is equivalent to <i>(a, i){var r = a[i]; a[i]
   * = r - 1; return r}(e1, e2)</i></blockquote>
   */
  @Override
  public Void visitPostfixExpression(PostfixExpression node) {
    return recordType(node, getType(node.getOperand()));
  }

  /**
   * See {@link #visitSimpleIdentifier(SimpleIdentifier)}.
   */
  @Override
  public Void visitPrefixedIdentifier(PrefixedIdentifier node) {
    // TODO Implement this
    return recordType(node, typeProvider.getDynamicType());
  }

  /**
   * The Dart Language Specification, 12.27: <blockquote>A unary expression <i>u</i> of the form
   * <i>op e</i> is equivalent to a method invocation <i>expression e.op()</i>. An expression of the
   * form <i>op super</i> is equivalent to the method invocation <i>super.op()<i>.</blockquote>
   */
  @Override
  public Void visitPrefixExpression(PrefixExpression node) {
    TokenType operator = node.getOperator().getType();
    if (operator == TokenType.BANG) {
      return recordType(node, typeProvider.getBoolType());
    }
    // The other cases are equivalent to invoking a method.
    return recordReturnType(node, node.getElement());
  }

  /**
   * The Dart Language Specification, 12.13: <blockquote> Property extraction allows for a member of
   * an object to be concisely extracted from the object. If <i>o</i> is an object, and if <i>m</i>
   * is the name of a method member of <i>o</i>, then
   * <ul>
   * <li><i>o.m</i> is defined to be equivalent to: <i>(r<sub>1</sub>, &hellip;, r<sub>n</sub>,
   * {p<sub>1</sub> : d<sub>1</sub>, &hellip;, p<sub>k</sub> : d<sub>k</sub>}){return
   * o.m(r<sub>1</sub>, &hellip;, r<sub>n</sub>, p<sub>1</sub>: p<sub>1</sub>, &hellip;,
   * p<sub>k</sub>: p<sub>k</sub>);}</i> if <i>m</i> has required parameters <i>r<sub>1</sub>,
   * &hellip;, r<sub>n</sub></i>, and named parameters <i>p<sub>1</sub> &hellip; p<sub>k</sub></i>
   * with defaults <i>d<sub>1</sub>, &hellip;, d<sub>k</sub></i>.</li>
   * <li><i>(r<sub>1</sub>, &hellip;, r<sub>n</sub>, [p<sub>1</sub> = d<sub>1</sub>, &hellip;,
   * p<sub>k</sub> = d<sub>k</sub>]){return o.m(r<sub>1</sub>, &hellip;, r<sub>n</sub>,
   * p<sub>1</sub>, &hellip;, p<sub>k</sub>);}</i> if <i>m</i> has required parameters
   * <i>r<sub>1</sub>, &hellip;, r<sub>n</sub></i>, and optional positional parameters
   * <i>p<sub>1</sub> &hellip; p<sub>k</sub></i> with defaults <i>d<sub>1</sub>, &hellip;,
   * d<sub>k</sub></i>.</li>
   * </ul>
   * Otherwise, if <i>m</i> is the name of a getter member of <i>o</i> (declared implicitly or
   * explicitly) then <i>o.m</i> evaluates to the result of invoking the getter. </blockquote>
   * <p>
   * The Dart Language Specification, 12.17: <blockquote> ... a getter invocation <i>i</i> of the
   * form <i>e.m</i> ...
   * <p>
   * Let <i>T</i> be the static type of <i>e</i>. It is a static type warning if <i>T</i> does not
   * have a getter named <i>m</i>.
   * <p>
   * The static type of <i>i</i> is the declared return type of <i>T.m</i>, if <i>T.m</i> exists;
   * otherwise the static type of <i>i</i> is dynamic.
   * <p>
   * ... a getter invocation <i>i</i> of the form <i>C.m</i> ...
   * <p>
   * It is a static warning if there is no class <i>C</i> in the enclosing lexical scope of
   * <i>i</i>, or if <i>C</i> does not declare, implicitly or explicitly, a getter named <i>m</i>.
   * <p>
   * The static type of <i>i</i> is the declared return type of <i>C.m</i> if it exists or dynamic
   * otherwise.
   * <p>
   * ... a top-level getter invocation <i>i</i> of the form <i>m</i>, where <i>m</i> is an
   * identifier ...
   * <p>
   * The static type of <i>i</i> is the declared return type of <i>m</i>.</blockquote>
   */
  @Override
  public Void visitPropertyAccess(PropertyAccess node) {
    // TODO Implement this
    Element element = node.getPropertyName().getElement();
    if (element instanceof MethodElement) {
      return recordType(node, ((MethodElement) element).getType());
    } else if (element instanceof PropertyAccessorElement) {
      PropertyAccessorElement accessor = (PropertyAccessorElement) element;
      if (accessor.isGetter()) {
        if (accessor.getType() == null) {
          // TODO(brianwilkerson) I think this can go away when everything is done because the type
          // of the accessor should never be null.
          return recordType(node, typeProvider.getDynamicType());
        }
        return recordType(node, accessor.getType().getReturnType());
      } else {
        return recordType(node, VoidTypeImpl.getInstance());
      }
    } else {
      // TODO(brianwilkerson) Report this internal error.
    }
    return recordType(node, typeProvider.getDynamicType());
  }

  @Override
  public Void visitSimpleFormalParameter(SimpleFormalParameter node) {
    Type declaredType;
    TypeName typeName = node.getType();
    if (typeName == null) {
      declaredType = typeProvider.getDynamicType();
    } else {
      declaredType = getType(typeName);
    }
    Element element = node.getIdentifier().getElement();
    if (element instanceof ParameterElement) {
      ((ParameterElementImpl) element).setType(declaredType);
    } else {
      // TODO(brianwilkerson) Report the internal error.
    }
    return null;
  }

  /**
   * The Dart Language Specification, 12.30: <blockquote>Evaluation of an identifier expression
   * <i>e</i> of the form <i>id</i> proceeds as follows:
   * <p>
   * Let <i>d</i> be the innermost declaration in the enclosing lexical scope whose name is
   * <i>id</i>. If no such declaration exists in the lexical scope, let <i>d</i> be the declaration
   * of the inherited member named <i>id</i> if it exists.
   * <ul>
   * <li>If <i>d</i> is a class or type alias <i>T</i>, the value of <i>e</i> is the unique instance
   * of class {@code Type} reifying <i>T</i>.
   * <li>If <i>d</i> is a type parameter <i>T</i>, then the value of <i>e</i> is the value of the
   * actual type argument corresponding to <i>T</i> that was passed to the generative constructor
   * that created the current binding of this. We are assured that this is well defined, because if
   * we were in a static member the reference to <i>T</i> would be a compile-time error.
   * <li>If <i>d</i> is a library variable then:
   * <ul>
   * <li>If <i>d</i> is of one of the forms <i>var v = e<sub>i</sub>;</i>, <i>T v =
   * e<sub>i</sub>;</i>, <i>final v = e<sub>i</sub>;</i>, <i>final T v = e<sub>i</sub>;</i>, and no
   * value has yet been stored into <i>v</i> then the initializer expression <i>e<sub>i</sub></i> is
   * evaluated. If, during the evaluation of <i>e<sub>i</sub></i>, the getter for <i>v</i> is
   * referenced, a CyclicInitializationError is thrown. If the evaluation succeeded yielding an
   * object <i>o</i>, let <i>r = o</i>, otherwise let <i>r = null</i>. In any case, <i>r</i> is
   * stored into <i>v</i>. The value of <i>e</i> is <i>r</i>.
   * <li>If <i>d</i> is of one of the forms <i>const v = e;</i> or <i>const T v = e;</i> the result
   * of the getter is the value of the compile time constant <i>e</i>. Otherwise
   * <li><i>e</i> evaluates to the current binding of <i>id</i>.
   * </ul>
   * <li>If <i>d</i> is a local variable or formal parameter then <i>e</i> evaluates to the current
   * binding of <i>id</i>.
   * <li>If <i>d</i> is a static method, top level function or local function then <i>e</i>
   * evaluates to the function defined by <i>d</i>.
   * <li>If <i>d</i> is the declaration of a static variable or static getter declared in class
   * <i>C</i>, then <i>e</i> is equivalent to the getter invocation <i>C.id</i>.
   * <li>If <i>d</i> is the declaration of a top level getter, then <i>e</i> is equivalent to the
   * getter invocation <i>id</i>.
   * <li>Otherwise, if <i>e</i> occurs inside a top level or static function (be it function,
   * method, getter, or setter) or variable initializer, evaluation of e causes a NoSuchMethodError
   * to be thrown.
   * <li>Otherwise <i>e</i> is equivalent to the property extraction <i>this.id</i>.
   * </ul>
   * </blockquote>
   */
  @Override
  public Void visitSimpleIdentifier(SimpleIdentifier node) {
    Element element = node.getElement();
    if (element == null) {
      // The error should have been generated by the element resolver.
      return null;
    } else if (element instanceof ClassElement) {
      if (isTypeName(node)) {
        return recordType(node, ((ClassElement) element).getType());
      }
      return recordType(node, typeProvider.getTypeType());
    } else if (element instanceof TypeVariableElement) {
      if (isTypeName(node)) {
        return recordType(node, ((TypeVariableElement) element).getType());
      }
      return recordType(node, typeProvider.getTypeType());
    } else if (element instanceof TypeAliasElement) {
      return recordType(node, ((TypeAliasElement) element).getType());
    } else if (element instanceof VariableElement) {
      return recordType(node, ((VariableElement) element).getType());
    } else if (element instanceof MethodElement) {
      return recordType(node, ((MethodElement) element).getType());
    } else {
      // TODO(brianwilkerson) Compute and return the equivalent of 'this.id'.
      return recordType(node, typeProvider.getDynamicType());
    }
  }

  /**
   * The Dart Language Specification, 12.5: <blockquote>The static type of a string literal is
   * {@code String}.</blockquote>
   */
  @Override
  public Void visitSimpleStringLiteral(SimpleStringLiteral node) {
    return recordType(node, typeProvider.getStringType());
  }

  /**
   * The Dart Language Specification, 12.5: <blockquote>The static type of a string literal is
   * {@code String}.</blockquote>
   */
  @Override
  public Void visitStringInterpolation(StringInterpolation node) {
    return recordType(node, typeProvider.getStringType());
  }

  @Override
  public Void visitSuperExpression(SuperExpression node) {
    return recordType(
        node,
        thisType == null ? typeProvider.getDynamicType() : thisType.getSuperclass());
  }

  /**
   * The Dart Language Specification, 12.10: <blockquote>The static type of {@code this} is the
   * interface of the immediately enclosing class.</blockquote>
   */
  @Override
  public Void visitThisExpression(ThisExpression node) {
    return recordType(node, thisType);
  }

  /**
   * The Dart Language Specification, 12.8: <blockquote>The static type of a throw expression is
   * bottom.</blockquote>
   */
  @Override
  public Void visitThrowExpression(ThrowExpression node) {
    return recordType(node, typeProvider.getBottomType());
  }

  @Override
  public Void visitTypeName(TypeName node) {
    Type type = getType(node.getName());
    if (type == null) {
      return null;
    }
    TypeArgumentList argumentList = node.getTypeArguments();
    if (argumentList != null) {
      NodeList<TypeName> arguments = argumentList.getArguments();
      int argumentCount = arguments.size();
      int parameterCount = (type instanceof InterfaceType)
          ? ((InterfaceType) type).getTypeArguments().length
          : ((FunctionType) type).getTypeArguments().length;
      if (argumentCount != parameterCount) {
        // TODO(brianwilkerson) Report this error.
//      resolver.reportError(ResolverErrorCode.?, keyType);
      }
      ArrayList<Type> typeArguments = new ArrayList<Type>(argumentCount);
      for (int i = 0; i < argumentCount; i++) {
        Type argumentType = getType(arguments.get(i));
        if (argumentType != null) {
          typeArguments.add(argumentType);
        }
      }
      if (type instanceof InterfaceTypeImpl) {
        InterfaceTypeImpl interfaceType = (InterfaceTypeImpl) type;
        argumentCount = typeArguments.size(); // Recomputed in case any argument type was null
        if (interfaceType.getTypeArguments().length == argumentCount) {
          type = interfaceType.substitute(typeArguments.toArray(new Type[argumentCount]));
        } else {
          // TODO(brianwilkerson) Report this error (unless it already was).
//        resolver.reportError(ResolverErrorCode.?, keyType);
        }
      } else if (type instanceof FunctionTypeImpl) {
        FunctionTypeImpl functionType = (FunctionTypeImpl) type;
        argumentCount = typeArguments.size(); // Recomputed in case any argument type was null
        if (functionType.getTypeArguments().length == argumentCount) {
          type = functionType.substitute(typeArguments.toArray(new Type[argumentCount]));
        } else {
          // TODO(brianwilkerson) Report this error (unless it already was).
//          resolver.reportError(ResolverErrorCode.?, keyType);
        }
      } else {
        // TODO(brianwilkerson) Report this error.
//      resolver.reportError(ResolverErrorCode.?, keyType);
      }
    }
    node.setType(type);
    return null;
  }

  @Override
  public Void visitVariableDeclaration(VariableDeclaration node) {
    Type declaredType;
    TypeName typeName = ((VariableDeclarationList) node.getParent()).getType();
    if (typeName == null) {
      declaredType = typeProvider.getDynamicType();
    } else {
      declaredType = getType(typeName);
    }
    Element element = node.getName().getElement();
    if (element instanceof ParameterElement) {
      ((ParameterElementImpl) element).setType(declaredType);
    } else {
      // TODO(brianwilkerson) Report the internal error.
    }
    return null;
  }

  /**
   * Given a function expression, compute the return type of the function. The return type of
   * functions with a block body is {@code dynamicType}, with an expression body it is the type of
   * the expression.
   * 
   * @param node the function expression whose return type is to be computed
   * @return the return type that was computed
   */
  private Type computeReturnType(FunctionExpression node) {
    FunctionBody body = node.getBody();
    if (body instanceof ExpressionFunctionBody) {
      return getType(((ExpressionFunctionBody) body).getExpression());
    }
    return typeProvider.getDynamicType();
  }

  /**
   * Create a function type representing a function with the given return type and parameters.
   * 
   * @param returnType the return type of the function for which a type is being created
   * @param parameterList the parameter list for the function for which a type is being created
   * @return a function type representing a function with the given return type and parameters
   */
  private FunctionType createFunctionType(Type returnType, FormalParameterList parameterList) {
    ArrayList<Type> normalParameterTypes = new ArrayList<Type>();
    ArrayList<Type> optionalParameterTypes = new ArrayList<Type>();
    LinkedHashMap<String, Type> namedParameterTypes = new LinkedHashMap<String, Type>();
    for (FormalParameter parameter : parameterList.getParameters()) {
      Type parameterType = getType(parameter);
      switch (parameter.getKind()) {
        case REQUIRED:
          normalParameterTypes.add(parameterType);
          break;
        case POSITIONAL:
          optionalParameterTypes.add(parameterType);
          break;
        case NAMED:
          namedParameterTypes.put(parameter.getIdentifier().getName(), parameterType);
          break;
      }
    }

    FunctionTypeImpl functionType = new FunctionTypeImpl((ExecutableElement) null);
    functionType.setNormalParameterTypes(normalParameterTypes.toArray(new Type[normalParameterTypes.size()]));
    functionType.setOptionalParameterTypes(optionalParameterTypes.toArray(new Type[optionalParameterTypes.size()]));
    functionType.setNamedParameterTypes(namedParameterTypes);
    functionType.setReturnType(returnType);
    return functionType;
  }

  /**
   * Return the type of the given expression that is to be used for type analysis.
   * 
   * @param expression the expression whose type is to be returned
   * @return the type of the given expression
   */
  private Type getType(Expression expression) {
    return expression.getStaticType();
  }

  /**
   * Return the type of the given parameter that is to be used for type analysis.
   * 
   * @param parameter the parameter whose type is to be returned
   * @return the type of the given parameter
   */
  private Type getType(FormalParameter parameter) {
    Element element = parameter.getIdentifier().getElement();
    if (element instanceof ParameterElement) {
      return ((ParameterElement) element).getType();
    }
    // TODO(brianwilkerson) Report this internal error
    return typeProvider.getDynamicType();
  }

  /**
   * Return the type represented by the given type name.
   * 
   * @param typeName the type name representing the type to be returned
   * @return the type represented by the type name
   */
  private Type getType(TypeName typeName) {
    return typeName.getType();
  }

  /**
   * Return {@code true} if the given node is being used as the name of a type.
   * 
   * @param node the node being tested
   * @return {@code true} if the given node is being used as the name of a type
   */
  private boolean isTypeName(SimpleIdentifier node) {
    ASTNode parent = node.getParent();
    return parent instanceof TypeName
        || (parent instanceof PrefixedIdentifier && parent.getParent() instanceof TypeName);
  }

  /**
   * Record that the static type of the given node is the return type of the method or function
   * represented by the given element.
   * 
   * @param expression the node whose type is to be recorded
   * @param element the element representing the method or function invoked by the given node
   */
  private Void recordReturnType(Expression expression, Element element) {
    if (element instanceof ExecutableElement) {
      return recordType(expression, ((ExecutableElement) element).getType().getReturnType());
    }
    return recordType(expression, typeProvider.getDynamicType());
  }

  /**
   * Record that the static type of the given node is the given type.
   * 
   * @param expression the node whose type is to be recorded
   * @param type the static type of the node
   */
  private Void recordType(Expression expression, Type type) {
    if (type == null) {
      expression.setStaticType(typeProvider.getDynamicType());
    } else {
      expression.setStaticType(type);
    }
    return null;
  }
}
