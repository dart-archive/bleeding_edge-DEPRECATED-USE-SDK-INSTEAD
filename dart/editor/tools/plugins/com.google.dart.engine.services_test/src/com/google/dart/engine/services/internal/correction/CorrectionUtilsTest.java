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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.dart.engine.ast.AsExpression;
import com.google.dart.engine.ast.AstFactory;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.BlockFunctionBody;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionStatement;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.IfStatement;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.IntegerLiteral;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationStatement;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.TypeParameterElement;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.context.TimestampedData;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.services.change.Edit;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.internal.correction.CorrectionUtils.InsertDesc;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.engine.utilities.source.SourceRangeFactory;

import static com.google.dart.engine.ast.AstFactory.binaryExpression;
import static com.google.dart.engine.ast.AstFactory.identifier;
import static com.google.dart.engine.ast.AstFactory.label;
import static com.google.dart.engine.ast.AstFactory.namedExpression;
import static com.google.dart.engine.ast.AstFactory.postfixExpression;
import static com.google.dart.engine.ast.AstFactory.prefixExpression;
import static com.google.dart.engine.ast.AstFactory.propertyAccess;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartEnd;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartLength;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.nio.CharBuffer;
import java.util.List;
import java.util.Set;

public class CorrectionUtilsTest extends AbstractDartTest {

  private static void assert_getVariableNameSuggestions(Type expectedType, Expression expression,
      Set<String> nameSuggestExclude, String[] expected) {
    String[] suggestions = CorrectionUtils.getVariableNameSuggestions(
        expectedType,
        expression,
        nameSuggestExclude);
    assertThat(suggestions).isEqualTo(expected);
  }

  public void test_addEdit_badRange() throws Exception {
    parseTestUnit("// 12345");
    Edit edit = new Edit(400, 3, "abc");
    // mock SourceChange
    SourceChange change = mock(SourceChange.class);
    when(change.getSource()).thenReturn(testSource);
    // add Edit
    String description = "desc";
    try {
      CorrectionUtils.addEdit(getAnalysisContext(), change, description, "234", edit);
      fail();
    } catch (IllegalStateException e) {
    }
    // verify
    verify(change, never()).addEdit(description, edit);
  }

  public void test_addEdit_badRangeContent() throws Exception {
    parseTestUnit("// 12345");
    Edit edit = new Edit(4, 3, "abc");
    // mock SourceChange
    SourceChange change = mock(SourceChange.class);
    when(change.getSource()).thenReturn(testSource);
    // add Edit
    String description = "desc";
    try {
      CorrectionUtils.addEdit(getAnalysisContext(), change, description, "err", edit);
      fail();
    } catch (IllegalStateException e) {
    }
    // verify
    verify(change, never()).addEdit(description, edit);
  }

  public void test_addEdit_OK() throws Exception {
    parseTestUnit("// 12345");
    Edit edit = new Edit(4, 3, "abc");
    // mock SourceChange
    SourceChange change = mock(SourceChange.class);
    when(change.getSource()).thenReturn(testSource);
    // add Edit
    String description = "desc";
    CorrectionUtils.addEdit(getAnalysisContext(), change, description, "234", edit);
    // verify
    verify(change).addEdit(description, edit);
  }

  public void test_allListsEqual_0() throws Exception {
    List<List<Integer>> lists = ImmutableList.<List<Integer>> of(
        Lists.newArrayList(0, 1, 2),
        Lists.newArrayList(0, -1, -2));
    assertTrue(CorrectionUtils.allListsEqual(lists, 0));
    assertFalse(CorrectionUtils.allListsEqual(lists, 1));
  }

  public void test_allListsEqual_1() throws Exception {
    List<List<Integer>> lists = ImmutableList.<List<Integer>> of(
        Lists.newArrayList(-1, 0, 1, 2),
        Lists.newArrayList(1, 0, -1, -2),
        Lists.newArrayList(-10, 0, 10, 20));
    assertFalse(CorrectionUtils.allListsEqual(lists, 0));
    assertTrue(CorrectionUtils.allListsEqual(lists, 1));
  }

  public void test_applyReplaceEdits() throws Exception {
    String s = "0123456789";
    String result = CorrectionUtils.applyReplaceEdits(
        s,
        ImmutableList.of(new Edit(7, 1, "A"), new Edit(1, 2, "BBB"), new Edit(4, 1, "C")));
    assertEquals("0BBB3C56A89", result);
  }

  /**
   * Test for {@link CorrectionUtils#covers(SourceRange, DartNode)}.
   */
  public void test_covers_SourceRange_DartNode() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "}",
        "");
    assertFalse(CorrectionUtils.covers(rangeStartLength(0, 1), testUnit));
    assertTrue(CorrectionUtils.covers(rangeStartLength(0, 1000), testUnit));
  }

  public void test_createIndentEdit() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (true) {",
        "    print(0);",
        "  }",
        "} // marker");
    CorrectionUtils utils = getTestCorrectionUtils();
    SourceRange range = rangeStartEnd(findOffset("  if (true"), findOffset("} // marker"));
    Edit edit = utils.createIndentEdit(range, "  ", "");
    assertEquals(
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "main() {",
            "if (true) {",
            "  print(0);",
            "}",
            "} // marker"),
        CorrectionUtils.applyReplaceEdits(testCode, ImmutableList.of(edit)));
  }

  public void test_findNode() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  String v = null;",
        "}",
        "");
    CorrectionUtils utils = getTestCorrectionUtils();
    // no any node
    {
      AstNode node = utils.findNode(Integer.MAX_VALUE, AstNode.class);
      assertNull(node);
    }
    // "String" as SimpleIdentifier
    {
      SimpleIdentifier node = utils.findNode(findOffset("String "), SimpleIdentifier.class);
      assertNotNull(node);
    }
    // "String" as TypeName
    {
      TypeName node = utils.findNode(findOffset("String "), TypeName.class);
      assertNotNull(node);
    }
    // "String" as part of FunctionDeclaration
    {
      FunctionDeclaration node = utils.findNode(findOffset("String "), FunctionDeclaration.class);
      assertNotNull(node);
    }
  }

  public void test_getChildren() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var myField;",
        "  myMethod() {}",
        "}",
        "var topField;",
        "topFunction() {}",
        "");
    CompilationUnitElement unitElement = testUnit.getElement();
    ClassElement classElement = ((ClassDeclaration) testUnit.getDeclarations().get(0)).getElement();
    ExecutableElement topFunctionElement = ((FunctionDeclaration) testUnit.getDeclarations().get(2)).getElement();
    // not found
    assertThat(CorrectionUtils.getChildren(unitElement, "noSuchElement")).isEmpty();
    // find "A"
    assertThat(CorrectionUtils.getChildren(unitElement, "A")).containsOnly(classElement);
    // find "topFunction"
    assertThat(CorrectionUtils.getChildren(unitElement, "topFunction")).containsOnly(
        topFunctionElement);
    // find all "A" members
    {
      List<Element> children = CorrectionUtils.getChildren(classElement);
      // 1(field) + 2(getter + setter) + 1(default constructor) + 1(method)
      assertThat(children).hasSize(5);
    }
    // find "A.myMethod"
    {
      List<Element> children = CorrectionUtils.getChildren(classElement, "myMethod");
      assertThat(children).hasSize(1);
      MethodElement child = (MethodElement) Iterables.get(children, 0);
      assertEquals("myMethod", child.getDisplayName());
    }
  }

  public void test_getCommentRanges() throws Exception {
    parseTestUnit(
        "main() {",
        "  print(1);",
        "  /* marker-1",
        "    print(2);",
        "  marker-2 */",
        "  print(3);",
        "  /* marker-3",
        "    print(4);",
        "  marker-4 */",
        "}",
        "");
    CorrectionUtils utils = getTestCorrectionUtils();
    List<SourceRange> commentRanges = utils.getCommentRanges();
    assertThat(commentRanges).hasSize(2);
    assertEquals(
        SourceRangeFactory.rangeStartEnd(findOffset("/* marker-1"), findEnd("marker-2 */")),
        commentRanges.get(0));
    assertEquals(
        SourceRangeFactory.rangeStartEnd(findOffset("/* marker-3"), findEnd("marker-4 */")),
        commentRanges.get(1));
  }

  public void test_getDeltaOffset() throws Exception {
    assertEquals(1, CorrectionUtils.getDeltaOffset(new Edit(0, 5, "123456")));
    assertEquals(-2, CorrectionUtils.getDeltaOffset(new Edit(0, 5, "123")));
  }

  public void test_getElementKindName_ClassElement() throws Exception {
    ClassElement element = mock(ClassElement.class);
    when(element.getKind()).thenReturn(ElementKind.CLASS);
    assertEquals("class", CorrectionUtils.getElementKindName(element));
  }

  public void test_getElementKindName_CompilationUnitElement() throws Exception {
    CompilationUnitElement element = mock(CompilationUnitElement.class);
    when(element.getKind()).thenReturn(ElementKind.COMPILATION_UNIT);
    assertEquals("compilation unit", CorrectionUtils.getElementKindName(element));
  }

  public void test_getElementKindName_ConstructorElement() throws Exception {
    ConstructorElement element = mock(ConstructorElement.class);
    when(element.getKind()).thenReturn(ElementKind.CONSTRUCTOR);
    assertEquals("constructor", CorrectionUtils.getElementKindName(element));
  }

  public void test_getElementKindName_FieldElement() throws Exception {
    FieldElement element = mock(FieldElement.class);
    when(element.getKind()).thenReturn(ElementKind.FIELD);
    assertEquals("field", CorrectionUtils.getElementKindName(element));
  }

  public void test_getElementKindName_FunctionElement() throws Exception {
    FunctionElement element = mock(FunctionElement.class);
    when(element.getKind()).thenReturn(ElementKind.FUNCTION);
    assertEquals("function", CorrectionUtils.getElementKindName(element));
  }

  public void test_getElementKindName_LocalVariableElement() throws Exception {
    LocalVariableElement element = mock(LocalVariableElement.class);
    when(element.getKind()).thenReturn(ElementKind.LOCAL_VARIABLE);
    assertEquals("local variable", CorrectionUtils.getElementKindName(element));
  }

  public void test_getElementKindName_MethodElement() throws Exception {
    MethodElement element = mock(MethodElement.class);
    when(element.getKind()).thenReturn(ElementKind.METHOD);
    assertEquals("method", CorrectionUtils.getElementKindName(element));
  }

  public void test_getElementKindName_TypeAliasElement() throws Exception {
    FunctionTypeAliasElement element = mock(FunctionTypeAliasElement.class);
    when(element.getKind()).thenReturn(ElementKind.FUNCTION_TYPE_ALIAS);
    assertEquals("function type alias", CorrectionUtils.getElementKindName(element));
  }

  public void test_getElementKindName_TypeParameterElement() throws Exception {
    TypeParameterElement element = mock(TypeParameterElement.class);
    when(element.getKind()).thenReturn(ElementKind.TYPE_PARAMETER);
    assertEquals("type parameter", CorrectionUtils.getElementKindName(element));
  }

  public void test_getElementQualifiedName() throws Exception {
    ClassElement enclosingClass = mock(ClassElement.class);
    when(enclosingClass.getKind()).thenReturn(ElementKind.CLASS);
    when(enclosingClass.getDisplayName()).thenReturn("A");
    // ClassElement
    assertEquals("A", CorrectionUtils.getElementQualifiedName(enclosingClass));
    // MethodElement
    {
      MethodElement method = mock(MethodElement.class);
      when(method.getKind()).thenReturn(ElementKind.METHOD);
      when(method.getEnclosingElement()).thenReturn(enclosingClass);
      when(method.getDisplayName()).thenReturn("myMethod");
      assertEquals("A.myMethod", CorrectionUtils.getElementQualifiedName(method));
    }
    // FieldElement
    {
      FieldElement field = mock(FieldElement.class);
      when(field.getKind()).thenReturn(ElementKind.FIELD);
      when(field.getEnclosingElement()).thenReturn(enclosingClass);
      when(field.getDisplayName()).thenReturn("myField");
      assertEquals("A.myField", CorrectionUtils.getElementQualifiedName(field));
    }
  }

  public void test_getEnclosingExecutableElement_constructor() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  A() {",
        "    print(0);",
        "  }",
        "}",
        "");
    ConstructorDeclaration constructorNode = findNode("A()", ConstructorDeclaration.class);
    ConstructorElement constructorElement = constructorNode.getElement();
    SimpleIdentifier node = findIdentifier("print(0)");
    assertSame(constructorNode, CorrectionUtils.getEnclosingExecutableNode(node));
    assertSame(constructorElement, CorrectionUtils.getEnclosingExecutableElement(node));
  }

  public void test_getEnclosingExecutableElement_function() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "fff(p) {",
        "  var v;",
        "  zzz(p) {}",
        "  print(0);",
        "}",
        "");
    FunctionDeclaration functionNode = findNode("fff(p)", FunctionDeclaration.class);
    ExecutableElement functionElement = functionNode.getElement();
    SimpleIdentifier node = findIdentifier("print(0)");
    assertSame(functionNode, CorrectionUtils.getEnclosingExecutableNode(node));
    assertSame(functionElement, CorrectionUtils.getEnclosingExecutableElement(node));
  }

  public void test_getEnclosingExecutableElement_method() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  method() {",
        "    print(0);",
        "  }",
        "}",
        "");
    MethodDeclaration methodNode = findNode("method()", MethodDeclaration.class);
    MethodElement methodElement = (MethodElement) methodNode.getElement();
    SimpleIdentifier node = findIdentifier("print(0)");
    assertSame(methodNode, CorrectionUtils.getEnclosingExecutableNode(node));
    assertSame(methodElement, CorrectionUtils.getEnclosingExecutableElement(node));
  }

  public void test_getEnclosingExecutableElement_null() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var v = 42;",
        "");
    SimpleIdentifier node = findIdentifier("v = 42");
    assertSame(null, CorrectionUtils.getEnclosingExecutableNode(node));
    assertSame(null, CorrectionUtils.getEnclosingExecutableElement(node));
  }

  /**
   * Test for {@link CorrectionUtils#getEndOfLine()}.
   */
  public void test_getEndOfLine_unix() throws Exception {
    parseTestUnit("// aaa\n// bbb\n// ccc");
    CorrectionUtils utils = getTestCorrectionUtils();
    assertEquals("\n", utils.getEndOfLine());
  }

  /**
   * Test for {@link CorrectionUtils#getEndOfLine()}.
   */
  public void test_getEndOfLine_windows() throws Exception {
    parseTestUnit("// aaa\r\n// bbb\r\n// ccc");
    CorrectionUtils utils = getTestCorrectionUtils();
    assertEquals("\r\n", utils.getEndOfLine());
  }

  public void test_getExportedElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var myVariable;",
        "myFunction() {}",
        "class MyClass {}",
        "");
    // OK
    assertNotNull(CorrectionUtils.getExportedElement(testLibraryElement, "myVariable"));
    assertNotNull(CorrectionUtils.getExportedElement(testLibraryElement, "myFunction"));
    assertNotNull(CorrectionUtils.getExportedElement(testLibraryElement, "MyClass"));
    // no LibraryElement
    assertNull(CorrectionUtils.getExportedElement(null, "MyClass"));
    // no such name
    assertNull(CorrectionUtils.getExportedElement(testLibraryElement, "OtherName"));
  }

  public void test_getIndent() throws Exception {
    parseTestUnit();
    CorrectionUtils utils = getTestCorrectionUtils();
    assertEquals("", utils.getIndent(0));
    assertEquals("  ", utils.getIndent(1));
    assertEquals("    ", utils.getIndent(2));
  }

  public void test_getIndentSource_direction_left() throws Exception {
    parseTestUnit("");
    CorrectionUtils utils = getTestCorrectionUtils();
    assertEquals("a\nb\nc\n", utils.getIndentSource("  a\n  b\n  c\n", false));
    assertEquals("  a\n    b\n  c\n", utils.getIndentSource("    a\n      b\n    c\n", false));
  }

  public void test_getIndentSource_direction_right() throws Exception {
    parseTestUnit("");
    CorrectionUtils utils = getTestCorrectionUtils();
    assertEquals("  a\n  b\n  c\n", utils.getIndentSource("a\nb\nc\n", true));
    assertEquals("    a\n      b\n    c\n", utils.getIndentSource("  a\n    b\n  c\n", true));
  }

  public void test_getIndentSource_SourceRange() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (true) {",
        "    print(0);",
        "  }",
        "} // marker");
    CorrectionUtils utils = getTestCorrectionUtils();
    SourceRange range = rangeStartEnd(findOffset("  if (true"), findOffset("} // marker"));
    String result = utils.getIndentSource(range, "  ", "");
    assertEquals("if (true) {\n  print(0);\n}\n", toUnixEol(result));
  }

  public void test_getIndentSource_String() throws Exception {
    parseTestUnit("");
    CorrectionUtils utils = getTestCorrectionUtils();
    assertEquals(makeSource(//
        "{",
        "  var v;",
        "}",
        ""), utils.getIndentSource(makeSource(//
        "  {",
        "    var v;",
        "  }",
        ""), "  ", ""));
    assertEquals(makeSource(//
        "  {",
        "    var v;",
        "  }",
        ""), utils.getIndentSource(makeSource(//
        "{",
        "  var v;",
        "}",
        ""), "", "  "));
    assertEquals(makeSource(//
        "  {",
        "  ",
        "    var v;",
        "  }",
        ""), utils.getIndentSource(makeSource(//
        "{",
        "",
        "  var v;",
        "}",
        ""), "", "  "));
    assertEquals(makeSource(//
        "    {",
        "      var v;",
        "    }",
        ""), utils.getIndentSource(makeSource(//
        "  {",
        "    var v;",
        "  }",
        ""), "  ", "    "));
    // don't change multiline strings
    assertEquals(makeSource(//
        "  {",
        "    var v = '''",
        "first line",
        "second line",
        "  ''';",
        "    var v2 = 5;",
        "  }",
        ""), utils.getIndentSource(makeSource(//
        "{",
        "  var v = '''",
        "first line",
        "second line",
        "  ''';",
        "  var v2 = 5;",
        "}",
        ""), "", "  "));
    // when the line starts not _within_ of a string, we still can indent it
    assertEquals(makeSource(//
        "  {",
        "  '''aaa",
        "bbb'''.length;",
        "    var v2 = 5;",
        "  }",
        ""), utils.getIndentSource(makeSource(//
        "{",
        "'''aaa",
        "bbb'''.length;",
        "  var v2 = 5;",
        "}",
        ""), "", "  "));
  }

  public void test_getInsertDescImport_emptyUnit() throws Exception {
    parseTestUnit();
    CorrectionUtils utils = getTestCorrectionUtils();
    InsertDesc desc = utils.getInsertDescImport();
    assertEquals(0, desc.offset);
    assertEquals("", toUnixEol(desc.prefix));
    assertEquals("", toUnixEol(desc.suffix));
  }

  public void test_getInsertDescImport_hasExport() throws Exception {
    parseTestUnit("export 'dart:math';");
    CorrectionUtils utils = getTestCorrectionUtils();
    InsertDesc desc = utils.getInsertDescImport();
    assertEquals(findEnd("export 'dart:math';"), desc.offset);
    assertEquals("\n", toUnixEol(desc.prefix));
    assertEquals("", toUnixEol(desc.suffix));
  }

  public void test_getInsertDescImport_hasImport() throws Exception {
    parseTestUnit("import 'dart:math';");
    CorrectionUtils utils = getTestCorrectionUtils();
    InsertDesc desc = utils.getInsertDescImport();
    assertEquals(findEnd("import 'dart:math';"), desc.offset);
    assertEquals("\n", toUnixEol(desc.prefix));
    assertEquals("", toUnixEol(desc.suffix));
  }

  public void test_getInsertDescImport_hasLibrary() throws Exception {
    parseTestUnit("library app;");
    CorrectionUtils utils = getTestCorrectionUtils();
    InsertDesc desc = utils.getInsertDescImport();
    assertEquals(findEnd("library app;"), desc.offset);
    assertEquals("\n\n", toUnixEol(desc.prefix));
    assertEquals("", toUnixEol(desc.suffix));
  }

  public void test_getInsertDescPart_emptyUnit() throws Exception {
    parseTestUnit();
    CorrectionUtils utils = getTestCorrectionUtils();
    InsertDesc desc = utils.getInsertDescPart();
    assertEquals(0, desc.offset);
    assertEquals("", toUnixEol(desc.prefix));
    assertEquals("", toUnixEol(desc.suffix));
  }

  public void test_getInsertDescPart_hasLibrary() throws Exception {
    parseTestUnit("library app;");
    CorrectionUtils utils = getTestCorrectionUtils();
    InsertDesc desc = utils.getInsertDescPart();
    assertEquals(findEnd("library app;"), desc.offset);
    assertEquals("\n\n", toUnixEol(desc.prefix));
    assertEquals("", toUnixEol(desc.suffix));
  }

  public void test_getInsertDescTop_emptyUnit() throws Exception {
    parseTestUnit();
    CorrectionUtils utils = getTestCorrectionUtils();
    InsertDesc desc = utils.getInsertDescTop();
    assertEquals(0, desc.offset);
    assertEquals("", toUnixEol(desc.prefix));
    assertEquals("", toUnixEol(desc.suffix));
  }

  public void test_getInsertDescTop_hashBang() throws Exception {
    parseTestUnit(
        "#!/bin/dart",
        "",
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {}");
    CorrectionUtils utils = getTestCorrectionUtils();
    InsertDesc desc = utils.getInsertDescTop();
    assertEquals(findOffset("main()"), desc.offset);
    assertEquals("\n", toUnixEol(desc.prefix));
    assertEquals("\n", toUnixEol(desc.suffix));
  }

  public void test_getInsertDescTop_hasUnitMembers() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {}");
    CorrectionUtils utils = getTestCorrectionUtils();
    InsertDesc desc = utils.getInsertDescTop();
    assertEquals(findOffset("main()"), desc.offset);
    assertEquals("\n", toUnixEol(desc.prefix));
    assertEquals("\n", toUnixEol(desc.suffix));
  }

  public void test_getInsertDescTop_leadingComments_noUnitMembers() throws Exception {
    parseTestUnit("// filler filler filler filler filler filler filler filler filler filler");
    CorrectionUtils utils = getTestCorrectionUtils();
    InsertDesc desc = utils.getInsertDescTop();
    assertEquals(testCode.length(), desc.offset);
    assertEquals("\n", toUnixEol(desc.prefix));
    assertEquals("", toUnixEol(desc.suffix));
  }

  public void test_getLineContentEnd() throws Exception {
    parseTestUnit(Joiner.on("\n").join(
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler",
            "// 1 \t ",
            "// 2\r",
            "// 3",
            "",
            "// 4")));
    CorrectionUtils utils = getTestCorrectionUtils();
    assertEquals(findOffset("// 2"), utils.getLineContentEnd(findEnd("// 1")));
    assertEquals(findOffset("// 3"), utils.getLineContentEnd(findEnd("// 2")));
    assertEquals(findOffset("\n// 4"), utils.getLineContentEnd(findEnd("// 3")));
  }

  public void test_getLineContentStart() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "// 1",
        " \t // 2",
        "// 3");
    CorrectionUtils utils = getTestCorrectionUtils();
    assertEquals(findOffset("// 1"), utils.getLineContentStart(findOffset("// 1")));
    assertEquals(findOffset(" \t // 2"), utils.getLineContentStart(findOffset("// 2")));
  }

  public void test_getLineNext() throws Exception {
    parseTestUnit(Joiner.on("\n").join(
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler",
            "// 1",
            "// 2\r",
            "// 3")));
    CorrectionUtils utils = getTestCorrectionUtils();
    assertEquals(findOffset("// 1"), utils.getLineNext(findOffset(" filler")));
    assertEquals(findOffset("// 2"), utils.getLineNext(findOffset("/ 1")));
    assertEquals(findOffset("// 3"), utils.getLineNext(findOffset("/ 2")));
  }

  /**
   * Test for {@link CorrectionUtils#getLinePrefix(int)}.
   */
  public void test_getLinePrefix() throws Exception {
    parseTestUnit("//000\n  //111\n   \n  ");
    CorrectionUtils utils = getTestCorrectionUtils();
    // 0
    assertEquals("", utils.getLinePrefix(0));
    assertEquals("", utils.getLinePrefix(1));
    assertEquals("", utils.getLinePrefix(2));
    // 1
    assertEquals("  ", utils.getLinePrefix(6));
    assertEquals("  ", utils.getLinePrefix(7));
    assertEquals("  ", utils.getLinePrefix(8));
    assertEquals("  ", utils.getLinePrefix(9));
    assertEquals("  ", utils.getLinePrefix(10));
    // 2
    assertEquals("   ", utils.getLinePrefix(14));
    assertEquals("   ", utils.getLinePrefix(15));
    assertEquals("   ", utils.getLinePrefix(16));
    // 3
    assertEquals("  ", utils.getLinePrefix(19));
  }

  public void test_getLinesPrefix() throws Exception {
    assertEquals("", CorrectionUtils.getLinesPrefix(""));
    assertEquals("", CorrectionUtils.getLinesPrefix("noPrefix"));
    assertEquals("  ", CorrectionUtils.getLinesPrefix("  space2"));
    assertEquals(" \t ", CorrectionUtils.getLinesPrefix(" \t space-tab-space"));
    assertEquals("  ", CorrectionUtils.getLinesPrefix("  space2-NS-space "));
  }

  public void test_getLinesRange_SourceRange() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "// 1",
        "  \t // 2 \t  ",
        "// 3");
    CorrectionUtils utils = getTestCorrectionUtils();
    // use lines start/end already
    {
      int rs = findOffset("// 1");
      int re = findOffset("// 2");
      assertEquals(rangeStartEnd(rs, re), utils.getLinesRange(rangeStartEnd(rs, re)));
    }
    // use start/end with offsets from lines
    {
      int as = findOffset("// 2");
      int ae = findEnd("// 2");
      int rs = findOffset("  \t // 2");
      int re = findOffset("// 3");
      assertEquals(rangeStartEnd(rs, re), utils.getLinesRange(rangeStartEnd(as, ae)));
    }
  }

  public void test_getLinesRange_Statements() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() { // marker",
        "  var a;",
        "  var b;",
        "  var c;",
        "}",
        "");
    CorrectionUtils utils = getTestCorrectionUtils();
    Block block = findNode("{ // marker", Block.class);
    Statement statementA = findNode("var a", Statement.class);
    Statement statementB = findNode("var b", Statement.class);
    Statement statementC = findNode("var c", Statement.class);
    {
      SourceRange expected = rangeStartEnd(statementA.getOffset() - 2, statementC.getOffset() - 2);
      SourceRange range = utils.getLinesRange(statementA, statementB);
      assertEquals(expected, range);
    }
    {
      SourceRange expected = rangeStartEnd(
          statementC.getOffset() - 2,
          block.getRightBracket().getOffset());
      SourceRange range = utils.getLinesRange(statementC);
      assertEquals(expected, range);
    }
  }

  /**
   * Test for {@link CorrectionUtils#getLineThis(int)}.
   */
  public void test_getLineThis() throws Exception {
    parseTestUnit("//aaa\r\n//bbbb\r\n//ccccc");
    CorrectionUtils utils = getTestCorrectionUtils();
    // 0
    assertEquals(0, utils.getLineThis(0));
    assertEquals(0, utils.getLineThis(1));
    assertEquals(0, utils.getLineThis(2));
    // 5
    assertEquals(7, utils.getLineThis(7));
    assertEquals(7, utils.getLineThis(8));
    assertEquals(7, utils.getLineThis(9));
    assertEquals(7, utils.getLineThis(10));
    // 11
    assertEquals(15, utils.getLineThis(15));
    assertEquals(15, utils.getLineThis(16));
    assertEquals(15, utils.getLineThis(17));
    assertEquals(15, utils.getLineThis(18));
    assertEquals(15, utils.getLineThis(19));
  }

  public void test_getLocalOrParameterVariableElement_local() throws Exception {
    LocalVariableElement element = mock(LocalVariableElement.class);
    SimpleIdentifier identifier = AstFactory.identifier("name");
    identifier.setStaticElement(element);
    // check
    assertSame(element, CorrectionUtils.getLocalOrParameterVariableElement(identifier));
  }

  public void test_getLocalOrParameterVariableElement_method() throws Exception {
    Element element = mock(MethodElement.class);
    SimpleIdentifier identifier = AstFactory.identifier("name");
    identifier.setStaticElement(element);
    // check
    assertSame(null, CorrectionUtils.getLocalOrParameterVariableElement(identifier));
  }

  public void test_getLocalOrParameterVariableElement_parameter() throws Exception {
    ParameterElement element = mock(ParameterElement.class);
    SimpleIdentifier identifier = AstFactory.identifier("name");
    identifier.setStaticElement(element);
    // check
    assertSame(element, CorrectionUtils.getLocalOrParameterVariableElement(identifier));
  }

  public void test_getLocalVariableElement_local() throws Exception {
    LocalVariableElement element = mock(LocalVariableElement.class);
    SimpleIdentifier identifier = AstFactory.identifier("name");
    identifier.setStaticElement(element);
    // check
    assertSame(element, CorrectionUtils.getLocalVariableElement(identifier));
  }

  public void test_getLocalVariableElement_parameter() throws Exception {
    ParameterElement element = mock(ParameterElement.class);
    SimpleIdentifier identifier = AstFactory.identifier("name");
    identifier.setStaticElement(element);
    // check
    assertSame(null, CorrectionUtils.getLocalVariableElement(identifier));
  }

  public void test_getNearestCommonAncestor_innerBlock() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(1);",
        "  {",
        "    print(2);",
        "  }",
        "}",
        "");
    SimpleIdentifier node1 = findIdentifier("print(2)");
    SimpleIdentifier node2 = findIdentifier("print(1)");
    List<AstNode> nodes = ImmutableList.<AstNode> of(node1, node2);
    AstNode result = CorrectionUtils.getNearestCommonAncestor(nodes);
    assertEquals("{print(1); {print(2);}}", result.toSource());
  }

  public void test_getNearestCommonAncestor_noNodes() throws Exception {
    ImmutableList<AstNode> nodes = ImmutableList.<AstNode> of();
    AstNode result = CorrectionUtils.getNearestCommonAncestor(nodes);
    assertNull(result);
  }

  public void test_getNearestCommonAncestor_sameBlock() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(1);",
        "  print(2);",
        "}",
        "");
    SimpleIdentifier node1 = findIdentifier("print(1)");
    SimpleIdentifier node2 = findIdentifier("print(2)");
    List<AstNode> nodes = ImmutableList.<AstNode> of(node1, node2);
    AstNode result = CorrectionUtils.getNearestCommonAncestor(nodes);
    assertEquals("{print(1); print(2);}", result.toSource());
  }

  public void test_getNearestCommonAncestor_singleNode() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(1);",
        "}",
        "");
    SimpleIdentifier node1 = findIdentifier("print(1)");
    List<AstNode> nodes = ImmutableList.<AstNode> of(node1);
    AstNode result = CorrectionUtils.getNearestCommonAncestor(nodes);
    assertSame(node1.getParent(), result);
  }

  /**
   * Test for {@link CorrectionUtils#getNodePrefix(DartNode)}.
   */
  public void test_getNodePrefix_block_noPrefix() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "var a;",
        "}",
        "");
    assert_getNodePrefix("var a;", "");
  }

  /**
   * Test for {@link CorrectionUtils#getNodePrefix(DartNode)}.
   */
  public void test_getNodePrefix_block_withPrefix() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var a;",
        "}",
        "");
    assert_getNodePrefix("var a;", "  ");
  }

  /**
   * Test for {@link CorrectionUtils#getNodePrefix(DartNode)}.
   */
  public void test_getNodePrefix_FunctionExpression() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "foo(x) {}",
        "main() {",
        "  foo(() => true);",
        "}",
        "");
    // find node
    FunctionExpression node = findNode("() => true", FunctionExpression.class);
    // assert prefix
    CorrectionUtils utils = getTestCorrectionUtils();
    assertEquals("  ", utils.getNodePrefix(node));
  }

  /**
   * Test for {@link CorrectionUtils#getNodePrefix(DartNode)}.
   */
  public void test_getNodePrefix_noPrefix() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var b;var a;",
        "}",
        "");
    assert_getNodePrefix("var a;", "");
  }

  public void test_getNodeQualifier_PrefixedIdentifier() throws Exception {
    SimpleIdentifier name = AstFactory.identifier("");
    // no parent
    assertSame(null, CorrectionUtils.getNodeQualifier(name));
    // not PrefixedIdentifier
    {
      AstFactory.namedExpression("label", name);
      assertSame(null, CorrectionUtils.getNodeQualifier(name));
    }
    // not "identifier" in PrefixedIdentifier
    {
      AstFactory.identifier(name, AstFactory.identifier("otherName"));
      assertSame(null, CorrectionUtils.getNodeQualifier(name));
    }
    // OK, "identifier" in PrefixedIdentifier
    {
      SimpleIdentifier target = AstFactory.identifier("A");
      AstFactory.identifier(target, name);
      assertSame(target, CorrectionUtils.getNodeQualifier(name));
    }
  }

  public void test_getNodeQualifier_PropertyAccess() throws Exception {
    SimpleIdentifier name = AstFactory.identifier("");
    // no parent
    assertSame(null, CorrectionUtils.getNodeQualifier(name));
    // not PropertyAccess
    {
      AstFactory.namedExpression("label", name);
      assertSame(null, CorrectionUtils.getNodeQualifier(name));
    }
    // not "name" in PropertyAccess
    {
      AstFactory.propertyAccess(name, "otherName");
      assertSame(null, CorrectionUtils.getNodeQualifier(name));
    }
    // OK, "name" in PropertyAccess
    {
      Expression target = AstFactory.thisExpression();
      AstFactory.propertyAccess(target, name);
      assertSame(target, CorrectionUtils.getNodeQualifier(name));
    }
  }

  public void test_getNonWhitespaceForward() throws Exception {
    String code = " \t//0123456789  ";
    parseTestUnit(code);
    CorrectionUtils utils = getTestCorrectionUtils();
    assertEquals(2, utils.getNonWhitespaceForward(0));
    assertEquals(2, utils.getNonWhitespaceForward(1));
    assertEquals(2, utils.getNonWhitespaceForward(2));
    assertEquals(3, utils.getNonWhitespaceForward(3));
    assertEquals(code.length(), utils.getNonWhitespaceForward(code.indexOf("9") + 1));
  }

  public void test_getParameterElement_local() throws Exception {
    LocalVariableElement element = mock(LocalVariableElement.class);
    SimpleIdentifier identifier = AstFactory.identifier("name");
    identifier.setStaticElement(element);
    // check
    assertSame(null, CorrectionUtils.getParameterElement(identifier));
  }

  public void test_getParameterElement_parameter() throws Exception {
    ParameterElement element = mock(ParameterElement.class);
    SimpleIdentifier identifier = AstFactory.identifier("name");
    identifier.setStaticElement(element);
    // check
    assertSame(element, CorrectionUtils.getParameterElement(identifier));
  }

  public void test_getParameterSource() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:async' as pref;",
        "topLevel(double a, int b(double p1, pref.Future p2), dynamic c, d) {",
        "}",
        "");
    CorrectionUtils utils = getTestCorrectionUtils();
    // 'a'
    {
      ParameterElement p = findIdentifierElement("a, ");
      assertEquals("double a", utils.getParameterSource(p.getType(), p.getName()));
    }
    // 'b'
    {
      ParameterElement p = findIdentifierElement("b(double ");
      assertEquals(
          "int b(double p1, pref.Future p2)",
          utils.getParameterSource(p.getType(), p.getName()));
    }
    // 'c'
    {
      ParameterElement p = findIdentifierElement("c, ");
      assertEquals("c", utils.getParameterSource(p.getType(), p.getName()));
    }
    // 'd'
    {
      ParameterElement p = findIdentifierElement("d) {");
      assertEquals("d", utils.getParameterSource(p.getType(), p.getName()));
    }
  }

  public void test_getParentPrecedence() throws Exception {
    SimpleIdentifier a = identifier("a");
    SimpleIdentifier b = identifier("b");
    // binary
    {
      binaryExpression(a, TokenType.PLUS, b);
      assertEquals(TokenType.PLUS.getPrecedence(), CorrectionUtils.getParentPrecedence(a));
    }
    // no operator
    {
      SimpleIdentifier expr = identifier("c");
      assertEquals(-1, CorrectionUtils.getParentPrecedence(expr));
    }
  }

  public void test_getParents() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(0);",
        "}",
        "");
    SimpleIdentifier node = findIdentifier("print(0)");
    // prepare parents
    List<AstNode> parents = CorrectionUtils.getParents(node);
    // check first/last nodes
    assertThat(parents).hasSize(7);
    assertSame(testUnit, parents.get(0));
    assertSame(node.getParent(), parents.get(6));
    // check expected path
    assertThat(parents.get(1)).isInstanceOf(FunctionDeclaration.class);
    assertThat(parents.get(2)).isInstanceOf(FunctionExpression.class);
    assertThat(parents.get(3)).isInstanceOf(BlockFunctionBody.class);
    assertThat(parents.get(4)).isInstanceOf(Block.class);
    assertThat(parents.get(5)).isInstanceOf(ExpressionStatement.class);
    assertThat(parents.get(6)).isInstanceOf(MethodInvocation.class);
  }

  public void test_getPrecedence() throws Exception {
    SimpleIdentifier a = identifier("a");
    SimpleIdentifier b = identifier("b");
    // binary
    assertEquals(
        TokenType.PLUS.getPrecedence(),
        CorrectionUtils.getPrecedence(binaryExpression(a, TokenType.PLUS, b)));
    assertEquals(
        TokenType.STAR.getPrecedence(),
        CorrectionUtils.getPrecedence(binaryExpression(a, TokenType.STAR, b)));
    // prefix
    assertEquals(
        TokenType.BANG.getPrecedence(),
        CorrectionUtils.getPrecedence(prefixExpression(TokenType.BANG, a)));
    // postfix
    assertEquals(
        TokenType.PLUS_PLUS.getPrecedence(),
        CorrectionUtils.getPrecedence(postfixExpression(a, TokenType.PLUS_PLUS)));
    // no operator
    assertEquals(Integer.MAX_VALUE, CorrectionUtils.getPrecedence(a));
  }

  public void test_getPropertyAccessorElement_accessor() throws Exception {
    PropertyAccessorElement element = mock(PropertyAccessorElement.class);
    SimpleIdentifier identifier = AstFactory.identifier("name");
    identifier.setStaticElement(element);
    // check
    assertSame(element, CorrectionUtils.getPropertyAccessorElement(identifier));
  }

  public void test_getPropertyAccessorElement_local() throws Exception {
    LocalVariableElement element = mock(LocalVariableElement.class);
    SimpleIdentifier identifier = AstFactory.identifier("name");
    identifier.setStaticElement(element);
    // check
    assertSame(null, CorrectionUtils.getPropertyAccessorElement(identifier));
  }

  public void test_getQualifiedPropertyTarget_invocation() throws Exception {
    SimpleIdentifier target = identifier("target");
    SimpleIdentifier name = identifier("name");
    new MethodInvocation(target, null, name, null);
    assertSame(null, CorrectionUtils.getQualifiedPropertyTarget(name));
  }

  public void test_getQualifiedPropertyTarget_prefixedIdentifier() throws Exception {
    SimpleIdentifier prefix = identifier("prefix");
    SimpleIdentifier name = identifier("name");
    identifier(prefix, name);
    assertSame(prefix, CorrectionUtils.getQualifiedPropertyTarget(name));
  }

  public void test_getQualifiedPropertyTarget_propertyAccess() throws Exception {
    SimpleIdentifier target = identifier("target");
    SimpleIdentifier name = identifier("name");
    propertyAccess(target, name);
    assertSame(target, CorrectionUtils.getQualifiedPropertyTarget(name));
  }

  public void test_getRecommendedFileNameForClass() throws Exception {
    assertEquals("test.dart", CorrectionUtils.getRecommentedFileNameForClass("Test"));
    assertEquals("my_test.dart", CorrectionUtils.getRecommentedFileNameForClass("MyTest"));
    assertEquals(
        "my_super_test.dart",
        CorrectionUtils.getRecommentedFileNameForClass("MySuperTest"));
    assertEquals("http_server.dart", CorrectionUtils.getRecommentedFileNameForClass("HTTPServer"));
  }

  public void test_getSingleStatement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var singleStatement;",
        "  { // marker-1",
        "    var blockWithSingleStatement;",
        "  }",
        "  { // marker-2",
        "    var blockWith;",
        "    var severalStatements;",
        "  }",
        "}");
    {
      Statement statement = findNode("var singleStatement", Statement.class);
      assertSame(statement, CorrectionUtils.getSingleStatement(statement));
    }
    {
      Block block = findNode("{ // marker-1", Block.class);
      Statement statement = findNode("var blockWithSingleStatement", Statement.class);
      assertSame(statement, CorrectionUtils.getSingleStatement(block));
    }
    {
      Block block = findNode("{ // marker-2", Block.class);
      assertSame(null, CorrectionUtils.getSingleStatement(block));
    }
  }

  public void test_getStatements() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var singleStatement;",
        "  { // marker-1",
        "    var blockWithSingleStatement;",
        "  }",
        "  { // marker-2",
        "    var statementA;",
        "    var statementB;",
        "  }",
        "}");
    {
      Statement statement = findNode("var singleStatement", Statement.class);
      assertThat(CorrectionUtils.getStatements(statement)).containsExactly(statement);
    }
    {
      Block block = findNode("{ // marker-1", Block.class);
      Statement statement = findNode("var blockWithSingleStatement", Statement.class);
      assertThat(CorrectionUtils.getStatements(block)).containsExactly(statement);
    }
    {
      Block block = findNode("{ // marker-2", Block.class);
      Statement statementA = findNode("var statementA;", Statement.class);
      Statement statementB = findNode("var statementB", Statement.class);
      assertThat(CorrectionUtils.getStatements(block)).containsExactly(statementA, statementB);
    }
  }

  public void test_getStringPrefix() throws Exception {
    assertEquals("", CorrectionUtils.getStringPrefix(""));
    assertEquals("", CorrectionUtils.getStringPrefix("01234"));
    assertEquals("", CorrectionUtils.getStringPrefix("0 1234"));
    assertEquals(" ", CorrectionUtils.getStringPrefix(" 01234"));
    assertEquals("  ", CorrectionUtils.getStringPrefix("  01234"));
  }

  /**
   * Test for {@link CorrectionUtils#getText()}.
   */
  public void test_getText() throws Exception {
    String code = "// 0123456789";
    parseTestUnit(code);
    CorrectionUtils utils = getTestCorrectionUtils();
    assertEquals(code, utils.getText());
  }

  /**
   * Test for {@link CorrectionUtils#getText(AstNode)}.
   */
  public void test_getText_ASTNode() throws Exception {
    parseTestUnit("class AAA {}");
    AstNode node = findNode("AAA {", Identifier.class);
    CorrectionUtils utils = getTestCorrectionUtils();
    assertEquals("AAA", utils.getText(node));
  }

  /**
   * Test for {@link CorrectionUtils#getText(int, int)}.
   */
  public void test_getText_ints() throws Exception {
    parseTestUnit("// 0123456789");
    CorrectionUtils utils = getTestCorrectionUtils();
    assertEquals("0123", utils.getText(3, 4));
  }

  /**
   * Test for {@link CorrectionUtils#getText(SourceRange)}.
   */
  public void test_getText_SourceRange() throws Exception {
    parseTestUnit("// 0123456789");
    CorrectionUtils utils = getTestCorrectionUtils();
    assertEquals("0123", utils.getText(rangeStartLength(3, 4)));
  }

  public void test_getTypeSource_Expression() throws Exception {
    parseTestUnit("// 0123456789");
    CorrectionUtils utils = getTestCorrectionUtils();
    // prepare mocks
    Type type = mock(Type.class);
    Expression expression = mock(Expression.class);
    when(expression.getStaticType()).thenReturn(type);
    // null
    assertEquals(null, utils.getTypeSource((Expression) null));
    // int
    when(type.toString()).thenReturn("int");
    assertEquals("int", utils.getTypeSource(expression));
    // dynamic
    when(type.toString()).thenReturn("dynamic");
    assertEquals(null, utils.getTypeSource(expression));
    // List<dynamic>
    when(type.toString()).thenReturn("List<dynamic>");
    assertEquals("List", utils.getTypeSource(expression));
    // List<String>
    when(type.toString()).thenReturn("List<String>");
    assertEquals("List<String>", utils.getTypeSource(expression));
    // Map<dynamic, dynamic>
    when(type.toString()).thenReturn("Map<dynamic, dynamic>");
    assertEquals("Map", utils.getTypeSource(expression));
    // Map<int, String>
    when(type.toString()).thenReturn("Map<int, String>");
    assertEquals("Map<int, String>", utils.getTypeSource(expression));
  }

  public void test_getTypeSource_Expression_importedWithPrefix() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:async' as pref;",
        "main() {",
        "  pref.Future f = null;",
        "}",
        "");
    CorrectionUtils utils = getTestCorrectionUtils();
    SimpleIdentifier identifier = findIdentifier("f = ");
    assertEquals("pref.Future", utils.getTypeSource(identifier));
  }

  public void test_getTypeSource_Expression_importedWithPrefix_typeArgument() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:async' as pref;",
        "main() {",
        "  Map<pref.Future, List<pref.Future>> f = null;",
        "}",
        "");
    CorrectionUtils utils = getTestCorrectionUtils();
    SimpleIdentifier identifier = findIdentifier("f = ");
    assertEquals("Map<pref.Future, List<pref.Future>>", utils.getTypeSource(identifier));
  }

  public void test_getTypeSource_Type() throws Exception {
    parseTestUnit("// 0123456789");
    CorrectionUtils utils = getTestCorrectionUtils();
    // prepare mocks
    Type type = mock(Type.class);
    // int
    when(type.toString()).thenReturn("int");
    assertEquals("int", utils.getTypeSource(type));
    // dynamic
    when(type.toString()).thenReturn("dynamic");
    assertEquals("dynamic", utils.getTypeSource(type));
    // List<dynamic>
    when(type.toString()).thenReturn("List<dynamic>");
    assertEquals("List", utils.getTypeSource(type));
    // Map<dynamic, dynamic>
    when(type.toString()).thenReturn("Map<dynamic, dynamic>");
    assertEquals("Map", utils.getTypeSource(type));
  }

  public void test_getUnit() throws Exception {
    parseTestUnit("");
    CorrectionUtils utils = getTestCorrectionUtils();
    assertSame(testUnit, utils.getUnit());
  }

  public void test_getVariableNameSuggestions_expectedType() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class TreeNode {}",
        "main() {",
        "  TreeNode node = null;",
        "}");
    Expression expression = findNode("null;", Expression.class);
    Type expectedType = ((VariableDeclaration) expression.getParent()).getElement().getType();
    assert_getVariableNameSuggestions(
        expectedType,
        expression,
        ImmutableSet.of(""),
        formatLines("treeNode", "node"));
  }

  public void test_getVariableNameSuggestions_expectedType_double() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class TreeNode {}",
        "main() {",
        "  double res = 0.0;",
        "}");
    Expression expression = findNode("0;", Expression.class);
    Type expectedType = ((VariableDeclaration) expression.getParent()).getElement().getType();
    // first choice for "double" is "d"
    assert_getVariableNameSuggestions(
        expectedType,
        expression,
        ImmutableSet.of(""),
        formatLines("d"));
    // if "d" is used, try "e", "f", etc
    assert_getVariableNameSuggestions(
        expectedType,
        expression,
        ImmutableSet.of("d", "e"),
        formatLines("f"));
  }

  public void test_getVariableNameSuggestions_expectedType_int() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class TreeNode {}",
        "main() {",
        "  int res = 0;",
        "}");
    Expression expression = findNode("0;", Expression.class);
    Type expectedType = ((VariableDeclaration) expression.getParent()).getElement().getType();
    // first choice for "int" is "i"
    assert_getVariableNameSuggestions(
        expectedType,
        expression,
        ImmutableSet.of(""),
        formatLines("i"));
    // if "i" is used, try "j", "k", etc
    assert_getVariableNameSuggestions(
        expectedType,
        expression,
        ImmutableSet.of("i", "j"),
        formatLines("k"));
  }

  public void test_getVariableNameSuggestions_expectedType_String() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class TreeNode {}",
        "main() {",
        "  String res = '';",
        "}");
    Expression expression = findNode("'';", Expression.class);
    Type expectedType = ((VariableDeclaration) expression.getParent()).getElement().getType();
    // first choice for "String" is "s"
    assert_getVariableNameSuggestions(
        expectedType,
        expression,
        ImmutableSet.of(""),
        formatLines("s"));
  }

  public void test_getVariableNameSuggestions_invocationArgument_named() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "foo({a, b, c}) {}",
        "main() {",
        "  foo(a: 111, c: 333, b: 222);",
        "}");
    assert_getVariableNameSuggestions(
        null,
        findNode("111", Expression.class),
        ImmutableSet.of(""),
        formatLines("a"));
    assert_getVariableNameSuggestions(
        null,
        findNode("222", Expression.class),
        ImmutableSet.of(""),
        formatLines("b"));
    assert_getVariableNameSuggestions(
        null,
        findNode("333", Expression.class),
        ImmutableSet.of(""),
        formatLines("c"));
  }

  public void test_getVariableNameSuggestions_invocationArgument_optional() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "foo(a, [b = 2, c = 3]) {}",
        "main() {",
        "  foo(111, 222, 333);",
        "}");
    assert_getVariableNameSuggestions(
        null,
        findNode("111", Expression.class),
        ImmutableSet.of(""),
        formatLines("a"));
    assert_getVariableNameSuggestions(
        null,
        findNode("222", Expression.class),
        ImmutableSet.of(""),
        formatLines("b"));
    assert_getVariableNameSuggestions(
        null,
        findNode("333", Expression.class),
        ImmutableSet.of(""),
        formatLines("c"));
  }

  public void test_getVariableNameSuggestions_invocationArgument_positional() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "foo(a, b) {}",
        "main() {",
        "  foo(111, 222);",
        "}");
    assert_getVariableNameSuggestions(
        null,
        findNode("111", Expression.class),
        ImmutableSet.of(""),
        formatLines("a"));
    assert_getVariableNameSuggestions(
        null,
        findNode("222", Expression.class),
        ImmutableSet.of(""),
        formatLines("b"));
  }

  public void test_getVariableNameSuggestions_Node_cast() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var sortedNodes;",
        "  var res = sortedNodes as String;",
        "}");
    assert_getVariableNameSuggestions(
        null,
        findNode("sortedNodes as String", AsExpression.class),
        ImmutableSet.of(""),
        formatLines("sortedNodes", "nodes"));
  }

  public void test_getVariableNameSuggestions_Node_methodInvocation() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var doc;",
        "main() {",
        "  var res = doc.getSortedNodes();",
        "}");
    assert_getVariableNameSuggestions(
        null,
        findNode("doc.getSortedNodes()", MethodInvocation.class),
        ImmutableSet.of(""),
        formatLines("sortedNodes", "nodes"));
  }

  /**
   * "get" is valid, but not nice name.
   */
  public void test_getVariableNameSuggestions_Node_name_get() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var doc;",
        "main() {",
        "  var res = doc.get();",
        "}");
    assert_getVariableNameSuggestions(
        null,
        findNode("doc.get()", MethodInvocation.class),
        ImmutableSet.of(""),
        formatLines());
  }

  public void test_getVariableNameSuggestions_Node_noPrefix() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var doc;",
        "main() {",
        "  var res = doc.sortedNodes();",
        "}");
    assert_getVariableNameSuggestions(
        null,
        findNode("doc.sortedNodes()", MethodInvocation.class),
        ImmutableSet.of(""),
        formatLines("sortedNodes", "nodes"));
  }

  public void test_getVariableNameSuggestions_Node_propertyAccess() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var doc;",
        "main() {",
        "  var res = doc.sortedNodes;",
        "}");
    // TODO(scheglov) would be good if resolver rewrite this to PropertyAccess
    assert_getVariableNameSuggestions(
        null,
        findNode("doc.sortedNodes", PrefixedIdentifier.class),
        ImmutableSet.of(""),
        formatLines("sortedNodes", "nodes"));
  }

  public void test_getVariableNameSuggestions_Node_simpleName() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var sortedNodes = null;",
        "  var res = sortedNodes;",
        "}");
    assert_getVariableNameSuggestions(
        null,
        findNode("sortedNodes;", Expression.class),
        ImmutableSet.of(""),
        formatLines("sortedNodes", "nodes"));
  }

  public void test_getVariableNameSuggestions_Node_unqualifiedInvocation() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "getSortedNodes() {}",
        "main() {",
        "  var res = getSortedNodes();",
        "}");
    assert_getVariableNameSuggestions(
        null,
        findNode("getSortedNodes();", MethodInvocation.class),
        ImmutableSet.of(""),
        formatLines("sortedNodes", "nodes"));
  }

  public void test_getVariableNameSuggestions_Node_unresolvedInstanceCreation() throws Exception {
    verifyNoTestUnitErrors = false;
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:math' as p;",
        "main() {",
        "  new NoSuchClass();",
        "  new p.NoSuchClass();",
        "  new NoSuchClass.named();",
        "}");
    assert_getVariableNameSuggestions(
        null,
        findNode("new NoSuchClass()", InstanceCreationExpression.class),
        ImmutableSet.of(""),
        formatLines("noSuchClass", "suchClass", "class"));
    assert_getVariableNameSuggestions(
        null,
        findNode("new p.NoSuchClass()", InstanceCreationExpression.class),
        ImmutableSet.of(""),
        formatLines("noSuchClass", "suchClass", "class"));
    assert_getVariableNameSuggestions(
        null,
        findNode("new NoSuchClass.named()", InstanceCreationExpression.class),
        ImmutableSet.of(""),
        formatLines("noSuchClass", "suchClass", "class"));
  }

  public void test_getVariableNameSuggestions_Node_withExclude() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var sortedTreeNodes = null;",
        "  var res = sortedTreeNodes;",
        "}");
    assert_getVariableNameSuggestions(
        null,
        findNode("sortedTreeNodes", Expression.class),
        ImmutableSet.of("treeNodes"),
        formatLines("sortedTreeNodes", "treeNodes2", "nodes"));
  }

  public void test_getVariableNameSuggestions_String() throws Exception {
    {
      Set<String> nameSuggestExclude = ImmutableSet.of("");
      String[] suggestions = CorrectionUtils.getVariableNameSuggestions(
          "Goodbye, cruel world!",
          nameSuggestExclude);
      assertThat(suggestions).isEqualTo(new String[] {"goodbyeCruelWorld", "cruelWorld", "world"});
    }
    {
      Set<String> nameSuggestExclude = ImmutableSet.of("world");
      String[] suggestions = CorrectionUtils.getVariableNameSuggestions(
          "Goodbye, cruel world!",
          nameSuggestExclude);
      assertThat(suggestions).isEqualTo(new String[] {"goodbyeCruelWorld", "cruelWorld", "world2"});
    }
  }

  public void test_getVariableNameSuggestions_String_multipleUpper() throws Exception {
    List<String> suggestions = getVariableNameSuggestions_String("sortedHTMLNodes");
    assertThat(suggestions).containsExactly("sortedHTMLNodes", "htmlNodes", "nodes");
  }

  public void test_getVariableNameSuggestions_String_simpleCamel() throws Exception {
    List<String> suggestions = getVariableNameSuggestions_String("sortedNodes");
    assertThat(suggestions).containsExactly("sortedNodes", "nodes");
  }

  public void test_getVariableNameSuggestions_String_simpleName() throws Exception {
    List<String> suggestions = getVariableNameSuggestions_String("name");
    assertThat(suggestions).containsExactly("name");
  }

  public void test_invertCondition_compare() throws Exception {
    assert_invertCondition("0 < 1", "0 >= 1");
    assert_invertCondition("0 > 1", "0 <= 1");
    assert_invertCondition("0 <= 1", "0 > 1");
    assert_invertCondition("0 >= 1", "0 < 1");
    assert_invertCondition("0 == 1", "0 != 1");
    assert_invertCondition("0 != 1", "0 == 1");
  }

  public void test_invertCondition_complex() throws Exception {
    assert_invertCondition("b1 && b2 || b3", "(!b1 || !b2) && !b3");
    assert_invertCondition("b1 || b2 && b3", "!b1 && (!b2 || !b3)");
    assert_invertCondition("(!b1 || !b2) && !b3", "b1 && b2 || b3");
    assert_invertCondition("!b1 && (!b2 || !b3)", "b1 || b2 && b3");
  }

  public void test_invertCondition_is() throws Exception {
    assert_invertCondition("v1 is int", "v1 is! int");
    assert_invertCondition("v1 is! int", "v1 is int");
  }

  public void test_invertCondition_literals() throws Exception {
    assert_invertCondition("true", "false");
    assert_invertCondition("false", "true");
  }

  public void test_invertCondition_logical() throws Exception {
    assert_invertCondition("b1 && b2", "!b1 || !b2");
    assert_invertCondition("!b1 && !b2", "b1 || b2");
    assert_invertCondition("b1 || b2", "!b1 && !b2");
    assert_invertCondition("!b1 || !b2", "b1 && b2");
  }

  public void test_invertCondition_simple() throws Exception {
    assert_invertCondition("b1", "!b1");
    assert_invertCondition("!b1", "b1");
    assert_invertCondition("!((b1))", "b1");
    assert_invertCondition("(((b1)))", "!b1");
  }

  public void test_isJustWhitespace() throws Exception {
    parseTestUnit("//  0123");
    CorrectionUtils utils = getTestCorrectionUtils();
    assertTrue(utils.isJustWhitespace(rangeStartLength(2, 1)));
    assertTrue(utils.isJustWhitespace(rangeStartLength(2, 2)));
    assertFalse(utils.isJustWhitespace(rangeStartLength(0, 1)));
    assertFalse(utils.isJustWhitespace(rangeStartLength(2, 3)));
  }

  public void test_isJustWhitespaceOrComment() throws Exception {
    parseTestUnit("var a;  // 0123");
    CorrectionUtils utils = getTestCorrectionUtils();
    // whitespace
    assertTrue(utils.isJustWhitespaceOrComment(rangeStartLength(6, 1)));
    assertTrue(utils.isJustWhitespaceOrComment(rangeStartLength(6, 2)));
    // whitespace + comment
    assertTrue(utils.isJustWhitespaceOrComment(rangeStartLength(6, 4)));
    assertTrue(utils.isJustWhitespaceOrComment(rangeStartLength(6, 5)));
    // not whitespace
    assertFalse(utils.isJustWhitespaceOrComment(rangeStartLength(0, 1)));
    assertFalse(utils.isJustWhitespaceOrComment(rangeStartLength(3, 2)));
  }

  public void test_isNamedExpressionName_name() throws Exception {
    SimpleIdentifier labelIdentifier = identifier("name");
    namedExpression(label(labelIdentifier), identifier("value"));
    assertTrue(CorrectionUtils.isNamedExpressionName(labelIdentifier));
  }

  public void test_isNamedExpressionName_value() throws Exception {
    SimpleIdentifier labelIdentifier = identifier("name");
    SimpleIdentifier valueIdentifier = identifier("value");
    namedExpression(label(labelIdentifier), valueIdentifier);
    assertFalse(CorrectionUtils.isNamedExpressionName(valueIdentifier));
  }

  public void test_new_withCharBuffer() throws Exception {
    AnalysisContext context = new AnalysisContextImpl();
    context.setSourceFactory(new SourceFactory());
    Source source = mock(Source.class);
    CompilationUnit unit = mock(CompilationUnit.class);
    CompilationUnitElement unitElement = mock(CompilationUnitElement.class);
    when(unitElement.getContext()).thenReturn(context);
    when(unit.getElement()).thenReturn(unitElement);
    when(unitElement.getSource()).thenReturn(source);
    // mock content
    final CharBuffer charBuffer = mock(CharBuffer.class);
    when(charBuffer.toString()).thenReturn("// 0123");
    when(source.getContents()).thenReturn(new TimestampedData<CharSequence>(0L, charBuffer));
    // create CorrectionUtils, ask content
    CorrectionUtils utils = new CorrectionUtils(unit);
    // verify that content was requested
    verify(source).getContents();
    assertEquals("// 0123", utils.getText());
  }

  public void test_selectionIncludesNonWhitespaceOutsideNode() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var v = 111 + 222 + 333;",
        "");
    CorrectionUtils utils = getTestCorrectionUtils();
    AstNode node = findNode("222", IntegerLiteral.class);
    // "selection" does not cover node
    {
      SourceRange selection = rangeStartEnd(findOffset("22 "), findEnd("22 "));
      assertFalse(utils.selectionIncludesNonWhitespaceOutsideNode(selection, node));
    }
    // same range
    {
      SourceRange selection = rangeStartEnd(findOffset("222"), findEnd("222"));
      assertFalse(utils.selectionIncludesNonWhitespaceOutsideNode(selection, node));
    }
    // leading whitespace
    {
      SourceRange selection = rangeStartEnd(findOffset(" 222"), findEnd("222"));
      assertFalse(utils.selectionIncludesNonWhitespaceOutsideNode(selection, node));
    }
    // trailing whitespace
    {
      SourceRange selection = rangeStartEnd(findOffset("222"), findEnd("222 "));
      assertFalse(utils.selectionIncludesNonWhitespaceOutsideNode(selection, node));
    }
    // non-whitespace leading token
    {
      SourceRange selection = rangeStartEnd(findOffset("+ 222"), findEnd("222"));
      assertTrue(utils.selectionIncludesNonWhitespaceOutsideNode(selection, node));
    }
    // non-whitespace trailing token
    {
      SourceRange selection = rangeStartEnd(findOffset("222"), findEnd("222 +"));
      assertTrue(utils.selectionIncludesNonWhitespaceOutsideNode(selection, node));
    }
  }

  public void test_validateBinaryExpressionRange_OK_leadingComment() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var v = /* foo */ 111 + 222;",
        "");
    CorrectionUtils utils = getTestCorrectionUtils();
    BinaryExpression node = findVariableInitializer("v = ");
    // validate
    SourceRange selection = rangeStartEnd(findOffset("/* foo */ 111"), findEnd("111"));
    assertTrue(utils.validateBinaryExpressionRange(node, selection));
  }

  public void test_validateBinaryExpressionRange_OK_leadingWhitespace() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var v = 111 + 222;",
        "");
    CorrectionUtils utils = getTestCorrectionUtils();
    BinaryExpression node = findVariableInitializer("v = ");
    // validate
    SourceRange selection = rangeStartEnd(findOffset(" 111"), findEnd(" 111"));
    assertTrue(utils.validateBinaryExpressionRange(node, selection));
  }

  public void test_validateBinaryExpressionRange_OK_startOnFirst_endOnLast() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var v = 1 + 2 + 3;",
        "");
    CorrectionUtils utils = getTestCorrectionUtils();
    BinaryExpression node = findVariableInitializer("v = ");
    // validate
    SourceRange selection = rangeStartEnd(findOffset("1"), findEnd("3"));
    assertTrue(utils.validateBinaryExpressionRange(node, selection));
  }

  public void test_validateBinaryExpressionRange_OK_trailingComment() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var v = 111 /* foo */+ 222;",
        "");
    CorrectionUtils utils = getTestCorrectionUtils();
    BinaryExpression node = findVariableInitializer("v = ");
    // validate
    SourceRange selection = rangeStartEnd(findOffset("111 /* foo */"), findEnd("111 /* foo */"));
    assertTrue(utils.validateBinaryExpressionRange(node, selection));
  }

  public void test_validateBinaryExpressionRange_OK_trailingWhitespace() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var v = 111 + 222;",
        "");
    CorrectionUtils utils = getTestCorrectionUtils();
    BinaryExpression node = findVariableInitializer("v = ");
    // validate
    SourceRange selection = rangeStartEnd(findOffset("111"), findEnd("111 "));
    assertTrue(utils.validateBinaryExpressionRange(node, selection));
  }

  public void test_validateBinaryExpressionRange_wrong_leadingToken() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var v = 111 + 222;",
        "");
    CorrectionUtils utils = getTestCorrectionUtils();
    BinaryExpression node = findVariableInitializer("v = ");
    // validate
    SourceRange selection = rangeStartEnd(findOffset("+ 222"), findEnd("+ 222"));
    assertFalse(utils.validateBinaryExpressionRange(node, selection));
  }

  public void test_validateBinaryExpressionRange_wrong_notAssociative() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var v = 1 - 2;",
        "");
    CorrectionUtils utils = getTestCorrectionUtils();
    BinaryExpression node = findVariableInitializer("v = ");
    // validate
    SourceRange selection = rangeStartEnd(findOffset("1"), findEnd("2"));
    assertFalse(utils.validateBinaryExpressionRange(node, selection));
  }

  public void test_validateBinaryExpressionRange_wrong_startInOperandMiddle() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var v = 111 + 222;",
        "");
    CorrectionUtils utils = getTestCorrectionUtils();
    BinaryExpression node = findVariableInitializer("v = ");
    // validate
    SourceRange selection = rangeStartEnd(findOffset("11 +"), findEnd("= 111"));
    assertFalse(utils.validateBinaryExpressionRange(node, selection));
  }

  public void test_validateBinaryExpressionRange_wrong_trailingToken() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var v = 111 + 222;",
        "");
    CorrectionUtils utils = getTestCorrectionUtils();
    BinaryExpression node = findVariableInitializer("v = ");
    // validate
    SourceRange selection = rangeStartEnd(findOffset("111 +"), findEnd("111 +"));
    assertFalse(utils.validateBinaryExpressionRange(node, selection));
  }

  /**
   * Asserts that {@link ExtractUtils#getNodePrefix(DartNode)} in {@link #testUnit} has expected
   * prefix.
   */
  private void assert_getNodePrefix(String nodePattern, String expectedPrefix) throws Exception {
    // find node
    VariableDeclarationStatement node = findNode(nodePattern, VariableDeclarationStatement.class);
    // assert prefix
    CorrectionUtils utils = getTestCorrectionUtils();
    assertEquals(expectedPrefix, utils.getNodePrefix(node));
  }

  private void assert_invertCondition(String exprSource, String expected) throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v1, v2, v3, v4, v5;",
        "  bool b1, b2, b3, b4, b5;",
        "  if (" + exprSource + ") // marker",
        "    0;",
        "  else",
        "    1;",
        "}",
        "");
    parseTestUnit(initial);
    IfStatement ifStatement = findNode("if (", IfStatement.class);
    Expression condition = ifStatement.getCondition();
    String resultSource = getTestCorrectionUtils().invertCondition(condition);
    assertEquals(expected, resultSource);
  }

  @SuppressWarnings("unchecked")
  private <T extends AstNode> T findVariableInitializer(String pattern) {
    return (T) findNode(pattern, VariableDeclaration.class).getInitializer();
  }

  /**
   * Calls {@link StubUtility#getVariableNameSuggestions(String)}.
   */
  @SuppressWarnings("unchecked")
  private List<String> getVariableNameSuggestions_String(String name) throws Exception {
    Method method = CorrectionUtils.class.getDeclaredMethod(
        "getVariableNameSuggestions",
        String.class);
    method.setAccessible(true);
    return (List<String>) method.invoke(null, name);
  }
}
