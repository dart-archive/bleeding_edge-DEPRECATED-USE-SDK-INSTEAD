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
package com.google.dart.engine.services.internal.correction;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.dart.engine.ast.AsExpression;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.BooleanLiteral;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ConstructorName;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.IsExpression;
import com.google.dart.engine.ast.Label;
import com.google.dart.engine.ast.LibraryDirective;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NamedExpression;
import com.google.dart.engine.ast.ParenthesizedExpression;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.PostfixExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.ExportElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.element.visitor.GeneralizingElementVisitor;
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.internal.scope.NamespaceBuilder;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.services.change.Edit;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.internal.util.TokenUtils;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.engine.utilities.source.SourceRangeFactory;

import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeEndEnd;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeEndStart;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeNode;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeNodes;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartEnd;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartStart;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeToken;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utilities for analyzing {@link CompilationUnit}, its parts and source.
 */
public class CorrectionUtils {
  /**
   * Describes where to insert new directive or top-level declaration.
   */
  public class InsertDesc {
    public int offset;
    public String prefix = "";
    public String suffix = "";
  }

  /**
   * This class is used to hold the source and also its precedence during inverting logical
   * expressions.
   */
  private static class InvertedCondition {
    static InvertedCondition binary(int precedence, InvertedCondition left, String operation,
        InvertedCondition right) {
      return new InvertedCondition(precedence, parenthesizeIfRequired(left, precedence) + operation
          + parenthesizeIfRequired(right, precedence));
    }

    static InvertedCondition binary(InvertedCondition left, String operation,
        InvertedCondition right) {
      return new InvertedCondition(Integer.MAX_VALUE, left.source + operation + right.source);
    }

    static InvertedCondition simple(String source) {
      return new InvertedCondition(Integer.MAX_VALUE, source);
    }

    final int precedence;
    final String source;

    InvertedCondition(int precedence, String source) {
      this.precedence = precedence;
      this.source = source;
    }
  }

  /**
   * If {@code true} then {@link #addEdit(SourceChange, String, String, Edit)} validates that
   * {@link Edit} replaces correct part of the {@link Source}.
   */
  private static boolean DEBUG_VALIDATE_EDITS = true;

  private static final String[] KNOWN_METHOD_NAME_PREFIXES = {"get", "is", "to"};

  /**
   * Validates that the {@link Edit} replaces the expected part of the {@link Source} and adds this
   * {@link Edit} to the {@link SourceChange}.
   */
  public static void addEdit(AnalysisContext context, SourceChange change, String description,
      String expected, Edit edit) throws Exception {
    if (DEBUG_VALIDATE_EDITS) {
      Source source = change.getSource();
      String sourceContent = getSourceContent(context, source);
      // prepare range
      int beginIndex = edit.getOffset();
      int endIndex = beginIndex + edit.getLength();
      int sourceLength = sourceContent.length();
      if (beginIndex >= sourceLength || endIndex >= sourceLength) {
        throw new IllegalStateException(source + " has " + sourceLength + " characters but "
            + beginIndex + " to " + endIndex + " requested."
            + "\n\nTry to use Tools | Reanalyze Sources.");
      }
      // check that range has expected content
      String rangeContent = sourceContent.substring(beginIndex, endIndex);
      if (!rangeContent.equals(expected)) {
        throw new IllegalStateException(source + " expected |" + expected + "| at " + beginIndex
            + " to " + endIndex + " but |" + rangeContent + "| found."
            + "\n\nTry to use Tools | Reanalyze Sources.");
      }
    }
    // do add the Edit
    change.addEdit(edit, description);
  }

  /**
   * @return <code>true</code> if given {@link List}s are equals at given position.
   */
  public static <T> boolean allListsEqual(List<List<T>> lists, int position) {
    T element = lists.get(0).get(position);
    for (List<T> list : lists) {
      if (list.get(position) != element) {
        return false;
      }
    }
    return true;
  }

  /**
   * @return the updated {@link String} with applied {@link Edit}s.
   */
  public static String applyReplaceEdits(String s, List<Edit> edits) {
    // sort edits
    edits = Lists.newArrayList(edits);
    Collections.sort(edits, new Comparator<Edit>() {
      @Override
      public int compare(Edit o1, Edit o2) {
        return o1.getOffset() - o2.getOffset();
      }
    });
    // apply edits
    int delta = 0;
    for (Edit edit : edits) {
      int editOffset = edit.getOffset() + delta;
      String beforeEdit = s.substring(0, editOffset);
      String afterEdit = s.substring(editOffset + edit.getLength());
      s = beforeEdit + edit.getReplacement() + afterEdit;
      delta += getDeltaOffset(edit);
    }
    // done
    return s;
  }

  /**
   * @return <code>true</code> if given {@link SourceRange} covers given {@link AstNode}.
   */
  public static boolean covers(SourceRange r, AstNode node) {
    SourceRange nodeRange = rangeNode(node);
    return r.covers(nodeRange);
  }

  /**
   * @return all direct children of the given {@link Element}.
   */
  public static List<Element> getChildren(Element parent) {
    return getChildren(parent, null);
  }

  /**
   * @param name the required name of children; may be <code>null</code> to get children with any
   *          name.
   * @return all direct children of the given {@link Element}, with given name.
   */
  public static List<Element> getChildren(final Element parent, final String name) {
    final List<Element> children = Lists.newArrayList();
    parent.accept(new GeneralizingElementVisitor<Void>() {
      @Override
      public Void visitElement(Element element) {
        if (element == parent) {
          super.visitElement(element);
        } else if (name == null || hasDisplayName(element, name)) {
          children.add(element);
        }
        return null;
      }
    });
    return children;
  }

  // TODO(scheglov) document and test
  public static String getDefaultValueCode(Type type) {
    if (type != null) {
      String typeName = type.getDisplayName();
      if (typeName.equals("bool")) {
        return "false";
      }
      if (typeName.equals("int")) {
        return "0";
      }
      if (typeName.equals("double")) {
        return "0.0";
      }
      if (typeName.equals("String")) {
        return "''";
      }
    }
    // no better guess
    return "null";
  }

  /**
   * @return the number of characters this {@link Edit} will move offsets after its range.
   */
  public static int getDeltaOffset(Edit edit) {
    return edit.getReplacement().length() - edit.getLength();
  }

  /**
   * @return the name of the {@link Element} kind.
   */
  public static String getElementKindName(Element element) {
    ElementKind kind = element.getKind();
    return getElementKindName(kind);
  }

  /**
   * @return the display name of the {@link ElementKind}.
   */
  public static String getElementKindName(ElementKind kind) {
    return kind.getDisplayName();
  }

  /**
   * @return the human name of the {@link Element}.
   */
  public static String getElementQualifiedName(Element element) {
    ElementKind kind = element.getKind();
    switch (kind) {
      case FIELD:
      case METHOD:
        return element.getEnclosingElement().getDisplayName() + "." + element.getDisplayName();
      default:
        return element.getDisplayName();
    }
  }

  /**
   * Returns a class or an unit member enclosing the given {@link AstNode}.
   */
  public static AstNode getEnclosingClassOrUnitMember(AstNode node) {
    AstNode member = node;
    while (node != null) {
      if (node instanceof ClassDeclaration) {
        return member;
      }
      if (node instanceof CompilationUnit) {
        return member;
      }
      member = node;
      node = node.getParent();
    }
    return null;
  }

  /**
   * @return the {@link ExecutableElement} of the enclosing executable {@link AstNode}.
   */
  public static ExecutableElement getEnclosingExecutableElement(AstNode node) {
    while (node != null) {
      if (node instanceof FunctionDeclaration) {
        return ((FunctionDeclaration) node).getElement();
      }
      if (node instanceof ConstructorDeclaration) {
        return ((ConstructorDeclaration) node).getElement();
      }
      if (node instanceof MethodDeclaration) {
        return ((MethodDeclaration) node).getElement();
      }
      node = node.getParent();
    }
    return null;
  }

  /**
   * @return the enclosing executable {@link AstNode}.
   */
  public static AstNode getEnclosingExecutableNode(AstNode node) {
    while (node != null) {
      if (node instanceof FunctionDeclaration) {
        return node;
      }
      if (node instanceof ConstructorDeclaration) {
        return node;
      }
      if (node instanceof MethodDeclaration) {
        return node;
      }
      node = node.getParent();
    }
    return null;
  }

  /**
   * @return {@link Element} exported from the given {@link LibraryElement}.
   */
  public static Element getExportedElement(LibraryElement library, String name) {
    if (library == null) {
      return null;
    }
    return getExportNamespace(library).get(name);
  }

  /**
   * TODO(scheglov) may be replace with some API for this
   * 
   * @return the namespace of the given {@link ExportElement}.
   */
  public static Map<String, Element> getExportNamespace(ExportElement exp) {
    Namespace namespace = new NamespaceBuilder().createExportNamespaceForDirective(exp);
    return namespace.getDefinedNames();
  }

  /**
   * TODO(scheglov) may be replace with some API for this
   * 
   * @return the export namespace of the given {@link LibraryElement}.
   */
  public static Map<String, Element> getExportNamespace(LibraryElement library) {
    Namespace namespace = new NamespaceBuilder().createExportNamespaceForLibrary(library);
    return namespace.getDefinedNames();
  }

  /**
   * @return {@link #getExpressionPrecedence(AstNode)} for parent node, or {@code 0} if parent node
   *         is {@link ParenthesizedExpression}. The reason is that {@code (expr)} is always
   *         executed after {@code expr}.
   */
  public static int getExpressionParentPrecedence(AstNode node) {
    AstNode parent = node.getParent();
    if (parent instanceof ParenthesizedExpression) {
      return 0;
    }
    return getExpressionPrecedence(parent);
  }

  /**
   * @return the precedence of the given node - result of {@link Expression#getPrecedence()} if an
   *         {@link Expression}, negative otherwise.
   */
  public static int getExpressionPrecedence(AstNode node) {
    if (node instanceof Expression) {
      return ((Expression) node).getPrecedence();
    }
    return -1000;
  }

  /**
   * TODO(scheglov) may be replace with some API for this
   * 
   * @return the namespace of the given {@link ImportElement}.
   */
  public static Map<String, Element> getImportNamespace(ImportElement imp) {
    Namespace namespace = new NamespaceBuilder().createImportNamespaceForDirective(imp);
    return namespace.getDefinedNames();
  }

  /**
   * @return all {@link CompilationUnitElement} the given {@link LibraryElement} consists of.
   */
  public static List<CompilationUnitElement> getLibraryUnits(LibraryElement library) {
    List<CompilationUnitElement> units = Lists.newArrayList();
    units.add(library.getDefiningCompilationUnit());
    Collections.addAll(units, library.getParts());
    return units;
  }

  /**
   * @return the line prefix from the given source, i.e. basically just whitespace prefix of the
   *         given {@link String}.
   */
  public static String getLinesPrefix(String lines) {
    int index = 0;
    while (index < lines.length()) {
      char c = lines.charAt(index);
      if (!Character.isWhitespace(c)) {
        break;
      }
      index++;
    }
    return lines.substring(0, index);
  }

  /**
   * @return the {@link LocalVariableElement} or {@link ParameterElement} if given
   *         {@link SimpleIdentifier} is the reference to local variable or parameter, or
   *         <code>null</code> in the other case.
   */
  public static VariableElement getLocalOrParameterVariableElement(SimpleIdentifier node) {
    Element element = node.getStaticElement();
    if (element instanceof LocalVariableElement) {
      return (LocalVariableElement) element;
    }
    if (element instanceof ParameterElement) {
      return (ParameterElement) element;
    }
    return null;
  }

  /**
   * @return the {@link LocalVariableElement} if given {@link SimpleIdentifier} is the reference to
   *         local variable, or <code>null</code> in the other case.
   */
  public static LocalVariableElement getLocalVariableElement(SimpleIdentifier node) {
    Element element = node.getStaticElement();
    if (element instanceof LocalVariableElement) {
      return (LocalVariableElement) element;
    }
    return null;
  }

  /**
   * @return the nearest common ancestor {@link AstNode} of the given {@link AstNode}s.
   */
  public static AstNode getNearestCommonAncestor(List<AstNode> nodes) {
    // may be no nodes
    if (nodes.isEmpty()) {
      return null;
    }
    // prepare parents
    List<List<AstNode>> parents = Lists.newArrayList();
    for (AstNode node : nodes) {
      parents.add(getParents(node));
    }
    // find min length
    int minLength = Integer.MAX_VALUE;
    for (List<AstNode> parentList : parents) {
      minLength = Math.min(minLength, parentList.size());
    }
    // find deepest parent
    int i = 0;
    for (; i < minLength; i++) {
      if (!allListsEqual(parents, i)) {
        break;
      }
    }
    return parents.get(0).get(i - 1);
  }

  /**
   * @return the {@link Expression} qualified if given node is name part of a {@link PropertyAccess}
   *         or {@link PrefixedIdentifier}. May be <code>null</code>.
   */
  public static Expression getNodeQualifier(SimpleIdentifier node) {
    AstNode parent = node.getParent();
    if (parent instanceof PropertyAccess) {
      PropertyAccess propertyAccess = (PropertyAccess) parent;
      if (propertyAccess.getPropertyName() == node) {
        return propertyAccess.getTarget();
      }
    }
    if (parent instanceof PrefixedIdentifier) {
      PrefixedIdentifier prefixed = (PrefixedIdentifier) parent;
      if (prefixed.getIdentifier() == node) {
        return prefixed.getPrefix();
      }
    }
    return null;
  }

  /**
   * @return the {@link ParameterElement} if given {@link SimpleIdentifier} is the reference to
   *         parameter, or <code>null</code> in the other case.
   */
  public static ParameterElement getParameterElement(SimpleIdentifier node) {
    Element element = node.getStaticElement();
    if (element instanceof ParameterElement) {
      return (ParameterElement) element;
    }
    return null;
  }

  /**
   * @return the precedence of the given {@link Expression} parent. May be {@code -1} no operator.
   * @see #getPrecedence(Expression)
   */
  public static int getParentPrecedence(Expression expression) {
    AstNode parent = expression.getParent();
    if (parent instanceof Expression) {
      return getPrecedence((Expression) parent);
    }
    return -1;
  }

  /**
   * @return parent {@link AstNode}s from {@link CompilationUnit} (at index "0") to the given one.
   */
  public static List<AstNode> getParents(AstNode node) {
    // prepare number of parents
    int numParents = 0;
    {
      AstNode current = node.getParent();
      while (current != null) {
        numParents++;
        current = current.getParent();
      }
    }
    // fill array of parents
    AstNode[] parents = new AstNode[numParents];
    AstNode current = node.getParent();
    int index = numParents;
    while (current != null) {
      parents[--index] = current;
      current = current.getParent();
    }
    return Arrays.asList(parents);
  }

  /**
   * @return the precedence of the given {@link Expression} operator. May be
   *         {@code Integer#MAX_VALUE} if not an operator.
   */
  public static int getPrecedence(Expression expression) {
    if (expression instanceof BinaryExpression) {
      BinaryExpression binaryExpression = (BinaryExpression) expression;
      return binaryExpression.getOperator().getType().getPrecedence();
    }
    if (expression instanceof PrefixExpression) {
      PrefixExpression prefixExpression = (PrefixExpression) expression;
      return prefixExpression.getOperator().getType().getPrecedence();
    }
    if (expression instanceof PostfixExpression) {
      PostfixExpression postfixExpression = (PostfixExpression) expression;
      return postfixExpression.getOperator().getType().getPrecedence();
    }
    return Integer.MAX_VALUE;
  }

  /**
   * @return the {@link PropertyAccessorElement} if given {@link SimpleIdentifier} is the reference
   *         to property, or <code>null</code> in the other case.
   */
  public static PropertyAccessorElement getPropertyAccessorElement(SimpleIdentifier node) {
    Element element = node.getStaticElement();
    if (element instanceof PropertyAccessorElement) {
      return (PropertyAccessorElement) element;
    }
    return null;
  }

  /**
   * If given {@link AstNode} is name of qualified property extraction, returns target from which
   * this property is extracted. Otherwise {@code null}.
   */
  public static Expression getQualifiedPropertyTarget(AstNode node) {
    AstNode parent = node.getParent();
    if (parent instanceof PrefixedIdentifier) {
      PrefixedIdentifier prefixed = (PrefixedIdentifier) parent;
      if (prefixed.getIdentifier() == node) {
        return ((PrefixedIdentifier) parent).getPrefix();
      }
    }
    if (parent instanceof PropertyAccess) {
      PropertyAccess access = (PropertyAccess) parent;
      if (access.getPropertyName() == node) {
        return access.getRealTarget();
      }
    }
    return null;
  }

  /**
   * Returns the name of the file which corresponds to the name of the class according to the style
   * guide. However class does not have to be in this file.
   */
  public static String getRecommentedFileNameForClass(String className) {
    int len = className.length();
    StringBuilder sb = new StringBuilder(len * 2);
    boolean prevWasUpper = false;
    for (int i = 0; i < len; i++) {
      char c = className.charAt(i);
      if (Character.isUpperCase(c)) {
        boolean nextIsUpper = i < len - 1 && Character.isUpperCase(className.charAt(i + 1));
        if (i == 0) {
          // HttpServer
          // ^
        } else if (prevWasUpper) {
          // HTTPServer
          //     ^
          if (!nextIsUpper) {
            sb.append('_');
          }
        } else {
          // HttpServer
          //     ^
          sb.append('_');
        }
        prevWasUpper = true;
        c = Character.toLowerCase(c);
      } else {
        prevWasUpper = false;
      }
      sb.append(c);
    }
    sb.append(".dart");
    String fileName = sb.toString();
    return fileName;
  }

  /**
   * @return given {@link Statement} if not {@link Block}, first child {@link Statement} if
   *         {@link Block}, or <code>null</code> if more than one child.
   */
  public static Statement getSingleStatement(Statement statement) {
    if (statement instanceof Block) {
      List<Statement> blockStatements = ((Block) statement).getStatements();
      if (blockStatements.size() != 1) {
        return null;
      }
      return blockStatements.get(0);
    }
    return statement;
  }

  /**
   * @return the {@link String} content of the given {@link Source}.
   */
  public static String getSourceContent(AnalysisContext context, Source source) throws Exception {
    return context.getContents(source).getData().toString();
  }

  /**
   * @return given {@link Statement} if not {@link Block}, all children {@link Statement}s if
   *         {@link Block}.
   */
  public static List<Statement> getStatements(Statement statement) {
    if (statement instanceof Block) {
      return ((Block) statement).getStatements();
    }
    return ImmutableList.of(statement);
  }

  /**
   * @return all top-level elements declared in the given {@link LibraryElement}.
   */
  public static List<Element> getTopLevelElements(LibraryElement library) {
    List<Element> elements = Lists.newArrayList();
    List<CompilationUnitElement> units = getLibraryUnits(library);
    for (CompilationUnitElement unit : units) {
      Collections.addAll(elements, unit.getFunctions());
      Collections.addAll(elements, unit.getFunctionTypeAliases());
      Collections.addAll(elements, unit.getTypes());
      Collections.addAll(elements, unit.getTopLevelVariables());
    }
    return elements;
  }

  /**
   * @return the possible names for variable with initializer of the given {@link StringLiteral}.
   */
  public static String[] getVariableNameSuggestions(String text, Set<String> excluded) {
    // filter out everything except of letters and white spaces
    {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < text.length(); i++) {
        char c = text.charAt(i);
        if (Character.isLetter(c) || Character.isWhitespace(c)) {
          sb.append(c);
        }
      }
      text = sb.toString();
    }
    // make single camel-case text
    {
      String[] words = StringUtils.split(text);
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < words.length; i++) {
        String word = words[i];
        if (i > 0) {
          word = StringUtils.capitalize(word);
        }
        sb.append(word);
      }
      text = sb.toString();
    }
    // split camel-case into separate suggested names
    Set<String> res = Sets.newLinkedHashSet();
    addAll(excluded, res, getVariableNameSuggestions(text));
    return res.toArray(new String[res.size()]);
  }

  /**
   * @return the possible names for variable with given expected type and expression.
   */
  public static String[] getVariableNameSuggestions(Type expectedType,
      Expression assignedExpression, Set<String> excluded) {
    Set<String> res = Sets.newLinkedHashSet();
    // use expression
    if (assignedExpression != null) {
      String nameFromExpression = getBaseNameFromExpression(assignedExpression);
      if (nameFromExpression != null) {
        nameFromExpression = StringUtils.removeStart(nameFromExpression, "_");
        addAll(excluded, res, getVariableNameSuggestions(nameFromExpression));
      }

      String nameFromParent = getBaseNameFromLocationInParent(assignedExpression);
      if (nameFromParent != null) {
        addAll(excluded, res, getVariableNameSuggestions(nameFromParent));
      }
    }
    // use type
    if (expectedType != null && !expectedType.isDynamic()) {
      String typeName = expectedType.getName();
      if ("int".equals(typeName)) {
        addSingleCharacterName(excluded, res, 'i');
      } else if ("double".equals(typeName)) {
        addSingleCharacterName(excluded, res, 'd');
      } else if ("String".equals(typeName)) {
        addSingleCharacterName(excluded, res, 's');
      } else {
        addAll(excluded, res, getVariableNameSuggestions(typeName));
      }
      res.remove(typeName);
    }
    // done
    return res.toArray(new String[res.size()]);
  }

  /**
   * @return {@code true} if the given {@link Element#getDisplayName()} equals to the given name.
   */
  public static boolean hasDisplayName(Element element, String name) {
    if (element == null) {
      return false;
    }
    String elementDisplayName = element.getDisplayName();
    return StringUtils.equals(elementDisplayName, name);
  }

  /**
   * @return {@code true} if the given {@link Element#getName()} equals to the given name.
   */
  public static boolean hasName(Element element, String name) {
    if (element == null) {
      return false;
    }
    String elementName = element.getName();
    return StringUtils.equals(elementName, name);
  }

  /**
   * @return {@code true} if the given {@link SimpleIdentifier} is the name of the
   *         {@link NamedExpression}.
   */
  public static boolean isNamedExpressionName(SimpleIdentifier node) {
    AstNode parent = node.getParent();
    if (parent instanceof Label) {
      Label label = (Label) parent;
      if (label.getLabel() == node) {
        AstNode parent2 = label.getParent();
        if (parent2 instanceof NamedExpression) {
          return ((NamedExpression) parent2).getName() == label;
        }
      }
    }
    return false;
  }

  /**
   * Adds "toAdd" items which are not excluded.
   */
  private static void addAll(Set<String> excluded, Set<String> result, Collection<String> toAdd) {
    for (String item : toAdd) {
      // add name based on "item", but not "excluded"
      for (int suffix = 1;; suffix++) {
        // prepare name, just "item" or "item2", "item3", etc
        String name = item;
        if (suffix > 1) {
          name += Integer.toString(suffix);
        }
        // add once found not excluded
        if (!excluded.contains(name)) {
          result.add(name);
          break;
        }
      }
    }
  }

  /**
   * Add to "result" then given "c" or the first ASCII character after it.
   */
  private static void addSingleCharacterName(Set<String> excluded, Set<String> result, char c) {
    while (c < 'z') {
      String name = String.valueOf(c);
      // may be done
      if (!excluded.contains(name)) {
        result.add(name);
        break;
      }
      // next character
      c = (char) (c + 1);
    }
  }

  private static String getBaseNameFromExpression(Expression expression) {
    String name = null;
    // e as Type
    if (expression instanceof AsExpression) {
      AsExpression asExpression = (AsExpression) expression;
      expression = asExpression.getExpression();
    }
    // analyze expressions
    if (expression instanceof SimpleIdentifier) {
      SimpleIdentifier node = (SimpleIdentifier) expression;
      return node.getName();
    } else if (expression instanceof PrefixedIdentifier) {
      PrefixedIdentifier node = (PrefixedIdentifier) expression;
      return node.getIdentifier().getName();
    } else if (expression instanceof MethodInvocation) {
      name = ((MethodInvocation) expression).getMethodName().getName();
    } else if (expression instanceof InstanceCreationExpression) {
      InstanceCreationExpression creation = (InstanceCreationExpression) expression;
      ConstructorName constructorName = creation.getConstructorName();
      TypeName typeName = constructorName.getType();
      if (typeName != null) {
        Identifier typeNameIdentifier = typeName.getName();
        // new ClassName()
        if (typeNameIdentifier instanceof SimpleIdentifier) {
          return typeNameIdentifier.getName();
        }
        // new prefix.name();
        if (typeNameIdentifier instanceof PrefixedIdentifier) {
          PrefixedIdentifier prefixed = (PrefixedIdentifier) typeNameIdentifier;
          // new prefix.ClassName()
          if (prefixed.getPrefix().getStaticElement() instanceof PrefixElement) {
            return prefixed.getIdentifier().getName();
          }
          // new ClassName.constructorName()
          return prefixed.getPrefix().getName();
        }
      }
    }
    // strip known prefixes
    if (name != null) {
      for (int i = 0; i < KNOWN_METHOD_NAME_PREFIXES.length; i++) {
        String curr = KNOWN_METHOD_NAME_PREFIXES[i];
        if (name.startsWith(curr)) {
          if (name.equals(curr)) {
            return null; // don't suggest 'get' as variable name
          } else if (Character.isUpperCase(name.charAt(curr.length()))) {
            return name.substring(curr.length());
          }
        }
      }
    }
    // done
    return name;
  }

  private static String getBaseNameFromLocationInParent(Expression expression) {
    // value in named expression
    if (expression.getParent() instanceof NamedExpression) {
      NamedExpression namedExpression = (NamedExpression) expression.getParent();
      if (namedExpression.getExpression() == expression) {
        return namedExpression.getName().getLabel().getName();
      }
    }
    // positional argument
    {
      ParameterElement parameter = expression.getPropagatedParameterElement();
      if (parameter == null) {
        parameter = expression.getStaticParameterElement();
      }
      if (parameter != null) {
        return parameter.getDisplayName();
      }
    }
    // unknown
    return null;
  }

  /**
   * @return {@link Expression}s from <code>operands</code> which are completely covered by given
   *         {@link SourceRange}. Range should start and end between given {@link Expression}s.
   */
  private static List<Expression> getOperandsForSourceRange(List<Expression> operands,
      SourceRange range) {
    assert !operands.isEmpty();
    List<Expression> subOperands = Lists.newArrayList();
    // track range enter/exit
    boolean entered = false;
    boolean exited = false;
    // may be range starts before or on first operand
    if (range.getOffset() <= operands.get(0).getOffset()) {
      entered = true;
    }
    // iterate over gaps between operands
    for (int i = 0; i < operands.size() - 1; i++) {
      Expression operand = operands.get(i);
      Expression nextOperand = operands.get(i + 1);
      SourceRange inclusiveGap = rangeEndStart(operand, nextOperand).getMoveEnd(1);
      // add operand, if already entered range
      if (entered) {
        subOperands.add(operand);
        // may be last operand in range
        if (range.endsIn(inclusiveGap)) {
          exited = true;
        }
      } else {
        // may be first operand in range
        if (range.startsIn(inclusiveGap)) {
          entered = true;
        }
      }
    }
    // check if last operand is in range
    Expression lastGroupMember = operands.get(operands.size() - 1);
    if (range.getEnd() == lastGroupMember.getEnd()) {
      subOperands.add(lastGroupMember);
      exited = true;
    }
    // we expect that range covers only given operands
    if (!exited) {
      return Lists.newArrayList();
    }
    // done
    return subOperands;
  }

  /**
   * @return all operands of the given {@link BinaryExpression} and its children with the same
   *         operator.
   */
  private static List<Expression> getOperandsInOrderFor(BinaryExpression groupRoot) {
    final List<Expression> operands = Lists.newArrayList();
    final TokenType groupOperatorType = groupRoot.getOperator().getType();
    groupRoot.accept(new GeneralizingAstVisitor<Void>() {
      @Override
      public Void visitExpression(Expression node) {
        if (node instanceof BinaryExpression
            && ((BinaryExpression) node).getOperator().getType() == groupOperatorType) {
          return super.visitNode(node);
        }
        operands.add(node);
        return null;
      }
    });
    return operands;
  }

  /**
   * @return all variants of names by removing leading words by one.
   */
  private static List<String> getVariableNameSuggestions(String name) {
    List<String> result = Lists.newArrayList();
    String[] parts = name.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
    for (int i = 0; i < parts.length; i++) {
      String suggestion = parts[i].toLowerCase() + StringUtils.join(parts, "", i + 1, parts.length);
      result.add(suggestion);
    }
    return result;
  }

  /**
   * Adds enclosing parenthesis if the precedence of the {@link InvertedCondition} if less than the
   * precedence of the expression we are going it to use in.
   */
  private static String parenthesizeIfRequired(InvertedCondition expr, int newOperatorPrecedence) {
    if (expr.precedence < newOperatorPrecedence) {
      return "(" + expr.source + ")";
    }
    return expr.source;
  }

  private final CompilationUnit unit;

  private final LibraryElement library;

  private final String buffer;

  private String endOfLine;

  public CorrectionUtils(CompilationUnit unit) throws Exception {
    this.unit = unit;
    CompilationUnitElement element = unit.getElement();
    this.library = element.getLibrary();
    this.buffer = getSourceContent(element.getContext(), element.getSource());
  }

  /**
   * @return the source of the given {@link SourceRange} with indentation changed from "oldIndent"
   *         to "newIndent", keeping indentation of the lines relative to each other.
   */
  public Edit createIndentEdit(SourceRange range, String oldIndent, String newIndent) {
    String newSource = getIndentSource(range, oldIndent, newIndent);
    return new Edit(range.getOffset(), range.getLength(), newSource);
  }

  /**
   * @return the {@link AstNode} that encloses the given offset.
   */
  public AstNode findNode(int offset) {
    return new NodeLocator(offset).searchWithin(unit);
  }

  /**
   * TODO(scheglov) replace with nodes once there will be {@link CompilationUnit#getComments()}.
   * 
   * @return the {@link SourceRange}s of all comments in {@link CompilationUnit}.
   */
  public List<SourceRange> getCommentRanges() {
    List<SourceRange> ranges = Lists.newArrayList();
    Token token = unit.getBeginToken();
    while (token != null && token.getType() != TokenType.EOF) {
      Token commentToken = token.getPrecedingComments();
      while (commentToken != null) {
        ranges.add(SourceRangeFactory.rangeToken(commentToken));
        commentToken = commentToken.getNext();
      }
      token = token.getNext();
    }
    return ranges;
  }

  /**
   * @return the EOL to use for this {@link CompilationUnit}.
   */
  public String getEndOfLine() {
    if (endOfLine == null) {
      if (buffer.contains("\r\n")) {
        endOfLine = "\r\n";
      } else {
        endOfLine = "\n";
      }
    }
    return endOfLine;
  }

  /**
   * @return the default indentation with given level.
   */
  public String getIndent(int level) {
    return StringUtils.repeat("  ", level);
  }

  /**
   * @return the source of the given {@link SourceRange} with indentation changed from "oldIndent"
   *         to "newIndent", keeping indentation of the lines relative to each other.
   */
  public String getIndentSource(SourceRange range, String oldIndent, String newIndent) {
    String oldSource = getText(range);
    return getIndentSource(oldSource, oldIndent, newIndent);
  }

  /**
   * Indents given source left or right.
   * 
   * @return the source with changed indentation.
   */
  public String getIndentSource(String source, boolean right) {
    StringBuilder sb = new StringBuilder();
    String indent = getIndent(1);
    String eol = getEndOfLine();
    String[] lines = StringUtils.splitByWholeSeparatorPreserveAllTokens(source, eol);
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      // last line, stop if empty
      if (i == lines.length - 1 && StringUtils.isEmpty(line)) {
        break;
      }
      // update line
      if (right) {
        line = indent + line;
      } else {
        line = StringUtils.removeStart(line, indent);
      }
      // append line
      sb.append(line);
      sb.append(eol);
    }
    return sb.toString();
  }

  /**
   * @return the source with indentation changed from "oldIndent" to "newIndent", keeping
   *         indentation of the lines relative to each other.
   */
  public String getIndentSource(String source, String oldIndent, String newIndent) {
    // prepare STRING token ranges
    List<SourceRange> lineRanges = Lists.newArrayList();
    for (Token token : TokenUtils.getTokens(source)) {
      if (token.getType() == TokenType.STRING) {
        lineRanges.add(rangeToken(token));
      }
    }
    // re-indent lines
    StringBuilder sb = new StringBuilder();
    String eol = getEndOfLine();
    String[] lines = StringUtils.splitByWholeSeparatorPreserveAllTokens(source, eol);
    int lineOffset = 0;
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      // last line, stop if empty
      if (i == lines.length - 1 && StringUtils.isEmpty(line)) {
        break;
      }
      // check if "offset" is in one of the String ranges
      boolean inString = false;
      for (SourceRange lineRange : lineRanges) {
        inString |= lineOffset > lineRange.getOffset() && lineOffset < lineRange.getEnd();
        if (lineOffset > lineRange.getEnd()) {
          break;
        }
      }
      lineOffset += line.length() + eol.length();
      // update line indent
      if (!inString) {
        line = newIndent + StringUtils.removeStart(line, oldIndent);
      }
      // append line
      sb.append(line);
      sb.append(eol);
    }
    return sb.toString();
  }

  /**
   * @return {@link InsertDesc}, description where to insert new library-related directive.
   */
  public InsertDesc getInsertDescImport() {
    // analyze directives
    Directive prevDirective = null;
    for (Directive directive : unit.getDirectives()) {
      if (directive instanceof LibraryDirective || directive instanceof ImportDirective
          || directive instanceof ExportDirective) {
        prevDirective = directive;
      }
    }
    // insert after last library-related directive
    if (prevDirective != null) {
      InsertDesc result = new InsertDesc();
      result.offset = prevDirective.getEnd();
      String eol = getEndOfLine();
      if (prevDirective instanceof LibraryDirective) {
        result.prefix = eol + eol;
      } else {
        result.prefix = eol;
      }
      return result;
    }
    // no directives, use "top" location
    return getInsertDescTop();
  }

  /**
   * @return {@link InsertDesc}, description where to insert new 'part 'directive.
   */
  public InsertDesc getInsertDescPart() {
    // analyze directives
    Directive prevDirective = null;
    for (Directive directive : unit.getDirectives()) {
      prevDirective = directive;
    }
    // insert after last directive
    if (prevDirective != null) {
      InsertDesc result = new InsertDesc();
      result.offset = prevDirective.getEnd();
      String eol = getEndOfLine();
      if (prevDirective instanceof PartDirective) {
        result.prefix = eol;
      } else {
        result.prefix = eol + eol;
      }
      return result;
    }
    // no directives, use "top" location
    return getInsertDescTop();
  }

  /**
   * @return {@link InsertDesc}, description where to insert new directive or top-level declaration
   *         at the top of file.
   */
  public InsertDesc getInsertDescTop() {
    // skip leading line comments
    int offset = 0;
    boolean insertEmptyLineBefore = false;
    boolean insertEmptyLineAfter = false;
    String source = getText();
    // skip hash-bang
    if (offset < source.length() - 2) {
      String linePrefix = getText(offset, 2);
      if (linePrefix.equals("#!")) {
        insertEmptyLineBefore = true;
        offset = getLineNext(offset);
        // skip empty lines to first line comment
        int emptyOffset = offset;
        while (emptyOffset < source.length() - 2) {
          int nextLineOffset = getLineNext(emptyOffset);
          String line = source.substring(emptyOffset, nextLineOffset);
          if (line.trim().isEmpty()) {
            emptyOffset = nextLineOffset;
            continue;
          } else if (line.startsWith("//")) {
            offset = emptyOffset;
            break;
          } else {
            break;
          }
        }
      }
    }
    // skip line comments
    while (offset < source.length() - 2) {
      String linePrefix = getText(offset, 2);
      if (linePrefix.equals("//")) {
        insertEmptyLineBefore = true;
        offset = getLineNext(offset);
      } else {
        break;
      }
    }
    // determine if empty line is required after
    int nextLineOffset = getLineNext(offset);
    String insertLine = source.substring(offset, nextLineOffset);
    if (!insertLine.trim().isEmpty()) {
      insertEmptyLineAfter = true;
    }
    // fill InsertDesc
    InsertDesc desc = new InsertDesc();
    desc.offset = offset;
    if (insertEmptyLineBefore) {
      desc.prefix = getEndOfLine();
    }
    if (insertEmptyLineAfter) {
      desc.suffix = getEndOfLine();
    }
    return desc;
  }

  /**
   * Skips whitespace characters and single EOL on the right from the given position. If from
   * statement or method end, then this is in the most cases start of the next line.
   */
  public int getLineContentEnd(int index) {
    int length = buffer.length();
    // skip whitespace characters
    while (index < length) {
      char c = buffer.charAt(index);
      if (!Character.isWhitespace(c) || c == '\r' || c == '\n') {
        break;
      }
      index++;
    }
    // skip single \r
    if (index < length && buffer.charAt(index) == '\r') {
      index++;
    }
    // skip single \n
    if (index < length && buffer.charAt(index) == '\n') {
      index++;
    }
    // done
    return index;
  }

  /**
   * @return the index of the last space or tab on the left from the given one, if from statement or
   *         method start, then this is in most cases start of the line.
   */
  public int getLineContentStart(int index) {
    while (index > 0) {
      char c = buffer.charAt(index - 1);
      if (c != ' ' && c != '\t') {
        break;
      }
      index--;
    }
    return index;
  }

  /**
   * @return the start index of the next line after the line which contains given index.
   */
  public int getLineNext(int index) {
    int length = buffer.length();
    // skip to the end of the line
    while (index < length) {
      char c = buffer.charAt(index);
      if (c == '\r' || c == '\n') {
        break;
      }
      index++;
    }
    // skip single \r
    if (index < length && buffer.charAt(index) == '\r') {
      index++;
    }
    // skip single \n
    if (index < length && buffer.charAt(index) == '\n') {
      index++;
    }
    // done
    return index;
  }

  /**
   * @return the whitespace prefix of the line which contains given offset.
   */
  public String getLinePrefix(int index) {
    int lineStart = getLineThis(index);
    int length = buffer.length();
    int lineNonWhitespace = lineStart;
    while (lineNonWhitespace < length) {
      char c = buffer.charAt(lineNonWhitespace);
      if (c == '\r' || c == '\n') {
        break;
      }
      if (!Character.isWhitespace(c)) {
        break;
      }
      lineNonWhitespace++;
    }
    return getText(lineStart, lineNonWhitespace - lineStart);
  }

  /**
   * @return the {@link #getLinesRange(SourceRange)} for given {@link Statement}s.
   */
  public SourceRange getLinesRange(List<Statement> statements) {
    SourceRange range = rangeNodes(statements);
    return getLinesRange(range);
  }

  /**
   * @return the {@link SourceRange} which starts at the start of the line of "offset" and ends at
   *         the start of the next line after "end" of the given {@link SourceRange}, i.e. basically
   *         complete lines of the source for given {@link SourceRange}.
   */
  public SourceRange getLinesRange(SourceRange range) {
    // start
    int startOffset = range.getOffset();
    int startLineOffset = getLineContentStart(startOffset);
    // end
    int endOffset = range.getEnd();
    int afterEndLineOffset = getLineContentEnd(endOffset);
    // range
    return rangeStartEnd(startLineOffset, afterEndLineOffset);
  }

  /**
   * @return the {@link #getLinesRange(SourceRange)} for given {@link Statement}s.
   */
  public SourceRange getLinesRange(Statement... statements) {
    return getLinesRange(Lists.newArrayList(statements));
  }

  /**
   * @return the start index of the line which contains given index.
   */
  public int getLineThis(int index) {
    while (index > 0) {
      char c = buffer.charAt(index - 1);
      if (c == '\r' || c == '\n') {
        break;
      }
      index--;
    }
    return index;
  }

  /**
   * @return the line prefix consisting of spaces and tabs on the left from the given
   *         {@link AstNode}.
   */
  public String getNodePrefix(AstNode node) {
    int offset = node.getOffset();
    // function literal is special, it uses offset of enclosing line
    if (node instanceof FunctionExpression) {
      return getLinePrefix(offset);
    }
    // use just prefix directly before node
    return getPrefix(offset);
  }

  /**
   * @return the index of the first non-whitespace character after given index.
   */
  public int getNonWhitespaceForward(int index) {
    int length = buffer.length();
    // skip whitespace characters
    while (index < length) {
      char c = buffer.charAt(index);
      if (!Character.isWhitespace(c)) {
        break;
      }
      index++;
    }
    // done
    return index;
  }

  /**
   * @return the source for the parameter with the given type and name.
   */
  public String getParameterSource(Type type, String name) {
    // no type
    if (type == null || type.isDynamic()) {
      return name;
    }
    // function type
    if (type instanceof FunctionType) {
      FunctionType functionType = (FunctionType) type;
      StringBuilder sb = new StringBuilder();
      // return type
      Type returnType = functionType.getReturnType();
      if (returnType != null && !returnType.isDynamic()) {
        sb.append(getTypeSource(returnType));
        sb.append(' ');
      }
      // parameter name
      sb.append(name);
      // parameters
      sb.append('(');
      ParameterElement[] fParameters = functionType.getParameters();
      for (int i = 0; i < fParameters.length; i++) {
        ParameterElement fParameter = fParameters[i];
        if (i != 0) {
          sb.append(", ");
        }
        sb.append(getParameterSource(fParameter.getType(), fParameter.getName()));
      }
      sb.append(')');
      // done
      return sb.toString();
    }
    // simple type
    return getTypeSource(type) + " " + name;
  }

  /**
   * @return the line prefix consisting of spaces and tabs on the left from the given offset.
   */
  public String getPrefix(int endIndex) {
    int startIndex = getLineContentStart(endIndex);
    return buffer.substring(startIndex, endIndex);
  }

  /**
   * @return the full text of unit.
   */
  public String getText() {
    return buffer;
  }

  /**
   * @return the given range of text from unit.
   */
  public String getText(AstNode node) {
    return getText(node.getOffset(), node.getLength());
  }

  /**
   * @return the given range of text from unit.
   */
  public String getText(int offset, int length) {
    return buffer.substring(offset, offset + length);
  }

  /**
   * @return the given range of text from unit.
   */
  public String getText(SourceRange range) {
    return getText(range.getOffset(), range.getLength());
  }

  /**
   * @return the actual type source of the given {@link Expression}, may be {@code null} if can not
   *         be resolved, should be treated as <code>Dynamic</code>.
   */
  public String getTypeSource(Expression expression) {
    if (expression == null) {
      return null;
    }
    Type type = expression.getBestType();
    String typeSource = getTypeSource(type);
    if ("dynamic".equals(typeSource)) {
      return null;
    }
    return typeSource;
  }

  /**
   * @return the source to reference the given {@link Type} in this {@link CompilationUnit}.
   */
  public String getTypeSource(Type type) {
    StringBuilder sb = new StringBuilder();
    // prepare element
    Element element = type.getElement();
    if (element == null) {
      String source = type.toString();
      source = StringUtils.remove(source, "<dynamic>");
      source = StringUtils.remove(source, "<dynamic, dynamic>");
      return source;
    }
    // append prefix
    {
      ImportElement imp = getImportElement(element);
      if (imp != null && imp.getPrefix() != null) {
        sb.append(imp.getPrefix().getDisplayName());
        sb.append(".");
      }
    }
    // append simple name
    String name = element.getDisplayName();
    sb.append(name);
    // may be type arguments
    if (type instanceof InterfaceType) {
      InterfaceType interfaceType = (InterfaceType) type;
      Type[] arguments = interfaceType.getTypeArguments();
      // check if has arguments
      boolean hasArguments = false;
      for (Type argument : arguments) {
        if (!argument.isDynamic()) {
          hasArguments = true;
          break;
        }
      }
      // append type arguments
      if (hasArguments) {
        sb.append("<");
        for (int i = 0; i < arguments.length; i++) {
          Type argument = arguments[i];
          if (i != 0) {
            sb.append(", ");
          }
          sb.append(getTypeSource(argument));
        }
        sb.append(">");
      }
    }
    // done
    return sb.toString();
  }

  /**
   * @return the underlying {@link CompilationUnit}.
   */
  public CompilationUnit getUnit() {
    return unit;
  }

  /**
   * @return the source of the inverted condition for the given logical expression.
   */
  public String invertCondition(Expression expression) {
    return invertCondition0(expression).source;
  }

  /**
   * @return <code>true</code> if selection range contains only whitespace.
   */
  public boolean isJustWhitespace(SourceRange range) {
    return getText(range).trim().length() == 0;
  }

  /**
   * @return <code>true</code> if selection range contains only whitespace or comments
   */
  public boolean isJustWhitespaceOrComment(SourceRange range) {
    String trimmedText = getText(range).trim();
    // may be whitespace
    if (trimmedText.isEmpty()) {
      return true;
    }
    // may be comment
    return TokenUtils.getTokens(trimmedText).isEmpty();
  }

  /**
   * @return <code>true</code> if "selection" covers "node" and there are any non-whitespace tokens
   *         between "selection" and "node" start/end.
   */
  public boolean selectionIncludesNonWhitespaceOutsideNode(SourceRange selection, AstNode node) {
    return selectionIncludesNonWhitespaceOutsideRange(selection, rangeNode(node));
  }

  /**
   * @return <code>true</code> if given range of {@link BinaryExpression} can be extracted.
   */
  public boolean validateBinaryExpressionRange(BinaryExpression binaryExpression, SourceRange range) {
    // only parts of associative expression are safe to extract
    if (!binaryExpression.getOperator().getType().isAssociativeOperator()) {
      return false;
    }
    // prepare selected operands
    List<Expression> operands = getOperandsInOrderFor(binaryExpression);
    List<Expression> subOperands = getOperandsForSourceRange(operands, range);
    // if empty, then something wrong with selection
    if (subOperands.isEmpty()) {
      return false;
    }
    // may be some punctuation included into selection - operators, braces, etc
    if (selectionIncludesNonWhitespaceOutsideOperands(range, subOperands)) {
      return false;
    }
    // OK
    return true;
  }

  /**
   * @return the {@link ImportElement} used to import given {@link Element} into {@link #library}.
   *         May be {@code null} if was not imported, i.e. declared in the same library.
   */
  private ImportElement getImportElement(Element element) {
    for (ImportElement imp : library.getImports()) {
      Map<String, Element> definedNames = getImportNamespace(imp);
      if (definedNames.containsValue(element)) {
        return imp;
      }
    }
    return null;
  }

  /**
   * @return the {@link InvertedCondition} for the given logical expression.
   */
  private InvertedCondition invertCondition0(Expression expression) {
    if (expression instanceof BooleanLiteral) {
      BooleanLiteral literal = (BooleanLiteral) expression;
      if (literal.getValue()) {
        return InvertedCondition.simple("false");
      } else {
        return InvertedCondition.simple("true");
      }
    }
    if (expression instanceof BinaryExpression) {
      BinaryExpression binary = (BinaryExpression) expression;
      TokenType operator = binary.getOperator().getType();
      Expression le = binary.getLeftOperand();
      Expression re = binary.getRightOperand();
      InvertedCondition ls = invertCondition0(le);
      InvertedCondition rs = invertCondition0(re);
      if (operator == TokenType.LT) {
        return InvertedCondition.binary(ls, " >= ", rs);
      }
      if (operator == TokenType.GT) {
        return InvertedCondition.binary(ls, " <= ", rs);
      }
      if (operator == TokenType.LT_EQ) {
        return InvertedCondition.binary(ls, " > ", rs);
      }
      if (operator == TokenType.GT_EQ) {
        return InvertedCondition.binary(ls, " < ", rs);
      }
      if (operator == TokenType.EQ_EQ) {
        return InvertedCondition.binary(ls, " != ", rs);
      }
      if (operator == TokenType.BANG_EQ) {
        return InvertedCondition.binary(ls, " == ", rs);
      }
      if (operator == TokenType.AMPERSAND_AMPERSAND) {
        int newPrecedence = TokenType.BAR_BAR.getPrecedence();
        return InvertedCondition.binary(newPrecedence, ls, " || ", rs);
      }
      if (operator == TokenType.BAR_BAR) {
        int newPrecedence = TokenType.AMPERSAND_AMPERSAND.getPrecedence();
        return InvertedCondition.binary(newPrecedence, ls, " && ", rs);
      }
    }
    if (expression instanceof IsExpression) {
      IsExpression isExpression = (IsExpression) expression;
      String expressionSource = getText(isExpression.getExpression());
      String typeSource = getText(isExpression.getType());
      if (isExpression.getNotOperator() == null) {
        return InvertedCondition.simple(expressionSource + " is! " + typeSource);
      } else {
        return InvertedCondition.simple(expressionSource + " is " + typeSource);
      }
    }
    if (expression instanceof PrefixExpression) {
      PrefixExpression prefixExpression = (PrefixExpression) expression;
      TokenType operator = prefixExpression.getOperator().getType();
      if (operator == TokenType.BANG) {
        Expression operand = prefixExpression.getOperand();
        while (operand instanceof ParenthesizedExpression) {
          ParenthesizedExpression pe = (ParenthesizedExpression) operand;
          operand = pe.getExpression();
        }
        return InvertedCondition.simple(getText(operand));
      }
    }
    if (expression instanceof ParenthesizedExpression) {
      ParenthesizedExpression pe = (ParenthesizedExpression) expression;
      Expression innerExpresion = pe.getExpression();
      while (innerExpresion instanceof ParenthesizedExpression) {
        innerExpresion = ((ParenthesizedExpression) innerExpresion).getExpression();
      }
      return invertCondition0(innerExpresion);
    }
    Type type = expression.getBestType();
    if (type.getDisplayName().equals("bool")) {
      return InvertedCondition.simple("!" + getText(expression));
    }
    return InvertedCondition.simple(getText(expression));
  }

  private boolean selectionIncludesNonWhitespaceOutsideOperands(SourceRange selection,
      List<Expression> operands) {
    return selectionIncludesNonWhitespaceOutsideRange(selection, rangeNodes(operands));
  }

  /**
   * @return <code>true</code> if "selection" covers "range" and there are any non-whitespace tokens
   *         between "selection" and "range" start/end.
   */
  private boolean selectionIncludesNonWhitespaceOutsideRange(SourceRange selection,
      SourceRange range) {
    // selection should cover range
    if (!selection.covers(range)) {
      return false;
    }
    // non-whitespace between selection start and range start
    if (!isJustWhitespaceOrComment(rangeStartStart(selection, range))) {
      return true;
    }
    // non-whitespace after range
    if (!isJustWhitespaceOrComment(rangeEndEnd(range, selection))) {
      return true;
    }
    // only whitespace in selection around range
    return false;
  }
}
