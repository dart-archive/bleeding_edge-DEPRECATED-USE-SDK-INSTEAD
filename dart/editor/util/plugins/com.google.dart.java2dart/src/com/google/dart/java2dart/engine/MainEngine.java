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
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.java2dart.Context;
import com.google.dart.java2dart.processor.BeautifySemanticProcessor;
import com.google.dart.java2dart.processor.CollectionSemanticProcessor;
import com.google.dart.java2dart.processor.JUnitSemanticProcessor;
import com.google.dart.java2dart.processor.ObjectSemanticProcessor;
import com.google.dart.java2dart.processor.PropertySemanticProcessor;
import com.google.dart.java2dart.processor.SemanticProcessor;
import com.google.dart.java2dart.util.ASTFactory;
import com.google.dart.java2dart.util.ToFormattedSourceVisitor;

import static com.google.dart.java2dart.util.ASTFactory.identifier;
import static com.google.dart.java2dart.util.ASTFactory.importDirective;
import static com.google.dart.java2dart.util.ASTFactory.importHideCombinator;
import static com.google.dart.java2dart.util.ASTFactory.libraryDirective;

import java.io.File;
import java.util.List;
import java.util.Map.Entry;

/**
 * Translates some parts of "com.google.dart.engine" project.
 */
public class MainEngine {
  private static final Context context = new Context();
  private static File engineFolder;
  private static File engineTestFolder;
  private static File engineFolder2;
  private static CompilationUnit dartUnit;

  private static final List<SemanticProcessor> PROCESSORS = ImmutableList.of(
      ObjectSemanticProcessor.INSTANCE,
      CollectionSemanticProcessor.INSTANCE,
      PropertySemanticProcessor.INSTANCE,
      JUnitSemanticProcessor.INSTANCE,
      BeautifySemanticProcessor.INSTANCE,
      EngineSemanticProcessor.INSTANCE);

  private static final String HEADER = "// This code was auto-generated, is not intended to be edited, and is subject to\n"
      + "// significant change. Please see the README file for more information.\n\n";

  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      System.out.println("Usage: java2dart <target-src-folder> <target-test-folder>");
      System.exit(0);
    }
    String targetFolder = args[0];
    String targetTestFolder = args[1];
    System.out.println("Generating files into " + targetFolder);
    new File(targetFolder).mkdirs();
    //
    engineFolder = new File("../../../tools/plugins/com.google.dart.engine/src");
    engineTestFolder = new File("../../../tools/plugins/com.google.dart.engine_test/src");
    engineFolder2 = new File("src");
    engineFolder = engineFolder.getCanonicalFile();
    // configure Context
    context.addClasspathFile(new File("../../../../third_party/junit/v4_8_2/junit.jar"));
    context.addSourceFolder(engineFolder);
    context.addSourceFiles(new File(
        engineFolder,
        "com/google/dart/engine/utilities/instrumentation"));
    context.addSourceFile(new File(engineFolder, "com/google/dart/engine/source/Source.java"));
    context.addSourceFile(new File(
        engineFolder,
        "com/google/dart/engine/utilities/source/LineInfo.java"));
    context.addSourceFile(new File(
        engineFolder,
        "com/google/dart/engine/utilities/dart/ParameterKind.java"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/error"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/scanner"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/ast"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/parser"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/parser"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/element"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/type"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/element"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/type"));
    context.addSourceFile(new File(
        engineFolder2,
        "com/google/dart/java2dart/util/ToFormattedSourceVisitor.java"));
    context.addSourceFile(new File(engineTestFolder, "com/google/dart/engine/EngineTestCase.java"));
    context.addSourceFile(new File(
        engineTestFolder,
        "com/google/dart/engine/error/GatheringErrorListener.java"));
    context.addSourceFiles(new File(engineTestFolder, "com/google/dart/engine/scanner"));
    // translate into single CompilationUnit
    dartUnit = context.translate();
    // run processors
    for (SemanticProcessor processor : PROCESSORS) {
      processor.process(context, dartUnit);
    }
    // run this again, because we may introduce conflicts when convert methods to getters/setters
    context.ensureUniqueClassMemberNames(dartUnit);
    context.ensureNoVariableNameReferenceFromInitializer(dartUnit);
    // dump as several libraries
    Files.copy(new File("resources/java_core.dart"), new File(targetFolder + "/java_core.dart"));
    Files.copy(new File("resources/java_junit.dart"), new File(targetFolder + "/java_junit.dart"));
    Files.copy(new File("resources/java_engine.dart"), new File(targetFolder + "/java_engine.dart"));
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
      CompilationUnit library = buildElementLibrary();
      Files.write(
          getFormattedSource(library),
          new File(targetFolder + "/element.dart"),
          Charsets.UTF_8);
    }
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
    System.out.println("Translation complete");
  }

  private static CompilationUnit buildAstLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "ast"));
    unit.getDirectives().add(importDirective("dart:collection", null));
    unit.getDirectives().add(importDirective("java_core.dart", null));
    unit.getDirectives().add(importDirective("java_engine.dart", null));
    unit.getDirectives().add(importDirective("error.dart", null));
    unit.getDirectives().add(importDirective("scanner.dart", null));
    unit.getDirectives().add(
        importDirective("element.dart", null, importHideCombinator(identifier("Annotation"))));
    for (CompilationUnitMember member : dartUnit.getDeclarations()) {
      File file = context.getMemberToFile().get(member);
      if (isEnginePath(file, "ast/")) {
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
    unit.getDirectives().add(importDirective("ast.dart", null));
    for (CompilationUnitMember member : dartUnit.getDeclarations()) {
      File file = context.getMemberToFile().get(member);
      if (isEnginePath(file, "internal/element/handle/")) {
        continue;
      }
      if (isEnginePath(file, "element/") || isEnginePath(file, "type/")
          || isEnginePath(file, "utilities/dart/ParameterKind")
          || isEnginePath(file, "internal/element/") || isEnginePath(file, "internal/type/")) {
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
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isEnginePath(file, "error/")) {
        unit.getDeclarations().addAll(entry.getValue());
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
        unit.getDeclarations().addAll(entry.getValue());
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
    unit.getDirectives().add(importDirective("error.dart", null));
    unit.getDirectives().add(importDirective("source.dart", null));
    unit.getDirectives().add(importDirective("scanner.dart", null));
    unit.getDirectives().add(importDirective("ast.dart", null));
    unit.getDirectives().add(
        importDirective(
            "element.dart",
            null,
            ASTFactory.importShowCombinator(identifier("ParameterKind"))));
    for (CompilationUnitMember member : dartUnit.getDeclarations()) {
      File file = context.getMemberToFile().get(member);
      if (isEnginePath(file, "parser/") || isEnginePath(file, "internal/parser/")
          || file.getName().equals("ToFormattedSourceVisitor.java")) {
        unit.getDeclarations().add(member);
      }
    }
    return unit;
  }

  private static CompilationUnit buildScannerLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "scanner"));
    unit.getDirectives().add(importDirective("dart:collection", null));
    unit.getDirectives().add(importDirective("java_core.dart", null));
    unit.getDirectives().add(importDirective("source.dart", null));
    unit.getDirectives().add(importDirective("error.dart", null));
    unit.getDirectives().add(importDirective("instrumentation.dart", null));
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isEnginePath(file, "scanner/")) {
        unit.getDeclarations().addAll(entry.getValue());
      }
    }
    return unit;
  }

  private static CompilationUnit buildScannerTestLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "scanner_test"));
    unit.getDirectives().add(importDirective("dart:collection", null));
    unit.getDirectives().add(importDirective("package:analysis_engine/src/java_core.dart", null));
    unit.getDirectives().add(importDirective("package:analysis_engine/src/java_engine.dart", null));
    unit.getDirectives().add(importDirective("package:analysis_engine/src/java_junit.dart", null));
    unit.getDirectives().add(importDirective("package:analysis_engine/src/source.dart", null));
    unit.getDirectives().add(importDirective("package:analysis_engine/src/error.dart", null));
    unit.getDirectives().add(importDirective("package:analysis_engine/src/scanner.dart", null));
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

  private static CompilationUnit buildSourceLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "source"));
    unit.getDirectives().add(importDirective("java_core.dart", null));
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isEnginePath(file, "source/") || isEnginePath(file, "utilities/source/LineInfo.java")) {
        unit.getDeclarations().addAll(entry.getValue());
      }
    }
    return unit;
  }

  // XXX
  private static CompilationUnit buildTestSupportLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "scanner_test"));
    unit.getDirectives().add(importDirective("dart:collection", null));
    unit.getDirectives().add(importDirective("package:analysis_engine/src/java_core.dart", null));
    unit.getDirectives().add(importDirective("package:analysis_engine/src/java_engine.dart", null));
    unit.getDirectives().add(importDirective("package:analysis_engine/src/java_junit.dart", null));
    unit.getDirectives().add(importDirective("package:analysis_engine/src/source.dart", null));
    unit.getDirectives().add(importDirective("package:analysis_engine/src/error.dart", null));
    unit.getDirectives().add(importDirective("package:unittest/unittest.dart", "_ut"));
    List<Statement> mainStatements = Lists.newArrayList();
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isEngineTestPath(file, "error/")) {
        unit.getDeclarations().addAll(entry.getValue());
      }
    }
    EngineSemanticProcessor.addMain(unit, mainStatements);
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
