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

import com.google.dart.engine.ast.AdjacentStrings;
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.AsExpression;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.BlockFunctionBody;
import com.google.dart.engine.ast.BooleanLiteral;
import com.google.dart.engine.ast.CascadeExpression;
import com.google.dart.engine.ast.ConditionalExpression;
import com.google.dart.engine.ast.DoubleLiteral;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionFunctionBody;
import com.google.dart.engine.ast.FunctionBody;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.FunctionExpressionInvocation;
import com.google.dart.engine.ast.Identifier;
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
import com.google.dart.engine.ast.RethrowExpression;
import com.google.dart.engine.ast.ReturnStatement;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.StringInterpolation;
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.ast.SuperExpression;
import com.google.dart.engine.ast.SymbolLiteral;
import com.google.dart.engine.ast.ThisExpression;
import com.google.dart.engine.ast.ThrowExpression;
import com.google.dart.engine.ast.TypeArgumentList;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.engine.ast.visitor.SimpleAstVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.PropertyInducingElement;
import com.google.dart.engine.element.TypeParameterElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.internal.element.ExecutableElementImpl;
import com.google.dart.engine.internal.type.BottomTypeImpl;
import com.google.dart.engine.internal.type.InterfaceTypeImpl;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.type.TypeParameterType;
import com.google.dart.engine.utilities.general.StringUtilities;

import java.util.HashMap;

/**
 * Instances of the class {@code StaticTypeAnalyzer} perform two type-related tasks. First, they
 * compute the static type of every expression. Second, they look for any static type errors or
 * warnings that might need to be generated. The requirements for the type analyzer are:
 * <ol>
 * <li>Every element that refers to types should be fully populated.
 * <li>Every node representing an expression should be resolved to the Type of the expression.</li>
 * </ol>
 * 
 * @coverage dart.engine.resolver
 */
public class StaticTypeAnalyzer extends SimpleAstVisitor<Void> {
  /**
   * Create a table mapping HTML tag names to the names of the classes (in 'dart:html') that
   * implement those tags.
   * 
   * @return the table that was created
   */
  private static HashMap<String, String> createHtmlTagToClassMap() {
    HashMap<String, String> map = new HashMap<String, String>();
    map.put("a", "AnchorElement");
    map.put("area", "AreaElement");
    map.put("br", "BRElement");
    map.put("base", "BaseElement");
    map.put("body", "BodyElement");
    map.put("button", "ButtonElement");
    map.put("canvas", "CanvasElement");
    map.put("content", "ContentElement");
    map.put("dl", "DListElement");
    map.put("datalist", "DataListElement");
    map.put("details", "DetailsElement");
    map.put("div", "DivElement");
    map.put("embed", "EmbedElement");
    map.put("fieldset", "FieldSetElement");
    map.put("form", "FormElement");
    map.put("hr", "HRElement");
    map.put("head", "HeadElement");
    map.put("h1", "HeadingElement");
    map.put("h2", "HeadingElement");
    map.put("h3", "HeadingElement");
    map.put("h4", "HeadingElement");
    map.put("h5", "HeadingElement");
    map.put("h6", "HeadingElement");
    map.put("html", "HtmlElement");
    map.put("iframe", "IFrameElement");
    map.put("img", "ImageElement");
    map.put("input", "InputElement");
    map.put("keygen", "KeygenElement");
    map.put("li", "LIElement");
    map.put("label", "LabelElement");
    map.put("legend", "LegendElement");
    map.put("link", "LinkElement");
    map.put("map", "MapElement");
    map.put("menu", "MenuElement");
    map.put("meter", "MeterElement");
    map.put("ol", "OListElement");
    map.put("object", "ObjectElement");
    map.put("optgroup", "OptGroupElement");
    map.put("output", "OutputElement");
    map.put("p", "ParagraphElement");
    map.put("param", "ParamElement");
    map.put("pre", "PreElement");
    map.put("progress", "ProgressElement");
    map.put("script", "ScriptElement");
    map.put("select", "SelectElement");
    map.put("source", "SourceElement");
    map.put("span", "SpanElement");
    map.put("style", "StyleElement");
    map.put("caption", "TableCaptionElement");
    map.put("td", "TableCellElement");
    map.put("col", "TableColElement");
    map.put("table", "TableElement");
    map.put("tr", "TableRowElement");
    map.put("textarea", "TextAreaElement");
    map.put("title", "TitleElement");
    map.put("track", "TrackElement");
    map.put("ul", "UListElement");
    map.put("video", "VideoElement");
    return map;
  }

  /**
   * The resolver driving the resolution and type analysis.
   */
  private ResolverVisitor resolver;

  /**
   * The object providing access to the types defined by the language.
   */
  private TypeProvider typeProvider;

  /**
   * The type representing the type 'dynamic'.
   */
  private Type dynamicType;

  /**
   * The type representing the class containing the nodes being analyzed, or {@code null} if the
   * nodes are not within a class.
   */
  private InterfaceType thisType;

  /**
   * The object keeping track of which elements have had their types overridden.
   */
  private TypeOverrideManager overrideManager;

  /**
   * The object keeping track of which elements have had their types promoted.
   */
  private TypePromotionManager promoteManager;

  /**
   * A table mapping {@link ExecutableElement}s to their propagated return types.
   */
  private HashMap<ExecutableElement, Type> propagatedReturnTypes = new HashMap<ExecutableElement, Type>();

  /**
   * A table mapping HTML tag names to the names of the classes (in 'dart:html') that implement
   * those tags.
   */
  private static final HashMap<String, String> HTML_ELEMENT_TO_CLASS_MAP = createHtmlTagToClassMap();

  /**
   * Initialize a newly created type analyzer.
   * 
   * @param resolver the resolver driving this participant
   */
  public StaticTypeAnalyzer(ResolverVisitor resolver) {
    this.resolver = resolver;
    typeProvider = resolver.getTypeProvider();
    dynamicType = typeProvider.getDynamicType();
    overrideManager = resolver.getOverrideManager();
    promoteManager = resolver.getPromoteManager();
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
    recordStaticType(node, typeProvider.getStringType());
    return null;
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
    recordStaticType(node, getType(node.getType()));
    return null;
  }

  /**
   * The Dart Language Specification, 12.18: <blockquote>... an assignment <i>a</i> of the form <i>v
   * = e</i> ...
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
   * <i>e<sub>3</sub></i>.</blockquote>
   */
  @Override
  public Void visitAssignmentExpression(AssignmentExpression node) {
    TokenType operator = node.getOperator().getType();
    if (operator == TokenType.EQ) {
      Expression rightHandSide = node.getRightHandSide();

      Type staticType = getStaticType(rightHandSide);
      recordStaticType(node, staticType);
      Type overrideType = staticType;

      Type propagatedType = rightHandSide.getPropagatedType();
      if (propagatedType != null) {
        if (propagatedType.isMoreSpecificThan(staticType)) {
          recordPropagatedType(node, propagatedType);
        }
        overrideType = propagatedType;
      }
      resolver.overrideExpression(node.getLeftHandSide(), overrideType, true);
    } else {
      ExecutableElement staticMethodElement = node.getStaticElement();
      Type staticType = computeStaticReturnType(staticMethodElement);
      recordStaticType(node, staticType);

      MethodElement propagatedMethodElement = node.getPropagatedElement();
      if (propagatedMethodElement != staticMethodElement) {
        Type propagatedType = computeStaticReturnType(propagatedMethodElement);
        if (propagatedType != null && propagatedType.isMoreSpecificThan(staticType)) {
          recordPropagatedType(node, propagatedType);
        }
      }
    }
    return null;
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
    ExecutableElement staticMethodElement = node.getStaticElement();
    Type staticType = computeStaticReturnType(staticMethodElement);
    staticType = refineBinaryExpressionType(node, staticType);
    recordStaticType(node, staticType);

    MethodElement propagatedMethodElement = node.getPropagatedElement();
    if (propagatedMethodElement != staticMethodElement) {
      Type propagatedType = computeStaticReturnType(propagatedMethodElement);
      if (propagatedType != null && propagatedType.isMoreSpecificThan(staticType)) {
        recordPropagatedType(node, propagatedType);
      }
    }
    return null;
  }

  /**
   * The Dart Language Specification, 12.4: <blockquote>The static type of a boolean literal is
   * bool.</blockquote>
   */
  @Override
  public Void visitBooleanLiteral(BooleanLiteral node) {
    recordStaticType(node, typeProvider.getBoolType());
    return null;
  }

  /**
   * The Dart Language Specification, 12.15.2: <blockquote>A cascaded method invocation expression
   * of the form <i>e..suffix</i> is equivalent to the expression <i>(t) {t.suffix; return
   * t;}(e)</i>.</blockquote>
   */
  @Override
  public Void visitCascadeExpression(CascadeExpression node) {
    recordStaticType(node, getStaticType(node.getTarget()));
    recordPropagatedType(node, node.getTarget().getPropagatedType());
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
    Type staticThenType = getStaticType(node.getThenExpression());
    Type staticElseType = getStaticType(node.getElseExpression());
    if (staticThenType == null) {
      // TODO(brianwilkerson) Determine whether this can still happen.
      staticThenType = dynamicType;
    }
    if (staticElseType == null) {
      // TODO(brianwilkerson) Determine whether this can still happen.
      staticElseType = dynamicType;
    }
    Type staticType = staticThenType.getLeastUpperBound(staticElseType);
    if (staticType == null) {
      staticType = dynamicType;
    }
    recordStaticType(node, staticType);

    Type propagatedThenType = node.getThenExpression().getPropagatedType();
    Type propagatedElseType = node.getElseExpression().getPropagatedType();
    if (propagatedThenType != null || propagatedElseType != null) {
      if (propagatedThenType == null) {
        propagatedThenType = staticThenType;
      }
      if (propagatedElseType == null) {
        propagatedElseType = staticElseType;
      }
      Type propagatedType = propagatedThenType.getLeastUpperBound(propagatedElseType);
      if (propagatedType != null && propagatedType.isMoreSpecificThan(staticType)) {
        recordPropagatedType(node, propagatedType);
      }
    }
    return null;
  }

  /**
   * The Dart Language Specification, 12.3: <blockquote>The static type of a literal double is
   * double.</blockquote>
   */
  @Override
  public Void visitDoubleLiteral(DoubleLiteral node) {
    recordStaticType(node, typeProvider.getDoubleType());
    return null;
  }

  @Override
  public Void visitFunctionDeclaration(FunctionDeclaration node) {
    FunctionExpression function = node.getFunctionExpression();
    ExecutableElementImpl functionElement = (ExecutableElementImpl) node.getElement();
    functionElement.setReturnType(computeStaticReturnTypeOfFunctionDeclaration(node));
    recordPropagatedTypeOfFunction(functionElement, function.getBody());
    recordStaticType(function, functionElement.getType());
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
    if (node.getParent() instanceof FunctionDeclaration) {
      // The function type will be resolved and set when we visit the parent node.
      return null;
    }
    ExecutableElementImpl functionElement = (ExecutableElementImpl) node.getElement();
    functionElement.setReturnType(computeStaticReturnTypeOfFunctionExpression(node));
    recordPropagatedTypeOfFunction(functionElement, node.getBody());
    recordStaticType(node, node.getElement().getType());
    return null;
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
    ExecutableElement staticMethodElement = node.getStaticElement();

    // Record static return type of the static element.
    Type staticStaticType = computeStaticReturnType(staticMethodElement);
    recordStaticType(node, staticStaticType);

    // Record propagated return type of the static element.
    Type staticPropagatedType = computePropagatedReturnType(staticMethodElement);
    if (staticPropagatedType != null
        && (staticStaticType == null || staticPropagatedType.isMoreSpecificThan(staticStaticType))) {
      recordPropagatedType(node, staticPropagatedType);
    }

    ExecutableElement propagatedMethodElement = node.getPropagatedElement();
    if (propagatedMethodElement != staticMethodElement) {
      // Record static return type of the propagated element.
      Type propagatedStaticType = computeStaticReturnType(propagatedMethodElement);
      if (propagatedStaticType != null
          && (staticStaticType == null || propagatedStaticType.isMoreSpecificThan(staticStaticType))
          && (staticPropagatedType == null || propagatedStaticType.isMoreSpecificThan(staticPropagatedType))) {
        recordPropagatedType(node, propagatedStaticType);
      }
      // Record propagated return type of the propagated element.
      Type propagatedPropagatedType = computePropagatedReturnType(propagatedMethodElement);
      if (propagatedPropagatedType != null
          && (staticStaticType == null || propagatedPropagatedType.isMoreSpecificThan(staticStaticType))
          && (staticPropagatedType == null || propagatedPropagatedType.isMoreSpecificThan(staticPropagatedType))
          && (propagatedStaticType == null || propagatedPropagatedType.isMoreSpecificThan(propagatedStaticType))) {
        recordPropagatedType(node, propagatedPropagatedType);
      }
    }

    return null;
  }

  /**
   * The Dart Language Specification, 12.29: <blockquote>An assignable expression of the form
   * <i>e<sub>1</sub>[e<sub>2</sub>]</i> is evaluated as a method invocation of the operator method
   * <i>[]</i> on <i>e<sub>1</sub></i> with argument <i>e<sub>2</sub></i>.</blockquote>
   */
  @Override
  public Void visitIndexExpression(IndexExpression node) {
    if (node.inSetterContext()) {
      ExecutableElement staticMethodElement = node.getStaticElement();
      Type staticType = computeArgumentType(staticMethodElement);
      recordStaticType(node, staticType);

      MethodElement propagatedMethodElement = node.getPropagatedElement();
      if (propagatedMethodElement != staticMethodElement) {
        Type propagatedType = computeArgumentType(propagatedMethodElement);
        if (propagatedType != null && propagatedType.isMoreSpecificThan(staticType)) {
          recordPropagatedType(node, propagatedType);
        }
      }
    } else {
      ExecutableElement staticMethodElement = node.getStaticElement();
      Type staticType = computeStaticReturnType(staticMethodElement);
      recordStaticType(node, staticType);

      MethodElement propagatedMethodElement = node.getPropagatedElement();
      if (propagatedMethodElement != staticMethodElement) {
        Type propagatedType = computeStaticReturnType(propagatedMethodElement);
        if (propagatedType != null && propagatedType.isMoreSpecificThan(staticType)) {
          recordPropagatedType(node, propagatedType);
        }
      }
    }
    return null;
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
    recordStaticType(node, node.getConstructorName().getType().getType());

    ConstructorElement element = node.getStaticElement();
    if (element != null && "Element".equals(element.getEnclosingElement().getName())) {
      LibraryElement library = element.getLibrary();
      if (isHtmlLibrary(library)) {
        String constructorName = element.getName();
        if ("tag".equals(constructorName)) {
          Type returnType = getFirstArgumentAsTypeWithMap(
              library,
              node.getArgumentList(),
              HTML_ELEMENT_TO_CLASS_MAP);
          if (returnType != null) {
            recordPropagatedType(node, returnType);
          }
        } else {
          Type returnType = getElementNameAsType(
              library,
              constructorName,
              HTML_ELEMENT_TO_CLASS_MAP);
          if (returnType != null) {
            recordPropagatedType(node, returnType);
          }
        }
      }
    }
    return null;
  }

  /**
   * The Dart Language Specification, 12.3: <blockquote>The static type of an integer literal is
   * {@code int}.</blockquote>
   */
  @Override
  public Void visitIntegerLiteral(IntegerLiteral node) {
    recordStaticType(node, typeProvider.getIntType());
    return null;
  }

  /**
   * The Dart Language Specification, 12.31: <blockquote>It is a static warning if <i>T</i> does not
   * denote a type available in the current lexical scope.
   * <p>
   * The static type of an is-expression is {@code bool}.</blockquote>
   */
  @Override
  public Void visitIsExpression(IsExpression node) {
    recordStaticType(node, typeProvider.getBoolType());
    return null;
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
    Type staticType = dynamicType;
    TypeArgumentList typeArguments = node.getTypeArguments();
    if (typeArguments != null) {
      NodeList<TypeName> arguments = typeArguments.getArguments();
      if (arguments != null && arguments.size() == 1) {
        TypeName argumentTypeName = arguments.get(0);
        Type argumentType = getType(argumentTypeName);
        if (argumentType != null) {
          staticType = argumentType;
        }
      }
    }
    recordStaticType(node, typeProvider.getListType().substitute(new Type[] {staticType}));
    return null;
  }

  /**
   * The Dart Language Specification, 12.7: <blockquote>The static type of a map literal of the form
   * <i><b>const</b> &lt;K, V&gt; {k<sub>1</sub>:e<sub>1</sub>, &hellip;,
   * k<sub>n</sub>:e<sub>n</sub>}</i> or the form <i>&lt;K, V&gt; {k<sub>1</sub>:e<sub>1</sub>,
   * &hellip;, k<sub>n</sub>:e<sub>n</sub>}</i> is {@code Map&lt;K, V&gt;}. The static type a map
   * literal of the form <i><b>const</b> {k<sub>1</sub>:e<sub>1</sub>, &hellip;,
   * k<sub>n</sub>:e<sub>n</sub>}</i> or the form <i>{k<sub>1</sub>:e<sub>1</sub>, &hellip;,
   * k<sub>n</sub>:e<sub>n</sub>}</i> is {@code Map&lt;dynamic, dynamic&gt;}.
   * <p>
   * It is a compile-time error if the first type argument to a map literal is not
   * <i>String</i>.</blockquote>
   */
  @Override
  public Void visitMapLiteral(MapLiteral node) {
    Type staticKeyType = dynamicType;
    Type staticValueType = dynamicType;
    TypeArgumentList typeArguments = node.getTypeArguments();
    if (typeArguments != null) {
      NodeList<TypeName> arguments = typeArguments.getArguments();
      if (arguments != null && arguments.size() == 2) {
        TypeName entryKeyTypeName = arguments.get(0);
        Type entryKeyType = getType(entryKeyTypeName);
        if (entryKeyType != null) {
          staticKeyType = entryKeyType;
        }
        TypeName entryValueTypeName = arguments.get(1);
        Type entryValueType = getType(entryValueTypeName);
        if (entryValueType != null) {
          staticValueType = entryValueType;
        }
      }
    }
    recordStaticType(
        node,
        typeProvider.getMapType().substitute(new Type[] {staticKeyType, staticValueType}));
    return null;
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
    SimpleIdentifier methodNameNode = node.getMethodName();
    Element staticMethodElement = methodNameNode.getStaticElement();

    // Record types of the local variable invoked as a function.
    if (staticMethodElement instanceof LocalVariableElement) {
      LocalVariableElement variable = (LocalVariableElement) staticMethodElement;
      Type staticType = variable.getType();
      recordStaticType(methodNameNode, staticType);
      Type propagatedType = overrideManager.getType(variable);
      if (propagatedType != null && propagatedType.isMoreSpecificThan(staticType)) {
        recordPropagatedType(methodNameNode, propagatedType);
      }
    }

    // Record static return type of the static element.
    Type staticStaticType = computeStaticReturnType(staticMethodElement);
    recordStaticType(node, staticStaticType);

    // Record propagated return type of the static element.
    Type staticPropagatedType = computePropagatedReturnType(staticMethodElement);
    if (staticPropagatedType != null
        && (staticStaticType == null || staticPropagatedType.isMoreSpecificThan(staticStaticType))) {
      recordPropagatedType(node, staticPropagatedType);
    }

    boolean needPropagatedType = true;
    String methodName = methodNameNode.getName();
    if (methodName.equals("then")) {
      Expression target = node.getRealTarget();
      if (target != null) {
        Type targetType = target.getBestType();
        if (isAsyncFutureType(targetType)) {
          // Future.then(closure) return type is:
          // 1) the returned Future type, if the closure returns a Future;
          // 2) Future<valueType>, if the closure returns a value.
          NodeList<Expression> arguments = node.getArgumentList().getArguments();
          if (arguments.size() == 1) {
            // TODO(brianwilkerson) Handle the case where both arguments are provided.
            Expression closureArg = arguments.get(0);
            if (closureArg instanceof FunctionExpression) {
              FunctionExpression closureExpr = (FunctionExpression) closureArg;
              Type returnType = computePropagatedReturnType(closureExpr.getElement());
              if (returnType != null) {
                // prepare the type of the returned Future
                InterfaceTypeImpl newFutureType;
                if (isAsyncFutureType(returnType)) {
                  newFutureType = (InterfaceTypeImpl) returnType;
                } else {
                  InterfaceType futureType = (InterfaceType) targetType;
                  newFutureType = new InterfaceTypeImpl(futureType.getElement());
                  newFutureType.setTypeArguments(new Type[] {returnType});
                }
                // set the 'then' invocation type
                recordPropagatedType(node, newFutureType);
                needPropagatedType = false;
                return null;
              }
            }
          }
        }
      }
    } else if (methodName.equals("$dom_createEvent")) {
      Expression target = node.getRealTarget();
      if (target != null) {
        Type targetType = target.getBestType();
        if (targetType instanceof InterfaceType
            && (targetType.getName().equals("HtmlDocument") || targetType.getName().equals(
                "Document"))) {
          LibraryElement library = targetType.getElement().getLibrary();
          if (isHtmlLibrary(library)) {
            Type returnType = getFirstArgumentAsType(library, node.getArgumentList());
            if (returnType != null) {
              recordPropagatedType(node, returnType);
              needPropagatedType = false;
            }
          }
        }
      }
    } else if (methodName.equals("query")) {
      Expression target = node.getRealTarget();
      if (target == null) {
        Element methodElement = methodNameNode.getBestElement();
        if (methodElement != null) {
          LibraryElement library = methodElement.getLibrary();
          if (isHtmlLibrary(library)) {
            Type returnType = getFirstArgumentAsQuery(library, node.getArgumentList());
            if (returnType != null) {
              recordPropagatedType(node, returnType);
              needPropagatedType = false;
            }
          }
        }
      } else {
        Type targetType = target.getBestType();
        if (targetType instanceof InterfaceType
            && (targetType.getName().equals("HtmlDocument") || targetType.getName().equals(
                "Document"))) {
          LibraryElement library = targetType.getElement().getLibrary();
          if (isHtmlLibrary(library)) {
            Type returnType = getFirstArgumentAsQuery(library, node.getArgumentList());
            if (returnType != null) {
              recordPropagatedType(node, returnType);
              needPropagatedType = false;
            }
          }
        }
      }
    } else if (methodName.equals("$dom_createElement")) {
      Expression target = node.getRealTarget();
      if (target != null) {
        Type targetType = target.getBestType();
        if (targetType instanceof InterfaceType
            && (targetType.getName().equals("HtmlDocument") || targetType.getName().equals(
                "Document"))) {
          LibraryElement library = targetType.getElement().getLibrary();
          if (isHtmlLibrary(library)) {
            Type returnType = getFirstArgumentAsQuery(library, node.getArgumentList());
            if (returnType != null) {
              recordPropagatedType(node, returnType);
              needPropagatedType = false;
            }
          }
        }
      }
    } else if (methodName.equals("JS")) {
      Type returnType = getFirstArgumentAsType(
          typeProvider.getObjectType().getElement().getLibrary(),
          node.getArgumentList());
      if (returnType != null) {
        recordPropagatedType(node, returnType);
        needPropagatedType = false;
      }
    } else if (methodName.equals("getContext")) {
      Expression target = node.getRealTarget();
      if (target != null) {
        Type targetType = target.getBestType();
        if (targetType instanceof InterfaceType && (targetType.getName().equals("CanvasElement"))) {
          NodeList<Expression> arguments = node.getArgumentList().getArguments();
          if (arguments.size() == 1) {
            Expression argument = arguments.get(0);
            if (argument instanceof StringLiteral) {
              String value = ((StringLiteral) argument).getStringValue();
              if ("2d".equals(value)) {
                PropertyAccessorElement getter = ((InterfaceType) targetType).getElement().getGetter(
                    "context2D");
                if (getter != null) {
                  Type returnType = getter.getReturnType();
                  if (returnType != null) {
                    recordPropagatedType(node, returnType);
                    needPropagatedType = false;
                  }
                }
              }
            }
          }
        }
      }
    }
    if (needPropagatedType) {
      Element propagatedElement = methodNameNode.getPropagatedElement();
      // HACK: special case for object methods ([toString]) on dynamic expressions.
      // More special cases in [visitPrefixedIdentfier].
      if (propagatedElement == null) {
        propagatedElement = typeProvider.getObjectType().getMethod(methodNameNode.getName());
      }

      if (propagatedElement != staticMethodElement) {
        // Record static return type of the propagated element.
        Type propagatedStaticType = computeStaticReturnType(propagatedElement);
        if (propagatedStaticType != null
            && (staticStaticType == null || propagatedStaticType.isMoreSpecificThan(staticStaticType))
            && (staticPropagatedType == null || propagatedStaticType.isMoreSpecificThan(staticPropagatedType))) {
          recordPropagatedType(node, propagatedStaticType);
        }
        // Record propagated return type of the propagated element.
        Type propagatedPropagatedType = computePropagatedReturnType(propagatedElement);
        if (propagatedPropagatedType != null
            && (staticStaticType == null || propagatedPropagatedType.isMoreSpecificThan(staticStaticType))
            && (staticPropagatedType == null || propagatedPropagatedType.isMoreSpecificThan(staticPropagatedType))
            && (propagatedStaticType == null || propagatedPropagatedType.isMoreSpecificThan(propagatedStaticType))) {
          recordPropagatedType(node, propagatedPropagatedType);
        }
      }
    }
    return null;
  }

  @Override
  public Void visitNamedExpression(NamedExpression node) {
    Expression expression = node.getExpression();
    recordStaticType(node, getStaticType(expression));
    recordPropagatedType(node, expression.getPropagatedType());
    return null;
  }

  /**
   * The Dart Language Specification, 12.2: <blockquote>The static type of {@code null} is bottom.
   * </blockquote>
   */
  @Override
  public Void visitNullLiteral(NullLiteral node) {
    recordStaticType(node, typeProvider.getBottomType());
    return null;
  }

  @Override
  public Void visitParenthesizedExpression(ParenthesizedExpression node) {
    Expression expression = node.getExpression();
    recordStaticType(node, getStaticType(expression));
    recordPropagatedType(node, expression.getPropagatedType());
    return null;
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
    Expression operand = node.getOperand();
    Type staticType = getStaticType(operand);
    TokenType operator = node.getOperator().getType();
    if (operator == TokenType.MINUS_MINUS || operator == TokenType.PLUS_PLUS) {
      Type intType = typeProvider.getIntType();
      if (getStaticType(node.getOperand()) == intType) {
        staticType = intType;
      }
    }
    recordStaticType(node, staticType);
    recordPropagatedType(node, operand.getPropagatedType());
    return null;
  }

  /**
   * See {@link #visitSimpleIdentifier(SimpleIdentifier)}.
   */
  @Override
  public Void visitPrefixedIdentifier(PrefixedIdentifier node) {
    SimpleIdentifier prefixedIdentifier = node.getIdentifier();
    Element staticElement = prefixedIdentifier.getStaticElement();
    Type staticType = dynamicType;
    Type propagatedType = null;
    if (staticElement instanceof ClassElement) {
      if (isNotTypeLiteral(node)) {
        staticType = ((ClassElement) staticElement).getType();
      } else {
        staticType = typeProvider.getTypeType();
      }
    } else if (staticElement instanceof FunctionTypeAliasElement) {
      if (isNotTypeLiteral(node)) {
        staticType = ((FunctionTypeAliasElement) staticElement).getType();
      } else {
        staticType = typeProvider.getTypeType();
      }
    } else if (staticElement instanceof MethodElement) {
      staticType = ((MethodElement) staticElement).getType();
    } else if (staticElement instanceof PropertyAccessorElement) {
      staticType = getTypeOfProperty(
          (PropertyAccessorElement) staticElement,
          node.getPrefix().getStaticType());
      propagatedType = getPropertyPropagatedType(staticElement, propagatedType);
    } else if (staticElement instanceof ExecutableElement) {
      staticType = ((ExecutableElement) staticElement).getType();
    } else if (staticElement instanceof TypeParameterElement) {
      staticType = ((TypeParameterElement) staticElement).getType();
    } else if (staticElement instanceof VariableElement) {
      staticType = ((VariableElement) staticElement).getType();
    }
    recordStaticType(prefixedIdentifier, staticType);
    recordStaticType(node, staticType);

    Element propagatedElement = prefixedIdentifier.getPropagatedElement();
    // HACK: special case for object getters ([hashCode] and [runtimeType]) on dynamic expressions.
    // More special cases in [visitMethodInvocation].
    if (propagatedElement == null) {
      propagatedElement = typeProvider.getObjectType().getGetter(prefixedIdentifier.getName());
    }

    if (propagatedElement instanceof ClassElement) {
      if (isNotTypeLiteral(node)) {
        propagatedType = ((ClassElement) propagatedElement).getType();
      } else {
        propagatedType = typeProvider.getTypeType();
      }
    } else if (propagatedElement instanceof FunctionTypeAliasElement) {
      propagatedType = ((FunctionTypeAliasElement) propagatedElement).getType();
    } else if (propagatedElement instanceof MethodElement) {
      propagatedType = ((MethodElement) propagatedElement).getType();
    } else if (propagatedElement instanceof PropertyAccessorElement) {
      propagatedType = getTypeOfProperty(
          (PropertyAccessorElement) propagatedElement,
          node.getPrefix().getStaticType());
      propagatedType = getPropertyPropagatedType(propagatedElement, propagatedType);
    } else if (propagatedElement instanceof ExecutableElement) {
      propagatedType = ((ExecutableElement) propagatedElement).getType();
    } else if (propagatedElement instanceof TypeParameterElement) {
      propagatedType = ((TypeParameterElement) propagatedElement).getType();
    } else if (propagatedElement instanceof VariableElement) {
      propagatedType = ((VariableElement) propagatedElement).getType();
    }
    Type overriddenType = overrideManager.getType(propagatedElement);
    if (propagatedType == null
        || (overriddenType != null && overriddenType.isMoreSpecificThan(propagatedType))) {
      propagatedType = overriddenType;
    }
    if (propagatedType != null && propagatedType.isMoreSpecificThan(staticType)) {
      recordPropagatedType(prefixedIdentifier, propagatedType);
      recordPropagatedType(node, propagatedType);
    }
    return null;
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
      recordStaticType(node, typeProvider.getBoolType());
    } else {
      // The other cases are equivalent to invoking a method.
      ExecutableElement staticMethodElement = node.getStaticElement();
      Type staticType = computeStaticReturnType(staticMethodElement);
      if (operator == TokenType.MINUS_MINUS || operator == TokenType.PLUS_PLUS) {
        Type intType = typeProvider.getIntType();
        if (getStaticType(node.getOperand()) == intType) {
          staticType = intType;
        }
      }
      recordStaticType(node, staticType);

      MethodElement propagatedMethodElement = node.getPropagatedElement();
      if (propagatedMethodElement != staticMethodElement) {
        Type propagatedType = computeStaticReturnType(propagatedMethodElement);
        if (propagatedType != null && propagatedType.isMoreSpecificThan(staticType)) {
          recordPropagatedType(node, propagatedType);
        }
      }
    }
    return null;
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
    SimpleIdentifier propertyName = node.getPropertyName();
    Element staticElement = propertyName.getStaticElement();
    Type staticType = dynamicType;
    if (staticElement instanceof MethodElement) {
      staticType = ((MethodElement) staticElement).getType();
    } else if (staticElement instanceof PropertyAccessorElement) {
      Expression realTarget = node.getRealTarget();
      staticType = getTypeOfProperty((PropertyAccessorElement) staticElement, realTarget != null
          ? getStaticType(realTarget) : null);
    } else {
      // TODO(brianwilkerson) Report this internal error.
    }
    recordStaticType(propertyName, staticType);
    recordStaticType(node, staticType);

    Element propagatedElement = propertyName.getPropagatedElement();
    Type propagatedType = overrideManager.getType(propagatedElement);
    if (propagatedElement instanceof MethodElement) {
      propagatedType = ((MethodElement) propagatedElement).getType();
    } else if (propagatedElement instanceof PropertyAccessorElement) {
      Expression realTarget = node.getRealTarget();
      propagatedType = getTypeOfProperty(
          (PropertyAccessorElement) propagatedElement,
          realTarget != null ? realTarget.getBestType() : null);
    } else {
      // TODO(brianwilkerson) Report this internal error.
    }
    if (propagatedType != null && propagatedType.isMoreSpecificThan(staticType)) {
      recordPropagatedType(propertyName, propagatedType);
      recordPropagatedType(node, propagatedType);
    }
    return null;
  }

  /**
   * The Dart Language Specification, 12.9: <blockquote>The static type of a rethrow expression is
   * bottom.</blockquote>
   */
  @Override
  public Void visitRethrowExpression(RethrowExpression node) {
    recordStaticType(node, typeProvider.getBottomType());
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
    Element element = node.getStaticElement();
    Type staticType = dynamicType;
    if (element instanceof ClassElement) {
      if (isNotTypeLiteral(node)) {
        staticType = ((ClassElement) element).getType();
      } else {
        staticType = typeProvider.getTypeType();
      }
    } else if (element instanceof FunctionTypeAliasElement) {
      if (isNotTypeLiteral(node)) {
        staticType = ((FunctionTypeAliasElement) element).getType();
      } else {
        staticType = typeProvider.getTypeType();
      }
    } else if (element instanceof MethodElement) {
      staticType = ((MethodElement) element).getType();
    } else if (element instanceof PropertyAccessorElement) {
      staticType = getTypeOfProperty((PropertyAccessorElement) element, null);
    } else if (element instanceof ExecutableElement) {
      staticType = ((ExecutableElement) element).getType();
    } else if (element instanceof TypeParameterElement) {
      staticType = typeProvider.getTypeType();
    } else if (element instanceof VariableElement) {
      VariableElement variable = (VariableElement) element;
      staticType = promoteManager.getStaticType(variable);
    } else if (element instanceof PrefixElement) {
      return null;
    } else {
      staticType = dynamicType;
    }
    recordStaticType(node, staticType);
    // TODO(brianwilkerson) I think we want to repeat the logic above using the propagated element
    // to get another candidate for the propagated type.
    Type propagatedType = getPropertyPropagatedType(element, null);
    if (propagatedType == null) {
      Type overriddenType = overrideManager.getType(element);
      if (propagatedType == null || overriddenType != null
          && overriddenType.isMoreSpecificThan(propagatedType)) {
        propagatedType = overriddenType;
      }
    }
    if (propagatedType != null && propagatedType.isMoreSpecificThan(staticType)) {
      recordPropagatedType(node, propagatedType);
    }
    return null;
  }

  /**
   * The Dart Language Specification, 12.5: <blockquote>The static type of a string literal is
   * {@code String}.</blockquote>
   */
  @Override
  public Void visitSimpleStringLiteral(SimpleStringLiteral node) {
    recordStaticType(node, typeProvider.getStringType());
    return null;
  }

  /**
   * The Dart Language Specification, 12.5: <blockquote>The static type of a string literal is
   * {@code String}.</blockquote>
   */
  @Override
  public Void visitStringInterpolation(StringInterpolation node) {
    recordStaticType(node, typeProvider.getStringType());
    return null;
  }

  @Override
  public Void visitSuperExpression(SuperExpression node) {
    if (thisType == null) {
      // TODO(brianwilkerson) Report this error if it hasn't already been reported
      recordStaticType(node, dynamicType);
    } else {
      recordStaticType(node, thisType);
    }
    return null;
  }

  @Override
  public Void visitSymbolLiteral(SymbolLiteral node) {
    recordStaticType(node, typeProvider.getSymbolType());
    return null;
  }

  /**
   * The Dart Language Specification, 12.10: <blockquote>The static type of {@code this} is the
   * interface of the immediately enclosing class.</blockquote>
   */
  @Override
  public Void visitThisExpression(ThisExpression node) {
    if (thisType == null) {
      // TODO(brianwilkerson) Report this error if it hasn't already been reported
      recordStaticType(node, dynamicType);
    } else {
      recordStaticType(node, thisType);
    }
    return null;
  }

  /**
   * The Dart Language Specification, 12.8: <blockquote>The static type of a throw expression is
   * bottom.</blockquote>
   */
  @Override
  public Void visitThrowExpression(ThrowExpression node) {
    recordStaticType(node, typeProvider.getBottomType());
    return null;
  }

  @Override
  public Void visitVariableDeclaration(VariableDeclaration node) {
    Expression initializer = node.getInitializer();
    if (initializer != null) {
      Type rightType = initializer.getBestType();
      SimpleIdentifier name = node.getName();
      recordPropagatedType(name, rightType);
      VariableElement element = (VariableElement) name.getStaticElement();
      if (element != null) {
        resolver.overrideVariable(element, rightType, true);
      }
    }
    return null;
  }

  /**
   * Record that the static type of the given node is the type of the second argument to the method
   * represented by the given element.
   * 
   * @param element the element representing the method invoked by the given node
   */
  private Type computeArgumentType(ExecutableElement element) {
    if (element != null) {
      ParameterElement[] parameters = element.getParameters();
      if (parameters != null && parameters.length == 2) {
        return parameters[1].getType();
      }
    }
    return dynamicType;
  }

  /**
   * Compute the propagated return type of the method or function represented by the given element.
   * 
   * @param element the element representing the method or function invoked by the given node
   * @return the propagated return type that was computed
   */
  private Type computePropagatedReturnType(Element element) {
    if (element instanceof ExecutableElement) {
      return propagatedReturnTypes.get(element);
    }
    return null;
  }

  /**
   * Given a function body, compute the propagated return type of the function. The propagated
   * return type of functions with a block body is the least upper bound of all
   * {@link ReturnStatement} expressions, with an expression body it is the type of the expression.
   * 
   * @param body the boy of the function whose propagated return type is to be computed
   * @return the propagated return type that was computed
   */
  private Type computePropagatedReturnTypeOfFunction(FunctionBody body) {
    if (body instanceof ExpressionFunctionBody) {
      ExpressionFunctionBody expressionBody = (ExpressionFunctionBody) body;
      return expressionBody.getExpression().getBestType();
    }
    if (body instanceof BlockFunctionBody) {
      final Type[] result = {null};
      body.accept(new GeneralizingAstVisitor<Void>() {
        @Override
        public Void visitExpression(Expression node) {
          // Don't visit expressions, there are no interesting return statements.
          return null;
        }

        @Override
        public Void visitReturnStatement(ReturnStatement node) {
          // prepare this 'return' type
          Type type;
          Expression expression = node.getExpression();
          if (expression != null) {
            type = expression.getBestType();
          } else {
            type = BottomTypeImpl.getInstance();
          }
          // merge types
          if (result[0] == null) {
            result[0] = type;
          } else {
            result[0] = result[0].getLeastUpperBound(type);
          }
          return null;
        }
      });
      return result[0];
    }
    return null;
  }

  /**
   * Compute the static return type of the method or function represented by the given element.
   * 
   * @param element the element representing the method or function invoked by the given node
   * @return the static return type that was computed
   */
  private Type computeStaticReturnType(Element element) {
    if (element instanceof PropertyAccessorElement) {
      //
      // This is a function invocation expression disguised as something else. We are invoking a
      // getter and then invoking the returned function.
      //
      FunctionType propertyType = ((PropertyAccessorElement) element).getType();
      if (propertyType != null) {
        Type returnType = propertyType.getReturnType();
        if (returnType.isDartCoreFunction()) {
          return dynamicType;
        } else if (returnType instanceof InterfaceType) {
          MethodElement callMethod = ((InterfaceType) returnType).lookUpMethod(
              FunctionElement.CALL_METHOD_NAME,
              resolver.getDefiningLibrary());
          if (callMethod != null) {
            return callMethod.getType().getReturnType();
          }
        } else if (returnType instanceof FunctionType) {
          Type innerReturnType = ((FunctionType) returnType).getReturnType();
          if (innerReturnType != null) {
            return innerReturnType;
          }
        }
        if (returnType != null) {
          return returnType;
        }
      }
    } else if (element instanceof ExecutableElement) {
      FunctionType type = ((ExecutableElement) element).getType();
      if (type != null) {
        // TODO(brianwilkerson) Figure out the conditions under which the type is null.
        return type.getReturnType();
      }
    } else if (element instanceof VariableElement) {
      VariableElement variable = (VariableElement) element;
      Type variableType = promoteManager.getStaticType(variable);
      if (variableType instanceof FunctionType) {
        return ((FunctionType) variableType).getReturnType();
      }
    }
    return dynamicType;
  }

  /**
   * Given a function declaration, compute the return static type of the function. The return type
   * of functions with a block body is {@code dynamicType}, with an expression body it is the type
   * of the expression.
   * 
   * @param node the function expression whose static return type is to be computed
   * @return the static return type that was computed
   */
  private Type computeStaticReturnTypeOfFunctionDeclaration(FunctionDeclaration node) {
    TypeName returnType = node.getReturnType();
    if (returnType == null) {
      return dynamicType;
    }
    return returnType.getType();
  }

  /**
   * Given a function expression, compute the return type of the function. The return type of
   * functions with a block body is {@code dynamicType}, with an expression body it is the type of
   * the expression.
   * 
   * @param node the function expression whose return type is to be computed
   * @return the return type that was computed
   */
  private Type computeStaticReturnTypeOfFunctionExpression(FunctionExpression node) {
    FunctionBody body = node.getBody();
    if (body instanceof ExpressionFunctionBody) {
      return getStaticType(((ExpressionFunctionBody) body).getExpression());
    }
    return dynamicType;
  }

  /**
   * If the given element name can be mapped to the name of a class defined within the given
   * library, return the type specified by the argument.
   * 
   * @param library the library in which the specified type would be defined
   * @param elementName the name of the element for which a type is being sought
   * @param nameMap an optional map used to map the element name to a type name
   * @return the type specified by the first argument in the argument list
   */
  private Type getElementNameAsType(LibraryElement library, String elementName,
      HashMap<String, String> nameMap) {
    if (elementName != null) {
      if (nameMap != null) {
        elementName = nameMap.get(elementName.toLowerCase());
      }
      ClassElement returnType = library.getType(elementName);
      if (returnType != null) {
        return returnType.getType();
      }
    }
    return null;
  }

  /**
   * If the given argument list contains at least one argument, and if the argument is a simple
   * string literal, then parse that argument as a query string and return the type specified by the
   * argument.
   * 
   * @param library the library in which the specified type would be defined
   * @param argumentList the list of arguments from which a type is to be extracted
   * @return the type specified by the first argument in the argument list
   */
  private Type getFirstArgumentAsQuery(LibraryElement library, ArgumentList argumentList) {
    String argumentValue = getFirstArgumentAsString(argumentList);
    if (argumentValue != null) {
      //
      // If the query has spaces, full parsing is required because it might be:
      //   E[text='warning text']
      //
      if (StringUtilities.indexOf1(argumentValue, 0, ' ') >= 0) {
        return null;
      }
      //
      // Otherwise, try to extract the tag based on http://www.w3.org/TR/CSS2/selector.html.
      //
      String tag = argumentValue;
      tag = StringUtilities.substringBeforeChar(tag, ':');
      tag = StringUtilities.substringBeforeChar(tag, '[');
      tag = StringUtilities.substringBeforeChar(tag, '.');
      tag = StringUtilities.substringBeforeChar(tag, '#');

      tag = HTML_ELEMENT_TO_CLASS_MAP.get(tag.toLowerCase());
      ClassElement returnType = library.getType(tag);
      if (returnType != null) {
        return returnType.getType();
      }
    }
    return null;
  }

  /**
   * If the given argument list contains at least one argument, and if the argument is a simple
   * string literal, return the String value of the argument.
   * 
   * @param argumentList the list of arguments from which a string value is to be extracted
   * @return the string specified by the first argument in the argument list
   */
  private String getFirstArgumentAsString(ArgumentList argumentList) {
    NodeList<Expression> arguments = argumentList.getArguments();
    if (arguments.size() > 0) {
      Expression argument = arguments.get(0);
      if (argument instanceof SimpleStringLiteral) {
        return ((SimpleStringLiteral) argument).getValue();
      }
    }
    return null;
  }

  /**
   * If the given argument list contains at least one argument, and if the argument is a simple
   * string literal, and if the value of the argument is the name of a class defined within the
   * given library, return the type specified by the argument.
   * 
   * @param library the library in which the specified type would be defined
   * @param argumentList the list of arguments from which a type is to be extracted
   * @return the type specified by the first argument in the argument list
   */
  private Type getFirstArgumentAsType(LibraryElement library, ArgumentList argumentList) {
    return getFirstArgumentAsTypeWithMap(library, argumentList, null);
  }

  /**
   * If the given argument list contains at least one argument, and if the argument is a simple
   * string literal, and if the value of the argument is the name of a class defined within the
   * given library, return the type specified by the argument.
   * 
   * @param library the library in which the specified type would be defined
   * @param argumentList the list of arguments from which a type is to be extracted
   * @param nameMap an optional map used to map the element name to a type name
   * @return the type specified by the first argument in the argument list
   */
  private Type getFirstArgumentAsTypeWithMap(LibraryElement library, ArgumentList argumentList,
      HashMap<String, String> nameMap) {
    return getElementNameAsType(library, getFirstArgumentAsString(argumentList), nameMap);
  }

  /**
   * Return the propagated type of the given {@link Element}, or {@code null}.
   */
  private Type getPropertyPropagatedType(Element element, Type currentType) {
    if (element instanceof PropertyAccessorElement) {
      PropertyAccessorElement accessor = (PropertyAccessorElement) element;
      if (accessor.isGetter()) {
        PropertyInducingElement variable = accessor.getVariable();
        Type propagatedType = variable.getPropagatedType();
        if (currentType == null || propagatedType != null
            && propagatedType.isMoreSpecificThan(currentType)) {
          return propagatedType;
        }
      }
    }
    return currentType;
  }

  /**
   * Return the static type of the given expression.
   * 
   * @param expression the expression whose type is to be returned
   * @return the static type of the given expression
   */
  private Type getStaticType(Expression expression) {
    Type type = expression.getStaticType();
    if (type == null) {
      // TODO(brianwilkerson) Determine the conditions for which the static type is null.
      return dynamicType;
    }
    return type;
  }

  /**
   * Return the type represented by the given type name.
   * 
   * @param typeName the type name representing the type to be returned
   * @return the type represented by the type name
   */
  private Type getType(TypeName typeName) {
    Type type = typeName.getType();
    if (type == null) {
      //TODO(brianwilkerson) Determine the conditions for which the type is null.
      return dynamicType;
    }
    return type;
  }

  /**
   * Return the type that should be recorded for a node that resolved to the given accessor.
   * 
   * @param accessor the accessor that the node resolved to
   * @param context if the accessor element has context [by being the RHS of a
   *          {@link PrefixedIdentifier} or {@link PropertyAccess}], and the return type of the
   *          accessor is a parameter type, then the type of the LHS can be used to get more
   *          specific type information
   * @return the type that should be recorded for a node that resolved to the given accessor
   */
  private Type getTypeOfProperty(PropertyAccessorElement accessor, Type context) {
    FunctionType functionType = accessor.getType();
    if (functionType == null) {
      // TODO(brianwilkerson) Report this internal error. This happens when we are analyzing a
      // reference to a property before we have analyzed the declaration of the property or when
      // the property does not have a defined type.
      return dynamicType;
    }
    if (accessor.isSetter()) {
      Type[] parameterTypes = functionType.getNormalParameterTypes();
      if (parameterTypes != null && parameterTypes.length > 0) {
        return parameterTypes[0];
      }
      PropertyAccessorElement getter = accessor.getVariable().getGetter();
      if (getter != null) {
        functionType = getter.getType();
        if (functionType != null) {
          return functionType.getReturnType();
        }
      }
      return dynamicType;
    }
    Type returnType = functionType.getReturnType();
    if (returnType instanceof TypeParameterType && context instanceof InterfaceType) {
      // if the return type is a TypeParameter, we try to use the context [that the function is being
      // called on] to get a more accurate returnType type
      InterfaceType interfaceTypeContext = ((InterfaceType) context);
//      Type[] argumentTypes = interfaceTypeContext.getTypeArguments();
      TypeParameterElement[] typeParameterElements = interfaceTypeContext.getElement() != null
          ? interfaceTypeContext.getElement().getTypeParameters() : null;
      if (typeParameterElements != null) {
        for (int i = 0; i < typeParameterElements.length; i++) {
          TypeParameterElement typeParameterElement = typeParameterElements[i];
          if (returnType.getName().equals(typeParameterElement.getName())) {
            return interfaceTypeContext.getTypeArguments()[i];
          }
        }
        // TODO(jwren) troubleshoot why call to substitute doesn't work
//        Type[] parameterTypes = TypeParameterTypeImpl.getTypes(parameterElements);
//        return returnType.substitute(argumentTypes, parameterTypes);
      }
    }
    return returnType;
  }

  /**
   * Return {@code true} if the given {@link Type} is the {@code Future} form the 'dart:async'
   * library.
   */
  private boolean isAsyncFutureType(Type type) {
    return type instanceof InterfaceType && type.getName().equals("Future")
        && isAsyncLibrary(type.getElement().getLibrary());
  }

  /**
   * Return {@code true} if the given library is the 'dart:async' library.
   * 
   * @param library the library being tested
   * @return {@code true} if the library is 'dart:async'
   */
  private boolean isAsyncLibrary(LibraryElement library) {
    return library.getName().equals("dart.async");
  }

  /**
   * Return {@code true} if the given library is the 'dart:html' library.
   * 
   * @param library the library being tested
   * @return {@code true} if the library is 'dart:html'
   */
  private boolean isHtmlLibrary(LibraryElement library) {
    return library != null && "dart.dom.html".equals(library.getName());
  }

  /**
   * Return {@code true} if the given node is not a type literal.
   * 
   * @param node the node being tested
   * @return {@code true} if the given node is not a type literal
   */
  private boolean isNotTypeLiteral(Identifier node) {
    AstNode parent = node.getParent();
    return parent instanceof TypeName
        || (parent instanceof PrefixedIdentifier && (parent.getParent() instanceof TypeName || ((PrefixedIdentifier) parent).getPrefix() == node))
        || (parent instanceof PropertyAccess && ((PropertyAccess) parent).getTarget() == node)
        || (parent instanceof MethodInvocation && node == ((MethodInvocation) parent).getTarget());
  }

  /**
   * Record that the propagated type of the given node is the given type.
   * 
   * @param expression the node whose type is to be recorded
   * @param type the propagated type of the node
   */
  private void recordPropagatedType(Expression expression, Type type) {
    if (type != null && !type.isDynamic() && !type.isBottom()) {
      expression.setPropagatedType(type);
    }
  }

  /**
   * Given a function element and its body, compute and record the propagated return type of the
   * function.
   * 
   * @param functionElement the function element to record propagated return type for
   * @param body the boy of the function whose propagated return type is to be computed
   * @return the propagated return type that was computed, may be {@code null} if it is not more
   *         specific than the static return type.
   */
  private void recordPropagatedTypeOfFunction(ExecutableElement functionElement, FunctionBody body) {
    Type propagatedReturnType = computePropagatedReturnTypeOfFunction(body);
    if (propagatedReturnType == null) {
      return;
    }
    // Ignore 'bottom' type.
    if (propagatedReturnType.isBottom()) {
      return;
    }
    // Record only if we inferred more specific type.
    Type staticReturnType = functionElement.getReturnType();
    if (!propagatedReturnType.isMoreSpecificThan(staticReturnType)) {
      return;
    }
    // OK, do record.
    propagatedReturnTypes.put(functionElement, propagatedReturnType);
  }

  /**
   * Record that the static type of the given node is the given type.
   * 
   * @param expression the node whose type is to be recorded
   * @param type the static type of the node
   */
  private void recordStaticType(Expression expression, Type type) {
    if (type == null) {
      expression.setStaticType(dynamicType);
    } else {
      expression.setStaticType(type);
    }
  }

  /**
   * Attempts to make a better guess for the static type of the given binary expression.
   * 
   * @param node the binary expression to analyze
   * @param staticType the static type of the expression as resolved
   * @return the better type guess, or the same static type as given
   */
  private Type refineBinaryExpressionType(BinaryExpression node, Type staticType) {
    TokenType operator = node.getOperator().getType();
    // bool
    if (operator == TokenType.AMPERSAND_AMPERSAND || operator == TokenType.BAR_BAR
        || operator == TokenType.EQ_EQ || operator == TokenType.BANG_EQ) {
      return typeProvider.getBoolType();
    }
    Type intType = typeProvider.getIntType();
    if (getStaticType(node.getLeftOperand()).equals(intType)) {
      // int op double
      if (operator == TokenType.MINUS || operator == TokenType.PERCENT
          || operator == TokenType.PLUS || operator == TokenType.STAR) {
        Type doubleType = typeProvider.getDoubleType();
        if (getStaticType(node.getRightOperand()).equals(doubleType)) {
          return doubleType;
        }
      }
      // int op int
      if (operator == TokenType.MINUS || operator == TokenType.PERCENT
          || operator == TokenType.PLUS || operator == TokenType.STAR
          || operator == TokenType.TILDE_SLASH) {
        if (getStaticType(node.getRightOperand()).equals(intType)) {
          staticType = intType;
        }
      }
    }
    // default
    return staticType;
  }
}
