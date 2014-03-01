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
package com.google.dart.engine.parser;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.ast.Annotation;
import com.google.dart.engine.ast.ArgumentDefinitionTest;
import com.google.dart.engine.ast.AsExpression;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.BooleanLiteral;
import com.google.dart.engine.ast.CascadeExpression;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConditionalExpression;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ConstructorName;
import com.google.dart.engine.ast.DoubleLiteral;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.FunctionExpressionInvocation;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.IndexExpression;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.IntegerLiteral;
import com.google.dart.engine.ast.IsExpression;
import com.google.dart.engine.ast.LibraryIdentifier;
import com.google.dart.engine.ast.ListLiteral;
import com.google.dart.engine.ast.MapLiteral;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NamedExpression;
import com.google.dart.engine.ast.NullLiteral;
import com.google.dart.engine.ast.ParenthesizedExpression;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.PartOfDirective;
import com.google.dart.engine.ast.PostfixExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.RedirectingConstructorInvocation;
import com.google.dart.engine.ast.RethrowExpression;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.StringInterpolation;
import com.google.dart.engine.ast.SuperConstructorInvocation;
import com.google.dart.engine.ast.SuperExpression;
import com.google.dart.engine.ast.SymbolLiteral;
import com.google.dart.engine.ast.ThisExpression;
import com.google.dart.engine.ast.ThrowExpression;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExportElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.internal.element.AuxiliaryElements;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.ExportElementImpl;
import com.google.dart.engine.internal.element.ImportElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.type.Type;

import static com.google.dart.engine.ast.AstFactory.annotation;
import static com.google.dart.engine.ast.AstFactory.argumentDefinitionTest;
import static com.google.dart.engine.ast.AstFactory.asExpression;
import static com.google.dart.engine.ast.AstFactory.assignmentExpression;
import static com.google.dart.engine.ast.AstFactory.binaryExpression;
import static com.google.dart.engine.ast.AstFactory.booleanLiteral;
import static com.google.dart.engine.ast.AstFactory.cascadeExpression;
import static com.google.dart.engine.ast.AstFactory.compilationUnit;
import static com.google.dart.engine.ast.AstFactory.conditionalExpression;
import static com.google.dart.engine.ast.AstFactory.constructorDeclaration;
import static com.google.dart.engine.ast.AstFactory.constructorName;
import static com.google.dart.engine.ast.AstFactory.doubleLiteral;
import static com.google.dart.engine.ast.AstFactory.emptyFunctionBody;
import static com.google.dart.engine.ast.AstFactory.exportDirective;
import static com.google.dart.engine.ast.AstFactory.formalParameterList;
import static com.google.dart.engine.ast.AstFactory.functionExpression;
import static com.google.dart.engine.ast.AstFactory.functionExpressionInvocation;
import static com.google.dart.engine.ast.AstFactory.identifier;
import static com.google.dart.engine.ast.AstFactory.importDirective;
import static com.google.dart.engine.ast.AstFactory.indexExpression;
import static com.google.dart.engine.ast.AstFactory.instanceCreationExpression;
import static com.google.dart.engine.ast.AstFactory.integer;
import static com.google.dart.engine.ast.AstFactory.interpolationString;
import static com.google.dart.engine.ast.AstFactory.isExpression;
import static com.google.dart.engine.ast.AstFactory.libraryIdentifier;
import static com.google.dart.engine.ast.AstFactory.listLiteral;
import static com.google.dart.engine.ast.AstFactory.mapLiteral;
import static com.google.dart.engine.ast.AstFactory.methodInvocation;
import static com.google.dart.engine.ast.AstFactory.namedExpression;
import static com.google.dart.engine.ast.AstFactory.nullLiteral;
import static com.google.dart.engine.ast.AstFactory.parenthesizedExpression;
import static com.google.dart.engine.ast.AstFactory.partDirective;
import static com.google.dart.engine.ast.AstFactory.partOfDirective;
import static com.google.dart.engine.ast.AstFactory.postfixExpression;
import static com.google.dart.engine.ast.AstFactory.prefixExpression;
import static com.google.dart.engine.ast.AstFactory.propertyAccess;
import static com.google.dart.engine.ast.AstFactory.redirectingConstructorInvocation;
import static com.google.dart.engine.ast.AstFactory.rethrowExpression;
import static com.google.dart.engine.ast.AstFactory.string;
import static com.google.dart.engine.ast.AstFactory.superConstructorInvocation;
import static com.google.dart.engine.ast.AstFactory.superExpression;
import static com.google.dart.engine.ast.AstFactory.symbolLiteral;
import static com.google.dart.engine.ast.AstFactory.thisExpression;
import static com.google.dart.engine.ast.AstFactory.throwExpression;
import static com.google.dart.engine.ast.AstFactory.typeName;
import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.constructorElement;
import static com.google.dart.engine.element.ElementFactory.methodElement;
import static com.google.dart.engine.element.ElementFactory.topLevelVariableElement;

public class ResolutionCopierTest extends EngineTestCase {
  public void test_visitAnnotation() {
    String annotationName = "proxy";
    Annotation fromNode = annotation(identifier(annotationName));
    Element element = topLevelVariableElement(annotationName);
    fromNode.setElement(element);
    Annotation toNode = annotation(identifier(annotationName));

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(element, toNode.getElement());
  }

  public void test_visitArgumentDefinitionTest() {
    String identifier = "p";
    ArgumentDefinitionTest fromNode = argumentDefinitionTest(identifier);
    Type propagatedType = classElement("A").getType();
    fromNode.setPropagatedType(propagatedType);
    Type staticType = classElement("B").getType();
    fromNode.setStaticType(staticType);
    ArgumentDefinitionTest toNode = argumentDefinitionTest(identifier);

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitAsExpression() {
    AsExpression fromNode = asExpression(identifier("x"), typeName("A"));
    Type propagatedType = classElement("A").getType();
    fromNode.setPropagatedType(propagatedType);
    Type staticType = classElement("B").getType();
    fromNode.setStaticType(staticType);
    AsExpression toNode = asExpression(identifier("x"), typeName("A"));

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitAssignmentExpression() {
    AssignmentExpression fromNode = assignmentExpression(
        identifier("a"),
        TokenType.PLUS_EQ,
        identifier("b"));
    Type propagatedType = classElement("C").getType();
    MethodElement propagatedElement = methodElement("+", propagatedType);
    fromNode.setPropagatedElement(propagatedElement);
    fromNode.setPropagatedType(propagatedType);
    Type staticType = classElement("C").getType();
    MethodElement staticElement = methodElement("+", staticType);
    fromNode.setStaticElement(staticElement);
    fromNode.setStaticType(staticType);
    AssignmentExpression toNode = assignmentExpression(
        identifier("a"),
        TokenType.PLUS_EQ,
        identifier("b"));

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(propagatedElement, toNode.getPropagatedElement());
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticElement, toNode.getStaticElement());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitBinaryExpression() {
    BinaryExpression fromNode = binaryExpression(identifier("a"), TokenType.PLUS, identifier("b"));
    Type propagatedType = classElement("C").getType();
    MethodElement propagatedElement = methodElement("+", propagatedType);
    fromNode.setPropagatedElement(propagatedElement);
    fromNode.setPropagatedType(propagatedType);
    Type staticType = classElement("C").getType();
    MethodElement staticElement = methodElement("+", staticType);
    fromNode.setStaticElement(staticElement);
    fromNode.setStaticType(staticType);
    BinaryExpression toNode = binaryExpression(identifier("a"), TokenType.PLUS, identifier("b"));

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(propagatedElement, toNode.getPropagatedElement());
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticElement, toNode.getStaticElement());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitBooleanLiteral() {
    BooleanLiteral fromNode = booleanLiteral(true);
    Type propagatedType = classElement("C").getType();
    fromNode.setPropagatedType(propagatedType);
    Type staticType = classElement("C").getType();
    fromNode.setStaticType(staticType);
    BooleanLiteral toNode = booleanLiteral(true);

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitCascadeExpression() {
    CascadeExpression fromNode = cascadeExpression(identifier("a"), identifier("b"));
    Type propagatedType = classElement("C").getType();
    fromNode.setPropagatedType(propagatedType);
    Type staticType = classElement("C").getType();
    fromNode.setStaticType(staticType);
    CascadeExpression toNode = cascadeExpression(identifier("a"), identifier("b"));

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitCompilationUnit() {
    CompilationUnit fromNode = compilationUnit();
    CompilationUnitElement element = new CompilationUnitElementImpl("test.dart");
    fromNode.setElement(element);
    CompilationUnit toNode = compilationUnit();

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(element, toNode.getElement());
  }

  public void test_visitConditionalExpression() {
    ConditionalExpression fromNode = conditionalExpression(
        identifier("c"),
        identifier("a"),
        identifier("b"));
    Type propagatedType = classElement("C").getType();
    fromNode.setPropagatedType(propagatedType);
    Type staticType = classElement("C").getType();
    fromNode.setStaticType(staticType);
    ConditionalExpression toNode = conditionalExpression(
        identifier("c"),
        identifier("a"),
        identifier("b"));

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitConstructorDeclaration() {
    String className = "A";
    String constructorName = "c";
    ConstructorDeclaration fromNode = constructorDeclaration(
        identifier(className),
        constructorName,
        formalParameterList(),
        null);
    ConstructorElement element = constructorElement(classElement(className), constructorName);
    fromNode.setElement(element);
    ConstructorDeclaration toNode = constructorDeclaration(
        identifier(className),
        constructorName,
        formalParameterList(),
        null);

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(element, toNode.getElement());
  }

  public void test_visitConstructorName() {
    ConstructorName fromNode = constructorName(typeName("A"), "c");
    ConstructorElement staticElement = constructorElement(classElement("A"), "c");
    fromNode.setStaticElement(staticElement);
    ConstructorName toNode = constructorName(typeName("A"), "c");

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(staticElement, toNode.getStaticElement());
  }

  public void test_visitDoubleLiteral() {
    DoubleLiteral fromNode = doubleLiteral(1.0);
    Type propagatedType = classElement("C").getType();
    fromNode.setPropagatedType(propagatedType);
    Type staticType = classElement("C").getType();
    fromNode.setStaticType(staticType);
    DoubleLiteral toNode = doubleLiteral(1.0);

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitExportDirective() {
    ExportDirective fromNode = exportDirective("dart:uri");
    ExportElement element = new ExportElementImpl();
    fromNode.setElement(element);
    ExportDirective toNode = exportDirective("dart:uri");

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(element, toNode.getElement());
  }

  public void test_visitFunctionExpression() {
    FunctionExpression fromNode = functionExpression(formalParameterList(), emptyFunctionBody());
    MethodElement element = methodElement("m", classElement("C").getType());
    fromNode.setElement(element);
    Type propagatedType = classElement("C").getType();
    fromNode.setPropagatedType(propagatedType);
    Type staticType = classElement("C").getType();
    fromNode.setStaticType(staticType);
    FunctionExpression toNode = functionExpression(formalParameterList(), emptyFunctionBody());

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(element, toNode.getElement());
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitFunctionExpressionInvocation() {
    FunctionExpressionInvocation fromNode = functionExpressionInvocation(identifier("f"));
    MethodElement propagatedElement = methodElement("m", classElement("C").getType());
    fromNode.setPropagatedElement(propagatedElement);
    Type propagatedType = classElement("C").getType();
    fromNode.setPropagatedType(propagatedType);
    MethodElement staticElement = methodElement("m", classElement("C").getType());
    fromNode.setStaticElement(staticElement);
    Type staticType = classElement("C").getType();
    fromNode.setStaticType(staticType);
    FunctionExpressionInvocation toNode = functionExpressionInvocation(identifier("f"));

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(propagatedElement, toNode.getPropagatedElement());
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticElement, toNode.getStaticElement());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitImportDirective() {
    ImportDirective fromNode = importDirective("dart:uri", null);
    ImportElement element = new ImportElementImpl(0);
    fromNode.setElement(element);
    ImportDirective toNode = importDirective("dart:uri", null);

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(element, toNode.getElement());
  }

  public void test_visitIndexExpression() {
    IndexExpression fromNode = indexExpression(identifier("a"), integer(0L));
    MethodElement propagatedElement = methodElement("m", classElement("C").getType());
    MethodElement staticElement = methodElement("m", classElement("C").getType());
    AuxiliaryElements auxiliaryElements = new AuxiliaryElements(staticElement, propagatedElement);
    fromNode.setAuxiliaryElements(auxiliaryElements);
    fromNode.setPropagatedElement(propagatedElement);
    Type propagatedType = classElement("C").getType();
    fromNode.setPropagatedType(propagatedType);
    fromNode.setStaticElement(staticElement);
    Type staticType = classElement("C").getType();
    fromNode.setStaticType(staticType);
    IndexExpression toNode = indexExpression(identifier("a"), integer(0L));

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(auxiliaryElements, toNode.getAuxiliaryElements());
    assertSame(propagatedElement, toNode.getPropagatedElement());
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticElement, toNode.getStaticElement());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitInstanceCreationExpression() {
    InstanceCreationExpression fromNode = instanceCreationExpression(Keyword.NEW, typeName("C"));
    Type propagatedType = classElement("C").getType();
    fromNode.setPropagatedType(propagatedType);
    ConstructorElement staticElement = constructorElement(classElement("C"), null);
    fromNode.setStaticElement(staticElement);
    Type staticType = classElement("C").getType();
    fromNode.setStaticType(staticType);
    InstanceCreationExpression toNode = instanceCreationExpression(Keyword.NEW, typeName("C"));

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticElement, toNode.getStaticElement());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitIntegerLiteral() {
    IntegerLiteral fromNode = integer(2L);
    Type propagatedType = classElement("C").getType();
    fromNode.setPropagatedType(propagatedType);
    Type staticType = classElement("C").getType();
    fromNode.setStaticType(staticType);
    IntegerLiteral toNode = integer(2L);

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitIsExpression() {
    IsExpression fromNode = isExpression(identifier("x"), false, typeName("A"));
    Type propagatedType = classElement("C").getType();
    fromNode.setPropagatedType(propagatedType);
    Type staticType = classElement("C").getType();
    fromNode.setStaticType(staticType);
    IsExpression toNode = isExpression(identifier("x"), false, typeName("A"));

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitLibraryIdentifier() {
    LibraryIdentifier fromNode = libraryIdentifier(identifier("lib"));
    Type propagatedType = classElement("C").getType();
    fromNode.setPropagatedType(propagatedType);
    Type staticType = classElement("C").getType();
    fromNode.setStaticType(staticType);
    LibraryIdentifier toNode = libraryIdentifier(identifier("lib"));

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitListLiteral() {
    ListLiteral fromNode = listLiteral();
    Type propagatedType = classElement("C").getType();
    fromNode.setPropagatedType(propagatedType);
    Type staticType = classElement("C").getType();
    fromNode.setStaticType(staticType);
    ListLiteral toNode = listLiteral();

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitMapLiteral() {
    MapLiteral fromNode = mapLiteral();
    Type propagatedType = classElement("C").getType();
    fromNode.setPropagatedType(propagatedType);
    Type staticType = classElement("C").getType();
    fromNode.setStaticType(staticType);
    MapLiteral toNode = mapLiteral();

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitMethodInvocation() {
    MethodInvocation fromNode = methodInvocation("m");
    Type propagatedType = classElement("C").getType();
    fromNode.setPropagatedType(propagatedType);
    Type staticType = classElement("C").getType();
    fromNode.setStaticType(staticType);
    MethodInvocation toNode = methodInvocation("m");

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitNamedExpression() {
    NamedExpression fromNode = namedExpression("n", integer(0L));
    Type propagatedType = classElement("C").getType();
    fromNode.setPropagatedType(propagatedType);
    Type staticType = classElement("C").getType();
    fromNode.setStaticType(staticType);
    NamedExpression toNode = namedExpression("n", integer(0L));

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitNullLiteral() {
    NullLiteral fromNode = nullLiteral();
    Type propagatedType = classElement("C").getType();
    fromNode.setPropagatedType(propagatedType);
    Type staticType = classElement("C").getType();
    fromNode.setStaticType(staticType);
    NullLiteral toNode = nullLiteral();

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitParenthesizedExpression() {
    ParenthesizedExpression fromNode = parenthesizedExpression(integer(0L));
    Type propagatedType = classElement("C").getType();
    fromNode.setPropagatedType(propagatedType);
    Type staticType = classElement("C").getType();
    fromNode.setStaticType(staticType);
    ParenthesizedExpression toNode = parenthesizedExpression(integer(0L));

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitPartDirective() {
    PartDirective fromNode = partDirective("part.dart");
    LibraryElement element = new LibraryElementImpl(null, libraryIdentifier("lib"));
    fromNode.setElement(element);
    PartDirective toNode = partDirective("part.dart");

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(element, toNode.getElement());
  }

  public void test_visitPartOfDirective() {
    PartOfDirective fromNode = partOfDirective(libraryIdentifier("lib"));
    LibraryElement element = new LibraryElementImpl(null, libraryIdentifier("lib"));
    fromNode.setElement(element);
    PartOfDirective toNode = partOfDirective(libraryIdentifier("lib"));

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(element, toNode.getElement());
  }

  public void test_visitPostfixExpression() {
    String variableName = "x";
    PostfixExpression fromNode = postfixExpression(identifier(variableName), TokenType.PLUS_PLUS);
    MethodElement propagatedElement = methodElement("+", classElement("C").getType());
    fromNode.setPropagatedElement(propagatedElement);
    Type propagatedType = classElement("C").getType();
    fromNode.setPropagatedType(propagatedType);
    MethodElement staticElement = methodElement("+", classElement("C").getType());
    fromNode.setStaticElement(staticElement);
    Type staticType = classElement("C").getType();
    fromNode.setStaticType(staticType);
    PostfixExpression toNode = postfixExpression(identifier(variableName), TokenType.PLUS_PLUS);

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(propagatedElement, toNode.getPropagatedElement());
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticElement, toNode.getStaticElement());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitPrefixedIdentifier() {
    PrefixedIdentifier fromNode = identifier("p", "f");
    Type propagatedType = classElement("C").getType();
    fromNode.setPropagatedType(propagatedType);
    Type staticType = classElement("C").getType();
    fromNode.setStaticType(staticType);
    PrefixedIdentifier toNode = identifier("p", "f");

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitPrefixExpression() {
    PrefixExpression fromNode = prefixExpression(TokenType.PLUS_PLUS, identifier("x"));
    MethodElement propagatedElement = methodElement("+", classElement("C").getType());
    Type propagatedType = classElement("C").getType();
    fromNode.setPropagatedElement(propagatedElement);
    fromNode.setPropagatedType(propagatedType);
    Type staticType = classElement("C").getType();
    MethodElement staticElement = methodElement("+", classElement("C").getType());
    fromNode.setStaticElement(staticElement);
    fromNode.setStaticType(staticType);
    PrefixExpression toNode = prefixExpression(TokenType.PLUS_PLUS, identifier("x"));

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(propagatedElement, toNode.getPropagatedElement());
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticElement, toNode.getStaticElement());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitPropertyAccess() {
    PropertyAccess fromNode = propertyAccess(identifier("x"), "y");
    Type propagatedType = classElement("C").getType();
    fromNode.setPropagatedType(propagatedType);
    Type staticType = classElement("C").getType();
    fromNode.setStaticType(staticType);
    PropertyAccess toNode = propertyAccess(identifier("x"), "y");

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitRedirectingConstructorInvocation() {
    RedirectingConstructorInvocation fromNode = redirectingConstructorInvocation();
    ConstructorElement staticElement = constructorElement(classElement("C"), null);
    fromNode.setStaticElement(staticElement);
    RedirectingConstructorInvocation toNode = redirectingConstructorInvocation();

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(staticElement, toNode.getStaticElement());
  }

  public void test_visitRethrowExpression() {
    RethrowExpression fromNode = rethrowExpression();
    Type propagatedType = classElement("C").getType();
    fromNode.setPropagatedType(propagatedType);
    Type staticType = classElement("C").getType();
    fromNode.setStaticType(staticType);
    RethrowExpression toNode = rethrowExpression();

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitSimpleIdentifier() {
    SimpleIdentifier fromNode = identifier("x");
    MethodElement propagatedElement = methodElement("m", classElement("C").getType());
    MethodElement staticElement = methodElement("m", classElement("C").getType());
    AuxiliaryElements auxiliaryElements = new AuxiliaryElements(staticElement, propagatedElement);
    fromNode.setAuxiliaryElements(auxiliaryElements);
    fromNode.setPropagatedElement(propagatedElement);
    Type propagatedType = classElement("C").getType();
    fromNode.setPropagatedType(propagatedType);
    fromNode.setStaticElement(staticElement);
    Type staticType = classElement("C").getType();
    fromNode.setStaticType(staticType);
    SimpleIdentifier toNode = identifier("x");

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(auxiliaryElements, toNode.getAuxiliaryElements());
    assertSame(propagatedElement, toNode.getPropagatedElement());
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticElement, toNode.getStaticElement());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitSimpleStringLiteral() {
    SimpleStringLiteral fromNode = string("abc");
    Type propagatedType = classElement("C").getType();
    fromNode.setPropagatedType(propagatedType);
    Type staticType = classElement("C").getType();
    fromNode.setStaticType(staticType);
    SimpleStringLiteral toNode = string("abc");

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitStringInterpolation() {
    StringInterpolation fromNode = string(interpolationString("a", "'a'"));
    Type propagatedType = classElement("C").getType();
    fromNode.setPropagatedType(propagatedType);
    Type staticType = classElement("C").getType();
    fromNode.setStaticType(staticType);
    StringInterpolation toNode = string(interpolationString("a", "'a'"));

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitSuperConstructorInvocation() {
    SuperConstructorInvocation fromNode = superConstructorInvocation();
    ConstructorElement staticElement = constructorElement(classElement("C"), null);
    fromNode.setStaticElement(staticElement);
    SuperConstructorInvocation toNode = superConstructorInvocation();

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(staticElement, toNode.getStaticElement());
  }

  public void test_visitSuperExpression() {
    SuperExpression fromNode = superExpression();
    Type propagatedType = classElement("C").getType();
    fromNode.setPropagatedType(propagatedType);
    Type staticType = classElement("C").getType();
    fromNode.setStaticType(staticType);
    SuperExpression toNode = superExpression();

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitSymbolLiteral() {
    SymbolLiteral fromNode = symbolLiteral("s");
    Type propagatedType = classElement("C").getType();
    fromNode.setPropagatedType(propagatedType);
    Type staticType = classElement("C").getType();
    fromNode.setStaticType(staticType);
    SymbolLiteral toNode = symbolLiteral("s");

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitThisExpression() {
    ThisExpression fromNode = thisExpression();
    Type propagatedType = classElement("C").getType();
    fromNode.setPropagatedType(propagatedType);
    Type staticType = classElement("C").getType();
    fromNode.setStaticType(staticType);
    ThisExpression toNode = thisExpression();

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitThrowExpression() {
    ThrowExpression fromNode = throwExpression();
    Type propagatedType = classElement("C").getType();
    fromNode.setPropagatedType(propagatedType);
    Type staticType = classElement("C").getType();
    fromNode.setStaticType(staticType);
    ThrowExpression toNode = throwExpression();

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(propagatedType, toNode.getPropagatedType());
    assertSame(staticType, toNode.getStaticType());
  }

  public void test_visitTypeName() {
    TypeName fromNode = typeName("C");
    Type type = classElement("C").getType();
    fromNode.setType(type);
    TypeName toNode = typeName("C");

    ResolutionCopier.copyResolutionData(fromNode, toNode);
    assertSame(type, toNode.getType());
  }
}
