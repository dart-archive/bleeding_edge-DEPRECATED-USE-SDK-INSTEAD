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

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.java2dart.Context;
import com.google.dart.java2dart.processor.BeautifySemanticProcessor;
import com.google.dart.java2dart.processor.CollectionSemanticProcessor;
import com.google.dart.java2dart.processor.GuavaSemanticProcessor;
import com.google.dart.java2dart.processor.IOSemanticProcessor;
import com.google.dart.java2dart.processor.JUnitSemanticProcessor;
import com.google.dart.java2dart.processor.ObjectSemanticProcessor;
import com.google.dart.java2dart.processor.PropertySemanticProcessor;
import com.google.dart.java2dart.processor.SemanticProcessor;
import com.google.dart.java2dart.util.JavaUtils;
import com.google.dart.java2dart.util.ToFormattedSourceVisitor;

import static com.google.dart.java2dart.util.ASTFactory.exportDirective;
import static com.google.dart.java2dart.util.ASTFactory.identifier;
import static com.google.dart.java2dart.util.ASTFactory.importDirective;
import static com.google.dart.java2dart.util.ASTFactory.importHideCombinator;
import static com.google.dart.java2dart.util.ASTFactory.importShowCombinator;
import static com.google.dart.java2dart.util.ASTFactory.libraryDirective;
import static com.google.dart.java2dart.util.TokenFactory.token;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.core.dom.ITypeBinding;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

/**
 * Translates some parts of "com.google.dart.engine" project.
 */
public class MainEngine {
  /**
   * Default package src location (can be overridden)
   */
  private static String src_package = "package:analysis_engine/src/";

  private static final Context context = new Context();
  private static File engineFolder;
  private static File engineTestFolder;
  private static File engineFolder2;
  private static CompilationUnit dartUnit;

  private static final List<SemanticProcessor> PROCESSORS = ImmutableList.of(
      ObjectSemanticProcessor.INSTANCE,
      CollectionSemanticProcessor.INSTANCE,
      IOSemanticProcessor.INSTANCE,
      PropertySemanticProcessor.INSTANCE,
      GuavaSemanticProcessor.INSTANCE,
      JUnitSemanticProcessor.INSTANCE,
      BeautifySemanticProcessor.INSTANCE,
      EngineSemanticProcessor.INSTANCE);

  private static final String HEADER = "// This code was auto-generated, is not intended to be edited, and is subject to\n"
      + "// significant change. Please see the README file for more information.\n\n";

  public static void main(String[] args) throws Exception {
    if (args.length != 2 && args.length != 3) {
      System.out.println("Usage: java2dart <target-src-folder> <target-test-folder> [src-package]");
      System.exit(0);
    }
    String targetFolder = args[0];
    String targetTestFolder = args[1];
    if (args.length == 3) {
      System.out.println("Overrriding default src package to: " + src_package);
      src_package = args[2];
    }
    System.out.println("Generating files into " + targetFolder);
    new File(targetFolder).mkdirs();
    //
    engineFolder = new File("../../../tools/plugins/com.google.dart.engine/src");
    engineTestFolder = new File("../../../tools/plugins/com.google.dart.engine_test/src");
    engineFolder2 = new File("src");
    engineFolder = engineFolder.getCanonicalFile();
    // configure Context
    context.addClasspathFile(new File("../../../../third_party/guava/r13/guava-13.0.1.jar"));
    context.addClasspathFile(new File("../../../../third_party/junit/v4_8_2/junit.jar"));
    context.addSourceFolder(engineFolder);
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/utilities/ast"));
    context.addSourceFiles(new File(
        engineFolder,
        "com/google/dart/engine/utilities/instrumentation"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/sdk"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/sdk"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/source"));
    context.addSourceFile(new File(
        engineFolder,
        "com/google/dart/engine/utilities/source/LineInfo.java"));
    context.addSourceFile(new File(
        engineFolder,
        "com/google/dart/engine/utilities/source/SourceRange.java"));
    context.addSourceFile(new File(
        engineFolder,
        "com/google/dart/engine/utilities/dart/ParameterKind.java"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/ast"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/ast/visitor"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/constant"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/element"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/error"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/html/ast"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/html/ast/visitor"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/html/parser"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/html/scanner"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/parser"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/resolver"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/scanner"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/type"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/constant"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/element"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/builder"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/error"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/parser"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/resolver"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/scope"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/type"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/verifier"));
    context.addSourceFile(new File(
        engineFolder2,
        "com/google/dart/java2dart/util/ToFormattedSourceVisitor.java"));
    context.addSourceFile(new File(engineFolder, "com/google/dart/engine/AnalysisEngine.java"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/utilities/logging"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/context"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/context"));
    // Tests
    context.addSourceFile(new File(
        engineTestFolder,
        "com/google/dart/engine/utilities/io/FileUtilities2.java"));
    context.addSourceFile(new File(engineTestFolder, "com/google/dart/engine/EngineTestCase.java"));
    context.addSourceFile(new File(
        engineTestFolder,
        "com/google/dart/engine/error/GatheringErrorListener.java"));
    context.addSourceFiles(new File(engineTestFolder, "com/google/dart/engine/scanner"));
    context.addSourceFiles(new File(engineTestFolder, "com/google/dart/engine/parser"));
    context.addSourceFiles(new File(engineTestFolder, "com/google/dart/engine/ast"));
    context.addSourceFiles(new File(engineTestFolder, "com/google/dart/engine/element"));
    context.addSourceFiles(new File(engineTestFolder, "com/google/dart/engine/internal/element"));
    context.addSourceFiles(new File(engineTestFolder, "com/google/dart/engine/internal/type"));
    context.addSourceFiles(new File(engineTestFolder, "com/google/dart/engine/resolver"));
    context.addSourceFiles(new File(engineTestFolder, "com/google/dart/engine/internal/resolver"));
    context.addSourceFiles(new File(engineTestFolder, "com/google/dart/engine/internal/scope"));
    context.addSourceFiles(new File(engineTestFolder, "com/google/dart/engine/context"));
    context.addSourceFiles(new File(engineTestFolder, "com/google/dart/engine/internal/context"));
    // configure renames
    context.addRename(
        "Lcom/google/dart/engine/ast/IndexExpression;.(Lcom/google/dart/engine/ast/Expression;"
            + "Lcom/google/dart/engine/scanner/Token;Lcom/google/dart/engine/ast/Expression;"
            + "Lcom/google/dart/engine/scanner/Token;)",
        "forTarget");
    context.addRename(
        "Lcom/google/dart/engine/ast/IndexExpression;.(Lcom/google/dart/engine/scanner/Token;"
            + "Lcom/google/dart/engine/scanner/Token;Lcom/google/dart/engine/ast/Expression;"
            + "Lcom/google/dart/engine/scanner/Token;)",
        "forCascade");
    context.addRename(
        "Lcom/google/dart/engine/html/ast/XmlTagNode;.becomeParentOf<T:Lcom/google/dart/engine/html/ast/XmlNode;>(Ljava/util/List<TT;>;Ljava/util/List<TT;>;)",
        "becomeParentOfEmpty");
    // translate into single CompilationUnit
    dartUnit = context.translate();
    // run processors
    for (SemanticProcessor processor : PROCESSORS) {
      processor.process(context, dartUnit);
    }
    // run this again, because we may introduce conflicts when convert methods to getters/setters
    context.ensureUniqueClassMemberNames(dartUnit);
    context.ensureNoVariableNameReferenceFromInitializer(dartUnit);
    context.ensureMethodParameterDoesNotHide(dartUnit);
    // handle reflection
    EngineSemanticProcessor.rewriteReflectionFieldsWithDirect(context, dartUnit);
    // dump as several libraries
    Files.copy(new File("resources/java_core.dart"), new File(targetFolder + "/java_core.dart"));
    Files.copy(new File("resources/java_io.dart"), new File(targetFolder + "/java_io.dart"));
    Files.copy(new File("resources/java_junit.dart"), new File(targetFolder + "/java_junit.dart"));
    Files.copy(new File("resources/java_engine.dart"), new File(targetFolder + "/java_engine.dart"));
    Files.copy(new File("resources/java_engine_io.dart"), new File(targetFolder
        + "/java_engine_io.dart"));
    Files.copy(new File("resources/all_test.dart"), new File(targetTestFolder + "/all_test.dart"));
    {
      CompilationUnit library = buildInstrumentationLibrary();
      Files.write(
          getFormattedSource(library),
          new File(targetFolder + "/instrumentation.dart"),
          Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildSourceLibrary();
      Files.write(
          getFormattedSource(library),
          new File(targetFolder + "/source.dart"),
          Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildSourceIoLibrary();
      Files.write(
          getFormattedSource(library),
          new File(targetFolder + "/source_io.dart"),
          Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildErrorLibrary();
      Files.write(
          getFormattedSource(library),
          new File(targetFolder + "/error.dart"),
          Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildScannerLibrary();
      Files.write(
          getFormattedSource(library),
          new File(targetFolder + "/scanner.dart"),
          Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildHtmlLibrary();
      Files.write(
          getFormattedSource(library),
          new File(targetFolder + "/html.dart"),
          Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildUtilitiesDartLibrary();
      File astFile = new File(targetFolder + "/utilities_dart.dart");
      Files.write(getFormattedSource(library), astFile, Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildAstLibrary();
      File astFile = new File(targetFolder + "/ast.dart");
      Files.write(getFormattedSource(library), astFile, Charsets.UTF_8);
      Files.append(
          Files.toString(new File("resources/ast_include.dart"), Charsets.UTF_8),
          astFile,
          Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildParserLibrary();
      Files.write(
          getFormattedSource(library),
          new File(targetFolder + "/parser.dart"),
          Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildSdkLibrary();
      Files.write(getFormattedSource(library), new File(targetFolder + "/sdk.dart"), Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildSdkIoLibrary();
      Files.write(
          getFormattedSource(library),
          new File(targetFolder + "/sdk_io.dart"),
          Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildConstantLibrary();
      Files.write(
          getFormattedSource(library),
          new File(targetFolder + "/constant.dart"),
          Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildElementLibrary();
      Files.write(
          getFormattedSource(library),
          new File(targetFolder + "/element.dart"),
          Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildResolverLibrary();
      Files.write(
          getFormattedSource(library),
          new File(targetFolder + "/resolver.dart"),
          Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildEngineLibrary();
      Files.write(
          getFormattedSource(library),
          new File(targetFolder + "/engine.dart"),
          Charsets.UTF_8);
    }
    // Tests
    {
      CompilationUnit library = buildTestSupportLibrary();
      File testSupportFile = new File(targetTestFolder + "/test_support.dart");
      Files.write(getFormattedSource(library), testSupportFile, Charsets.UTF_8);
      Files.append(
          Files.toString(new File("resources/test_support_include.dart"), Charsets.UTF_8),
          testSupportFile,
          Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildScannerTestLibrary();
      Files.write(
          getFormattedSource(library),
          new File(targetTestFolder + "/scanner_test.dart"),
          Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildParserTestLibrary();
      // replace reflection methods
      StringWriter methodWriter = new StringWriter();
      EngineSemanticProcessor.replaceReflectionMethods(
          context,
          new PrintWriter(methodWriter),
          dartUnit);
      // write to file
      File libraryFile = new File(targetTestFolder + "/parser_test.dart");
      Files.write(getFormattedSource(library), libraryFile, Charsets.UTF_8);
      Files.append(methodWriter.toString(), libraryFile, Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildAstTestLibrary();
      File astFile = new File(targetTestFolder + "/ast_test.dart");
      Files.write(getFormattedSource(library), astFile, Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildElementTestLibrary();
      Files.write(
          getFormattedSource(library),
          new File(targetTestFolder + "/element_test.dart"),
          Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildResolverTestLibrary();
      Files.write(
          getFormattedSource(library),
          new File(targetTestFolder + "/resolver_test.dart"),
          Charsets.UTF_8);
    }
    System.out.println("Translation complete");
  }

  private static void addNotRemovedCompiationUnitEntries(CompilationUnit targetUnit,
      List<CompilationUnitMember> members) {
    for (CompilationUnitMember member : members) {
      // may be removed
      CompilationUnit memberUnit = (CompilationUnit) member.getParent();
      if (memberUnit == null || !memberUnit.getDeclarations().contains(member)) {
        continue;
      }
      // OK, add this member
      targetUnit.getDeclarations().add(member);
    }
  }

  private static CompilationUnit buildAstLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "ast"));
    unit.getDirectives().add(importDirective("dart:collection", null));
    unit.getDirectives().add(importDirective("java_core.dart", null));
    unit.getDirectives().add(importDirective("java_engine.dart", null));
    unit.getDirectives().add(importDirective("error.dart", null));
    unit.getDirectives().add(importDirective("source.dart", null, importShowCombinator("LineInfo")));
    unit.getDirectives().add(importDirective("scanner.dart", null));
    unit.getDirectives().add(
        importDirective("engine.dart", null, importShowCombinator("AnalysisEngine")));
    unit.getDirectives().add(importDirective("utilities_dart.dart", null));
    unit.getDirectives().add(
        importDirective("element.dart", null, importHideCombinator(identifier("Annotation"))));
    for (CompilationUnitMember member : dartUnit.getDeclarations()) {
      File file = context.getMemberToFile().get(member);
      if (isEnginePath(file, "ast/") || isEnginePath(file, "utilities/ast/")) {
        unit.getDeclarations().add(member);
      }
    }
    EngineSemanticProcessor.generateConstructorWithNamedParametersInAST(context, unit);
    return unit;
  }

  private static CompilationUnit buildAstTestLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "ast_test"));
    unit.getDirectives().add(importDirective("dart:collection", null));
    unit.getDirectives().add(importDirective(src_package + "java_core.dart", null));
    unit.getDirectives().add(importDirective(src_package + "java_engine.dart", null));
    unit.getDirectives().add(importDirective(src_package + "java_junit.dart", null));
    unit.getDirectives().add(importDirective(src_package + "source.dart", null));
    unit.getDirectives().add(importDirective(src_package + "error.dart", null));
    unit.getDirectives().add(importDirective(src_package + "scanner.dart", null));
    unit.getDirectives().add(importDirective(src_package + "ast.dart", null));
    unit.getDirectives().add(importDirective(src_package + "utilities_dart.dart", null));
    unit.getDirectives().add(
        importDirective(src_package + "element.dart", null, importShowCombinator("ClassElement")));
    unit.getDirectives().add(importDirective("package:unittest/unittest.dart", "_ut"));
    unit.getDirectives().add(
        importDirective("parser_test.dart", null, importShowCombinator("ParserTestCase")));
    unit.getDirectives().add(importDirective("test_support.dart", null));
    unit.getDirectives().add(
        importDirective("scanner_test.dart", null, importShowCombinator("TokenFactory")));
    List<Statement> mainStatements = Lists.newArrayList();
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      // TODO(scheglov) I've asked Phil to remove ResolverTestCase dependency
      if (isEngineTestPath(file, "ast/visitor/ElementLocatorTest.java")) {
        continue;
      }
      if (isEngineTestPath(file, "ast/")) {
        List<CompilationUnitMember> unitMembers = entry.getValue();
        for (CompilationUnitMember unitMember : unitMembers) {
          boolean isTestSuite = EngineSemanticProcessor.gatherTestSuites(mainStatements, unitMember);
          if (!isTestSuite) {
            unit.getDeclarations().add(unitMember);
          }
        }
      }
    }
    // TODO(scheglov) remove ElementLocatorTest, it depends on ResolverTestCase
    {
      for (Iterator<Statement> I = mainStatements.iterator(); I.hasNext();) {
        Statement statement = I.next();
        if (statement.toSource().contains("ElementLocatorTest")) {
          I.remove();
        }
      }
    }
    EngineSemanticProcessor.addMain(unit, mainStatements);
    return unit;
  }

  private static CompilationUnit buildConstantLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "constant"));
    unit.getDirectives().add(importDirective("java_core.dart", null));
    unit.getDirectives().add(importDirective("source.dart", null, importShowCombinator("Source")));
    unit.getDirectives().add(
        importDirective(
            "error.dart",
            null,
            importShowCombinator("AnalysisError", "ErrorCode", "CompileTimeErrorCode")));
    unit.getDirectives().add(
        importDirective("scanner.dart", null, importShowCombinator("TokenType")));
    unit.getDirectives().add(importDirective("ast.dart", null));
    unit.getDirectives().add(importDirective("element.dart", null));
    unit.getDirectives().add(
        importDirective("engine.dart", null, importShowCombinator("AnalysisEngine")));
    for (CompilationUnitMember member : dartUnit.getDeclarations()) {
      File file = context.getMemberToFile().get(member);
      if (isEnginePath(file, "constant/") || isEnginePath(file, "internal/constant/")) {
        unit.getDeclarations().add(member);
      }
    }
    return unit;
  }

  private static CompilationUnit buildElementLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "element"));
    unit.getDirectives().add(importDirective("dart:collection", null));
    unit.getDirectives().add(importDirective("java_core.dart", null));
    unit.getDirectives().add(importDirective("java_engine.dart", null));
    unit.getDirectives().add(importDirective("source.dart", null));
    unit.getDirectives().add(importDirective("scanner.dart", null, importShowCombinator("Keyword")));
    unit.getDirectives().add(
        importDirective("ast.dart", null, importShowCombinator("Identifier", "LibraryIdentifier")));
    unit.getDirectives().add(importDirective("sdk.dart", null, importShowCombinator("DartSdk")));
    unit.getDirectives().add(importDirective("html.dart", null, importShowCombinator("XmlTagNode")));
    unit.getDirectives().add(
        importDirective("engine.dart", null, importShowCombinator("AnalysisContext")));
    unit.getDirectives().add(
        importDirective("constant.dart", null, importShowCombinator("EvaluationResultImpl")));
    unit.getDirectives().add(importDirective("utilities_dart.dart", null));
    for (CompilationUnitMember member : dartUnit.getDeclarations()) {
      File file = context.getMemberToFile().get(member);
      if (isEnginePath(file, "internal/element/handle/")) {
        continue;
      }
      if (isEnginePath(file, "element/") || isEnginePath(file, "type/")
          || isEnginePath(file, "internal/element/") || isEnginePath(file, "internal/type/")) {
        unit.getDeclarations().add(member);
      }
    }
    return unit;
  }

  private static CompilationUnit buildElementTestLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "element_test"));
    unit.getDirectives().add(importDirective("dart:collection", null));
    unit.getDirectives().add(importDirective("dart:io", null));
    unit.getDirectives().add(importDirective(src_package + "java_core.dart", null));
    unit.getDirectives().add(importDirective(src_package + "java_engine.dart", null));
    unit.getDirectives().add(importDirective(src_package + "java_engine_io.dart", null));
    unit.getDirectives().add(importDirective(src_package + "java_junit.dart", null));
    unit.getDirectives().add(importDirective(src_package + "source_io.dart", null));
    unit.getDirectives().add(importDirective(src_package + "error.dart", null));
    unit.getDirectives().add(importDirective(src_package + "scanner.dart", null));
    unit.getDirectives().add(importDirective(src_package + "utilities_dart.dart", null));
    unit.getDirectives().add(
        importDirective(src_package + "ast.dart", null, importHideCombinator("Annotation")));
    unit.getDirectives().add(
        importDirective(src_package + "element.dart", null, importHideCombinator("Annotation")));
    unit.getDirectives().add(
        importDirective(
            src_package + "engine.dart",
            null,
            importShowCombinator("AnalysisContext", "AnalysisContextImpl")));
    unit.getDirectives().add(importDirective("package:unittest/unittest.dart", "_ut"));
    unit.getDirectives().add(importDirective("test_support.dart", null));
    unit.getDirectives().add(
        importDirective("scanner_test.dart", null, importShowCombinator("TokenFactory")));
    unit.getDirectives().add(
        importDirective("ast_test.dart", null, importShowCombinator("ASTFactory")));
    unit.getDirectives().add(
        importDirective("resolver_test.dart", null, importShowCombinator("TestTypeProvider")));
    List<Statement> mainStatements = Lists.newArrayList();
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isEngineTestPath(file, "element/") || isEngineTestPath(file, "internal/element/")
          || isEngineTestPath(file, "internal/type/")) {
        List<CompilationUnitMember> unitMembers = entry.getValue();
        for (CompilationUnitMember unitMember : unitMembers) {
          boolean isTestSuite = EngineSemanticProcessor.gatherTestSuites(mainStatements, unitMember);
          if (!isTestSuite) {
            unit.getDeclarations().add(unitMember);
          }
        }
      }
    }
    EngineSemanticProcessor.addMain(unit, mainStatements);
    return unit;
  }

  private static CompilationUnit buildEngineLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine"));
    unit.getDirectives().add(
        importDirective("dart:collection", null, importShowCombinator("HasNextIterator")));
    unit.getDirectives().add(importDirective("dart:uri", null, importShowCombinator("Uri")));
    unit.getDirectives().add(importDirective("java_core.dart", null));
    unit.getDirectives().add(importDirective("java_engine.dart", null));
    unit.getDirectives().add(importDirective("instrumentation.dart", null));
    unit.getDirectives().add(importDirective("error.dart", null));
    unit.getDirectives().add(importDirective("source.dart", null));
    unit.getDirectives().add(
        importDirective(
            "scanner.dart",
            null,
            importShowCombinator("Token", "CharBufferScanner", "StringScanner")));
    unit.getDirectives().add(importDirective("ast.dart", null));
    unit.getDirectives().add(importDirective("parser.dart", null, importShowCombinator("Parser")));
    unit.getDirectives().add(importDirective("sdk.dart", null, importShowCombinator("DartSdk")));
    unit.getDirectives().add(importDirective("element.dart", null));
    unit.getDirectives().add(importDirective("resolver.dart", null));
    unit.getDirectives().add(
        importDirective(
            "html.dart",
            null,
            importShowCombinator(
                "XmlTagNode",
                "XmlAttributeNode",
                "RecursiveXmlVisitor",
                "HtmlScanner",
                "HtmlScanResult",
                "HtmlParser",
                "HtmlParseResult",
                "HtmlUnit")));
    for (CompilationUnitMember member : dartUnit.getDeclarations()) {
      File file = context.getMemberToFile().get(member);
      if (isEnginePath(file, "AnalysisEngine.java") || isEnginePath(file, "utilities/logging/")
          || isEnginePath(file, "context/") || isEnginePath(file, "internal/context/")) {
        unit.getDeclarations().add(member);
      }
    }
    return unit;
  }

  private static CompilationUnit buildErrorLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "error"));
    unit.getDirectives().add(importDirective("java_core.dart", null));
    unit.getDirectives().add(importDirective("source.dart", null));
    unit.getDirectives().add(importDirective("ast.dart", null, importShowCombinator("ASTNode")));
    unit.getDirectives().add(importDirective("scanner.dart", null, importShowCombinator("Token")));
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isEnginePath(file, "error/") || isEnginePath(file, "internal/error/")) {
        addNotRemovedCompiationUnitEntries(unit, entry.getValue());
      }
    }
    return unit;
  }

  private static CompilationUnit buildHtmlLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "html"));
    unit.getDirectives().add(importDirective("dart:collection", null));
    unit.getDirectives().add(importDirective("java_core.dart", null));
    unit.getDirectives().add(importDirective("java_engine.dart", null));
    unit.getDirectives().add(importDirective("source.dart", null));
    unit.getDirectives().add(importDirective("error.dart", null));
    unit.getDirectives().add(importDirective("instrumentation.dart", null));
    unit.getDirectives().add(
        importDirective("element.dart", null, importShowCombinator("HtmlElementImpl")));
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isEnginePath(file, "html/scanner/") || isEnginePath(file, "html/ast/")
          || isEnginePath(file, "html/parser/")) {
        addNotRemovedCompiationUnitEntries(unit, entry.getValue());
      }
    }
    return unit;
  }

  private static CompilationUnit buildInstrumentationLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "instrumentation"));
    unit.getDirectives().add(importDirective("java_core.dart", null));
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isEnginePath(file, "utilities/instrumentation/")) {
        addNotRemovedCompiationUnitEntries(unit, entry.getValue());
      }
    }
    return unit;
  }

  private static CompilationUnit buildParserLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "parser"));
    unit.getDirectives().add(importDirective("dart:collection", null));
    unit.getDirectives().add(importDirective("java_core.dart", null));
    unit.getDirectives().add(importDirective("java_engine.dart", null));
    unit.getDirectives().add(importDirective("instrumentation.dart", null));
    unit.getDirectives().add(importDirective("error.dart", null));
    unit.getDirectives().add(importDirective("source.dart", null));
    unit.getDirectives().add(importDirective("scanner.dart", null));
    unit.getDirectives().add(importDirective("ast.dart", null));
    unit.getDirectives().add(importDirective("utilities_dart.dart", null));
    for (CompilationUnitMember member : dartUnit.getDeclarations()) {
      File file = context.getMemberToFile().get(member);
      if (isEnginePath(file, "parser/") || isEnginePath(file, "internal/parser/")
          || file.getName().equals("ToFormattedSourceVisitor.java")) {
        unit.getDeclarations().add(member);
      }
    }
    return unit;
  }

  private static CompilationUnit buildParserTestLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "parser_test"));
    unit.getDirectives().add(importDirective("dart:collection", null));
    unit.getDirectives().add(importDirective(src_package + "java_core.dart", null));
    unit.getDirectives().add(importDirective(src_package + "java_engine.dart", null));
    unit.getDirectives().add(importDirective(src_package + "java_junit.dart", null));
    unit.getDirectives().add(importDirective(src_package + "source.dart", null));
    unit.getDirectives().add(importDirective(src_package + "error.dart", null));
    unit.getDirectives().add(importDirective(src_package + "scanner.dart", null));
    unit.getDirectives().add(importDirective(src_package + "ast.dart", null));
    unit.getDirectives().add(importDirective(src_package + "parser.dart", null));
    unit.getDirectives().add(importDirective(src_package + "utilities_dart.dart", null));
    unit.getDirectives().add(importDirective("package:unittest/unittest.dart", "_ut"));
    unit.getDirectives().add(importDirective("test_support.dart", null));
    unit.getDirectives().add(
        importDirective("scanner_test.dart", null, importShowCombinator("TokenFactory")));
    List<Statement> mainStatements = Lists.newArrayList();
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isEngineTestPath(file, "parser/")) {
        List<CompilationUnitMember> unitMembers = entry.getValue();
        for (CompilationUnitMember unitMember : unitMembers) {
          boolean isTestSuite = EngineSemanticProcessor.gatherTestSuites(mainStatements, unitMember);
          if (!isTestSuite) {
            unit.getDeclarations().add(unitMember);
          }
        }
      }
    }
    EngineSemanticProcessor.addMain(unit, mainStatements);
    return unit;
  }

  private static CompilationUnit buildResolverLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "resolver"));
    unit.getDirectives().add(importDirective("dart:collection", null));
    unit.getDirectives().add(importDirective("java_core.dart", null));
    unit.getDirectives().add(importDirective("java_engine.dart", null));
    unit.getDirectives().add(importDirective("instrumentation.dart", null));
    unit.getDirectives().add(importDirective("source.dart", null));
    unit.getDirectives().add(importDirective("error.dart", null));
    unit.getDirectives().add(importDirective("scanner.dart", "sc"));
    unit.getDirectives().add(importDirective("utilities_dart.dart", null));
    unit.getDirectives().add(importDirective("ast.dart", null));
    unit.getDirectives().add(
        importDirective("parser.dart", null, importShowCombinator("Parser", "ParserErrorCode")));
    unit.getDirectives().add(importDirective("sdk.dart", null, importShowCombinator("DartSdk")));
    unit.getDirectives().add(
        importDirective(
            "element.dart",
            null,
            importHideCombinator("HideCombinator", "ShowCombinator")));
    unit.getDirectives().add(importDirective("html.dart", "ht"));
    unit.getDirectives().add(importDirective("engine.dart", null));
    unit.getDirectives().add(importDirective("constant.dart", null));
    for (CompilationUnitMember member : dartUnit.getDeclarations()) {
      File file = context.getMemberToFile().get(member);
      if (isEnginePath(file, "resolver/") || isEnginePath(file, "internal/resolver/")
          || isEnginePath(file, "internal/builder/") || isEnginePath(file, "internal/scope/")
          || isEnginePath(file, "internal/verifier/")) {
        unit.getDeclarations().add(member);
      }
    }
    // there is conflict between Hide/ShowCombinator classes in AST and Element, so tweak them
    unit.getDirectives().add(
        importDirective(
            "element.dart",
            "__imp_combi",
            importShowCombinator("HideCombinator", "ShowCombinator")));
    unit.accept(new RecursiveASTVisitor<Void>() {
      @Override
      public Void visitTypeName(TypeName node) {
        ITypeBinding binding = context.getNodeTypeBinding(node);
        if (binding != null) {
          String shortName = binding.getName();
          shortName = StringUtils.substringBefore(shortName, "<");
          if (JavaUtils.isTypeNamed(binding, "com.google.dart.engine.element.HideCombinator")
              || JavaUtils.isTypeNamed(binding, "com.google.dart.engine.element.ShowCombinator")) {
            ((SimpleIdentifier) node.getName()).setToken(token("__imp_combi." + shortName));
          }
        }
        return super.visitTypeName(node);
      }
    });
    EngineSemanticProcessor.useImportPrefix(
        context,
        unit,
        "sc",
        new String[] {"com.google.dart.engine.scanner."});
    EngineSemanticProcessor.useImportPrefix(
        context,
        unit,
        "ht",
        new String[] {"com.google.dart.engine.html."});
    // done
    return unit;
  }

  private static CompilationUnit buildResolverTestLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "resolver_test"));
    unit.getDirectives().add(importDirective("dart:collection", null));
    unit.getDirectives().add(importDirective(src_package + "java_core.dart", null));
    unit.getDirectives().add(importDirective(src_package + "java_engine.dart", null));
    unit.getDirectives().add(importDirective(src_package + "java_junit.dart", null));
    unit.getDirectives().add(importDirective(src_package + "source_io.dart", null));
    unit.getDirectives().add(importDirective(src_package + "error.dart", null));
    unit.getDirectives().add(importDirective(src_package + "scanner.dart", null));
    unit.getDirectives().add(
        importDirective(src_package + "ast.dart", null, importHideCombinator("Annotation")));
    unit.getDirectives().add(
        importDirective(src_package + "parser.dart", null, importShowCombinator("ParserErrorCode")));
    unit.getDirectives().add(importDirective(src_package + "element.dart", null));
    unit.getDirectives().add(importDirective(src_package + "resolver.dart", null));
    unit.getDirectives().add(importDirective(src_package + "engine.dart", null));
    unit.getDirectives().add(importDirective(src_package + "java_engine_io.dart", null));
    unit.getDirectives().add(
        importDirective(src_package + "sdk.dart", null, importShowCombinator("DartSdk")));
    unit.getDirectives().add(
        importDirective(
            src_package + "sdk_io.dart",
            null,
            importShowCombinator("DirectoryBasedDartSdk")));
    unit.getDirectives().add(importDirective("package:unittest/unittest.dart", "_ut"));
    unit.getDirectives().add(importDirective("test_support.dart", null));
    unit.getDirectives().add(
        importDirective("ast_test.dart", null, importShowCombinator("ASTFactory")));
    unit.getDirectives().add(
        importDirective("element_test.dart", null, importShowCombinator("ElementFactory")));
    List<Statement> mainStatements = Lists.newArrayList();
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isEngineTestPath(file, "context/") || isEngineTestPath(file, "resolver/")
          || isEngineTestPath(file, "internal/resolver/")
          || isEngineTestPath(file, "internal/scope/")) {
        List<CompilationUnitMember> unitMembers = entry.getValue();
        for (CompilationUnitMember unitMember : unitMembers) {
          boolean isTestSuite = EngineSemanticProcessor.gatherTestSuites(mainStatements, unitMember);
          if (!isTestSuite) {
            unit.getDeclarations().add(unitMember);
          }
        }
      }
    }
    EngineSemanticProcessor.addMain(unit, mainStatements);
    return unit;
  }

  private static CompilationUnit buildScannerLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "scanner"));
    unit.getDirectives().add(importDirective("dart:collection", null));
    unit.getDirectives().add(importDirective("java_core.dart", null));
    unit.getDirectives().add(importDirective("java_engine.dart", null));
    unit.getDirectives().add(importDirective("source.dart", null));
    unit.getDirectives().add(importDirective("error.dart", null));
    unit.getDirectives().add(importDirective("instrumentation.dart", null));
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isEnginePath(file, "scanner/")) {
        addNotRemovedCompiationUnitEntries(unit, entry.getValue());
      }
    }
    return unit;
  }

  private static CompilationUnit buildScannerTestLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "scanner_test"));
    unit.getDirectives().add(importDirective("dart:collection", null));
    unit.getDirectives().add(importDirective(src_package + "java_core.dart", null));
    unit.getDirectives().add(importDirective(src_package + "java_engine.dart", null));
    unit.getDirectives().add(importDirective(src_package + "java_junit.dart", null));
    unit.getDirectives().add(importDirective(src_package + "source.dart", null));
    unit.getDirectives().add(importDirective(src_package + "error.dart", null));
    unit.getDirectives().add(importDirective(src_package + "scanner.dart", null));
    unit.getDirectives().add(importDirective("package:unittest/unittest.dart", "_ut"));
    unit.getDirectives().add(importDirective("test_support.dart", null));
    List<Statement> mainStatements = Lists.newArrayList();
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isEngineTestPath(file, "scanner/")) {
        List<CompilationUnitMember> unitMembers = entry.getValue();
        for (CompilationUnitMember unitMember : unitMembers) {
          boolean isTestSuite = EngineSemanticProcessor.gatherTestSuites(mainStatements, unitMember);
          if (!isTestSuite) {
            unit.getDeclarations().add(unitMember);
          }
        }
      }
    }
    EngineSemanticProcessor.addMain(unit, mainStatements);
    return unit;
  }

  private static CompilationUnit buildSdkIoLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "sdk"));
    unit.getDirectives().add(importDirective("dart:io", null));
    unit.getDirectives().add(importDirective("dart:uri", null));
    unit.getDirectives().add(importDirective("java_core.dart", null));
    unit.getDirectives().add(importDirective("java_io.dart", null));
    unit.getDirectives().add(importDirective("java_engine.dart", null));
    unit.getDirectives().add(importDirective("java_engine_io.dart", null));
    unit.getDirectives().add(importDirective("source_io.dart", null));
    unit.getDirectives().add(importDirective("error.dart", null));
    unit.getDirectives().add(importDirective("scanner.dart", null));
    unit.getDirectives().add(importDirective("ast.dart", null));
    unit.getDirectives().add(importDirective("parser.dart", null));
    unit.getDirectives().add(importDirective("sdk.dart", null));
    unit.getDirectives().add(importDirective("engine.dart", null));
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      // non-IO part
      if (isEnginePath(file, "sdk/DartSdk") || isEnginePath(file, "sdk/SdkLibrary")
          || isEnginePath(file, "internal/sdk/SdkLibraryImpl")
          || isEnginePath(file, "internal/sdk/LibraryMap")) {
        continue;
      }
      // IO part
      if (isEnginePath(file, "sdk/") || isEnginePath(file, "internal/sdk/")) {
        addNotRemovedCompiationUnitEntries(unit, entry.getValue());
      }
    }
    return unit;
  }

  private static CompilationUnit buildSdkLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "sdk"));
    unit.getDirectives().add(importDirective("dart:uri", null));
    unit.getDirectives().add(importDirective("java_core.dart", null));
    unit.getDirectives().add(importDirective("java_engine.dart", null));
    unit.getDirectives().add(
        importDirective("source.dart", null, importShowCombinator("Source", "ContentCache")));
    unit.getDirectives().add(
        importDirective("engine.dart", null, importShowCombinator("AnalysisContext")));
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isEnginePath(file, "sdk/DartSdk") || isEnginePath(file, "sdk/SdkLibrary")
          || isEnginePath(file, "internal/sdk/SdkLibraryImpl")
          || isEnginePath(file, "internal/sdk/LibraryMap")) {
        addNotRemovedCompiationUnitEntries(unit, entry.getValue());
      }
    }
    return unit;
  }

  private static CompilationUnit buildSourceIoLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "source", "io"));
    unit.getDirectives().add(importDirective("source.dart", null));
    unit.getDirectives().add(importDirective("dart:io", null));
    unit.getDirectives().add(importDirective("dart:uri", null));
    unit.getDirectives().add(importDirective("java_core.dart", null));
    unit.getDirectives().add(importDirective("java_io.dart", null));
    unit.getDirectives().add(importDirective("sdk.dart", null, importShowCombinator("DartSdk")));
    unit.getDirectives().add(
        importDirective(
            "engine.dart",
            null,
            importShowCombinator("AnalysisContext", "AnalysisEngine")));
    unit.getDirectives().add(exportDirective("source.dart"));
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isEnginePath(file, "source/Source.java")
          || isEnginePath(file, "source/ContentCache.java")
          || isEnginePath(file, "source/DartUriResolver.java")
          || isEnginePath(file, "source/SourceFactory.java")
          || isEnginePath(file, "source/SourceContainer.java")
          || isEnginePath(file, "source/SourceKind.java")
          || isEnginePath(file, "source/UriResolver.java")
          || isEnginePath(file, "utilities/source/")) {
        continue;
      }
      if (isEnginePath(file, "source/")) {
        addNotRemovedCompiationUnitEntries(unit, entry.getValue());
      }
    }
    return unit;
  }

  private static CompilationUnit buildSourceLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "source"));
    unit.getDirectives().add(importDirective("dart:uri", null));
    unit.getDirectives().add(importDirective("java_core.dart", null));
    unit.getDirectives().add(importDirective("sdk.dart", null, importShowCombinator("DartSdk")));
    unit.getDirectives().add(
        importDirective("engine.dart", null, importShowCombinator("AnalysisContext")));
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isEnginePath(file, "source/Source.java")
          || isEnginePath(file, "source/ContentCache.java")
          || isEnginePath(file, "source/DartUriResolver.java")
          || isEnginePath(file, "source/SourceFactory.java")
          || isEnginePath(file, "source/SourceContainer.java")
          || isEnginePath(file, "source/SourceKind.java")
          || isEnginePath(file, "source/UriResolver.java")
          || isEnginePath(file, "utilities/source/")) {
        addNotRemovedCompiationUnitEntries(unit, entry.getValue());
      }
    }
    return unit;
  }

  private static CompilationUnit buildTestSupportLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "test_support"));
    unit.getDirectives().add(importDirective("dart:collection", null));
    unit.getDirectives().add(importDirective("dart:uri", null));
    unit.getDirectives().add(importDirective(src_package + "java_core.dart", null));
    unit.getDirectives().add(importDirective(src_package + "java_engine.dart", null));
    unit.getDirectives().add(importDirective(src_package + "java_junit.dart", null));
    unit.getDirectives().add(importDirective(src_package + "source.dart", null));
    unit.getDirectives().add(importDirective(src_package + "error.dart", null));
    unit.getDirectives().add(importDirective(src_package + "scanner.dart", null));
    unit.getDirectives().add(
        importDirective(
            src_package + "element.dart",
            null,
            importShowCombinator("InterfaceType", "MethodElement", "PropertyAccessorElement")));
    unit.getDirectives().add(
        importDirective(
            src_package + "engine.dart",
            null,
            importShowCombinator("AnalysisContext", "AnalysisContextImpl")));
    unit.getDirectives().add(importDirective("package:unittest/unittest.dart", "_ut"));
    List<Statement> mainStatements = Lists.newArrayList();
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isEngineTestPath(file, "error/") || isEngineTestPath(file, "EngineTestCase.java")) {
        addNotRemovedCompiationUnitEntries(unit, entry.getValue());
      }
    }
    EngineSemanticProcessor.addMain(unit, mainStatements);
    return unit;
  }

  private static CompilationUnit buildUtilitiesDartLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "utilities", "dart"));
    for (CompilationUnitMember member : dartUnit.getDeclarations()) {
      File file = context.getMemberToFile().get(member);
      if (isEnginePath(file, "utilities/dart/ParameterKind")) {
        unit.getDeclarations().add(member);
      }
    }
    return unit;
  }

  /**
   * @return the formatted Dart source dump of the given {@link ASTNode}.
   */
  private static String getFormattedSource(ASTNode node) {
    PrintStringWriter writer = new PrintStringWriter();
    writer.append(HEADER);
    node.accept(new ToFormattedSourceVisitor(writer));
    return writer.toString();
  }

  /**
   * @param enginePackage the sub-package in <code>com/google/dart/engine</code>.
   * @return <code>true</code> if given {@link File} is located in sub-package of Engine project.
   */
  private static boolean isEnginePath(File file, String enginePackage) {
    return file.getAbsolutePath().startsWith(
        engineFolder.getAbsolutePath() + "/com/google/dart/engine/" + enginePackage);
  }

  /**
   * @param enginePackage the sub-package in <code>com/google/dart/engine</code>.
   * @return <code>true</code> if given {@link File} is located in sub-package of Engine project.
   */
  private static boolean isEngineTestPath(File file, String enginePackage) {
    return file.getAbsolutePath().startsWith(
        engineTestFolder.getAbsolutePath() + "/com/google/dart/engine/" + enginePackage);
  }
}
