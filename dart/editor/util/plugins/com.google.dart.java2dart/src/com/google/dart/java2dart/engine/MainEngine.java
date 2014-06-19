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
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.ast.AsExpression;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.ParenthesizedExpression;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.TryStatement;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisErrorInfo;
import com.google.dart.engine.context.AnalysisResult;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.HintCode;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.PackageUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.java2dart.Context;
import com.google.dart.java2dart.processor.BeautifySemanticProcessor;
import com.google.dart.java2dart.processor.CollectionSemanticProcessor;
import com.google.dart.java2dart.processor.GuavaSemanticProcessor;
import com.google.dart.java2dart.processor.IOSemanticProcessor;
import com.google.dart.java2dart.processor.JUnitSemanticProcessor;
import com.google.dart.java2dart.processor.ObjectSemanticProcessor;
import com.google.dart.java2dart.processor.PropertySemanticProcessor;
import com.google.dart.java2dart.processor.RenameConstructorsSemanticProcessor;
import com.google.dart.java2dart.processor.SemanticProcessor;
import com.google.dart.java2dart.processor.TypeSemanticProcessor;
import com.google.dart.java2dart.processor.UniqueMemberNamesSemanticProcessor;
import com.google.dart.java2dart.util.ToFormattedSourceVisitor;

import static com.google.dart.java2dart.util.AstFactory.exportDirective;
import static com.google.dart.java2dart.util.AstFactory.importDirective;
import static com.google.dart.java2dart.util.AstFactory.importShowCombinator;
import static com.google.dart.java2dart.util.AstFactory.instanceCreationExpression;
import static com.google.dart.java2dart.util.AstFactory.libraryDirective;
import static com.google.dart.java2dart.util.AstFactory.typeName;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Translates some parts of "com.google.dart.engine" project.
 */
public class MainEngine {
  static class Edit {
    public final int offset;
    public final int length;
    public final String replacement;

    public Edit(int offset, int length, String replacement) {
      this.offset = offset;
      this.length = length;
      this.replacement = replacement;
    }

    @Override
    public String toString() {
      return (offset < 0 ? "(" : "X(") + "offset: " + offset + ", length " + length
          + ", replacement :>" + replacement + "<:)";
    }

  }

  /**
   * Default package src location (can be overridden)
   */
  private static String src_package = "package:analysis_engine/src/";
  private static final Context context = new Context();
  private static File engineFolder;
  private static File engineTestFolder;

  private static CompilationUnit dartUnit;

  private static final String HEADER = "// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file\n"
      + "// for details. All rights reserved. Use of this source code is governed by a\n"
      + "// BSD-style license that can be found in the LICENSE file.\n"
      + "\n"
      + "// This code was auto-generated, is not intended to be edited, and is subject to\n"
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
    engineFolder = engineFolder.getCanonicalFile();
    // configure Context
    context.addClasspathFile(new File("../../../../third_party/guava/r13/guava-13.0.1.jar"));
    context.addClasspathFile(new File("../../../../third_party/junit/v4_8_2/junit.jar"));
    context.addSourceFolder(engineFolder);
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/utilities/ast"));
    // instrumentation
    context.addSourceFile(new File(
        engineFolder,
        "com/google/dart/engine/utilities/instrumentation/Instrumentation.java"));
    context.addSourceFile(new File(
        engineFolder,
        "com/google/dart/engine/utilities/instrumentation/InstrumentationBuilder.java"));
    context.addSourceFile(new File(
        engineFolder,
        "com/google/dart/engine/utilities/instrumentation/InstrumentationLevel.java"));
    context.addSourceFile(new File(
        engineFolder,
        "com/google/dart/engine/utilities/instrumentation/InstrumentationLogger.java"));
    // other
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
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/index"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/parser"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/resolver"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/scanner"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/type"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/builder"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/cache"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/constant"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/element"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/error"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/hint"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/html/angular"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/html/polymer"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/index"));
    context.removeSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/index/file"));
    context.removeSourceFiles(new File(
        engineFolder,
        "com/google/dart/engine/internal/index/structure/btree"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/object"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/parser"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/resolver"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/scope"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/task"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/type"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/verifier"));
    context.addSourceFile(new File(engineFolder, "com/google/dart/engine/AnalysisEngine.java"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/utilities/logging"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/context"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/context"));
    // utilities/general
    context.addSourceFile(new File(
        engineFolder,
        "com/google/dart/engine/utilities/general/TimeCounter.java"));
    // utilities/collection
    context.addSourceFile(new File(
        engineFolder,
        "com/google/dart/engine/utilities/collection/BooleanArray.java"));
    context.addSourceFile(new File(
        engineFolder,
        "com/google/dart/engine/utilities/collection/DirectedGraph.java"));
    context.addSourceFile(new File(
        engineFolder,
        "com/google/dart/engine/utilities/collection/ListUtilities.java"));
    context.addSourceFile(new File(
        engineFolder,
        "com/google/dart/engine/utilities/collection/MapIterator.java"));
    context.addSourceFile(new File(
        engineFolder,
        "com/google/dart/engine/utilities/collection/MultipleMapIterator.java"));
    context.addSourceFile(new File(
        engineFolder,
        "com/google/dart/engine/utilities/collection/SingleMapIterator.java"));
    context.addSourceFile(new File(
        engineFolder,
        "com/google/dart/engine/utilities/collection/TokenMap.java"));
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
    context.addSourceFiles(new File(engineTestFolder, "com/google/dart/engine/source"));
    context.addSourceFiles(new File(engineTestFolder, "com/google/dart/engine/internal/context"));
    // configure properties
    context.addNotProperty("Lcom/google/dart/engine/parser/Parser;.isFunctionDeclaration()");
    context.addNotProperty("Lcom/google/dart/engine/parser/Parser;.isInitializedVariableDeclaration()");
    context.addNotProperty("Lcom/google/dart/engine/parser/Parser;.isSwitchMember()");
    // translate into single CompilationUnit
    dartUnit = context.translate();
    // run processors
    {
      List<SemanticProcessor> PROCESSORS = ImmutableList.of(
          new TypeSemanticProcessor(context),
          new ObjectSemanticProcessor(context),
          new CollectionSemanticProcessor(context),
          new IOSemanticProcessor(context),
          new PropertySemanticProcessor(context),
          new GuavaSemanticProcessor(context),
          new JUnitSemanticProcessor(context),
          new EngineAnnotationProcessor(context),
          new EngineExceptionProcessor(context),
          new UniqueMemberNamesSemanticProcessor(context),
          new RenameConstructorsSemanticProcessor(context),
          new EngineSemanticProcessor(context),
          new EngineInstanceOfProcessor(context),
          new BeautifySemanticProcessor(context));
      for (SemanticProcessor processor : PROCESSORS) {
        processor.process(dartUnit);
      }
    }
    // run this again, because we may introduce conflicts when convert methods to getters/setters
    context.ensureUniqueClassMemberNames();
    context.applyLocalVariableSemanticChanges(dartUnit);
    EngineSemanticProcessor.rewriteReflectionFieldsWithDirect(context, dartUnit);
    // dump as several libraries
    Files.copy(new File("resources/interner.dart"), new File(targetFolder + "/interner.dart"));
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
      CompilationUnit library = buildUtilitiesCollectionLibrary();
      File file = new File(targetFolder + "/utilities_collection.dart");
      Files.write(getFormattedSource(library), file, Charsets.UTF_8);
      Files.append(
          Files.toString(new File("resources/utilities_collection_include.dart"), Charsets.UTF_8),
          file,
          Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildUtilitiesGeneralLibrary();
      removeClass(library, "TimeCounter");
      removeClass(library, "TimeCounter_TimeCounterHandle");
      String source = getFormattedSource(library);
      source += makeSource(
          "/**",
          " * Helper for measuring how much time is spent doing some operation.",
          " */",
          "class TimeCounter {",
          "  static final int NANOS_PER_MILLI = 1000 * 1000;",
          "  static final int NANOS_PER_MICRO = 1000;",
          "  static TimeCounter _current = null;",
          "  final Stopwatch _sw = new Stopwatch();",
          "",
          "  /**",
          "   * @return the number of milliseconds spent between [start] and [stop].",
          "   */",
          "  int get result => _sw.elapsedMilliseconds;",
          "",
          "  /**",
          "   * Starts counting time.",
          "   *",
          "   * @return the [TimeCounterHandle] that should be used to stop counting.",
          "   */",
          "  TimeCounter_TimeCounterHandle start() {",
          "    return new TimeCounter_TimeCounterHandle(this);",
          "  }",
          "}",
          "",
          "/**",
          " * The handle object that should be used to stop and update counter.",
          " */",
          "class TimeCounter_TimeCounterHandle {",
          "  final TimeCounter _counter;",
          "  int _startMicros;",
          "  TimeCounter _prev;",
          "",
          "  TimeCounter_TimeCounterHandle(this._counter) {",
          "    // if there is some counter running, pause it",
          "    _prev = TimeCounter._current;",
          "    if (_prev != null) {",
          "      _prev._sw.stop();",
          "    }",
          "    TimeCounter._current = _counter;",
          "    // start this counter",
          "    _startMicros = _counter._sw.elapsedMicroseconds;",
          "    _counter._sw.start();",
          "  }",
          "",
          "  /**",
          "   * Stops counting time and updates counter.",
          "   */",
          "  int stop() {",
          "    _counter._sw.stop();",
          "    int elapsed = (_counter._sw.elapsedMicroseconds - _startMicros) *",
          "        TimeCounter.NANOS_PER_MICRO;",
          "    // restore previous counter and resume it",
          "    TimeCounter._current = _prev;",
          "    if (_prev != null) {",
          "      _prev._sw.start();",
          "    }",
          "    // done",
          "    return elapsed;",
          "  }",
          "}",
          "");
      Files.write(source, new File(targetFolder + "/utilities_general.dart"), Charsets.UTF_8);
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
      String source = getFormattedSource(library);
      Files.write(source, new File(targetFolder + "/html.dart"), Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildUtilitiesDartLibrary();
      File astFile = new File(targetFolder + "/utilities_dart.dart");
      Files.write(getFormattedSource(library), astFile, Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildAstLibrary();
      File astFile = new File(targetFolder + "/ast.dart");
      String source = getFormattedSource(library);
      source = source.replace("AngularCompilationUnitBuilder.getElement(node, offset);", "null;");
      source = source.replace("PolymerCompilationUnitBuilder.getElement(node, offset);", "null;");
      Files.write(source, astFile, Charsets.UTF_8);
      Files.append(
          Files.toString(new File("resources/ast_include.dart"), Charsets.UTF_8),
          astFile,
          Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildParserLibrary();
      // generate "methodTable_Parser"
      StringWriter methodWriter = new StringWriter();
      EngineSemanticProcessor.replaceReflection_generateParserTable(context, new PrintWriter(
          methodWriter), dartUnit);
      // write
      File libraryFile = new File(targetFolder + "/parser.dart");
      Files.write(getFormattedSource(library), libraryFile, Charsets.UTF_8);
      Files.append(methodWriter.toString(), libraryFile, Charsets.UTF_8);
      Files.append(
          Files.toString(new File("resources/parser_include.dart"), Charsets.UTF_8),
          libraryFile,
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
      String source = getFormattedSource(library);
      Files.write(source, new File(targetFolder + "/constant.dart"), Charsets.UTF_8);
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
      String source = getFormattedSource(library);
      source = replaceSourceFragmentRE(
          source,
          "Object visitImportDirective\\(ImportDirective directive\\) \\{\n.*?checkForPackageImportContainsDotDot.*?return null;",
          makeSource("Object visitImportDirective(ImportDirective directive) {", "    return null;"));
      source = replaceSourceFragment(
          source,
          "fullName.replaceAll(new String.fromCharCode(JavaFile.separatorChar), '/')",
          "fullName.replaceAll(r'\\', '/')");
      Files.write(source, new File(targetFolder + "/resolver.dart"), Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildEngineLibrary();
      String source = getFormattedSource(library);
      // TODO(scheglov) restore NgRepeatProcessor
      source = replaceSourceFragment(
          source,
          "_processors.add(NgRepeatProcessor.INSTANCE);",
          "// _processors.add(NgRepeatProcessor.INSTANCE);");
      Files.write(source, new File(targetFolder + "/engine.dart"), Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildIndexLibrary();
      Files.write(
          getFormattedSource(library),
          new File(targetFolder + "/index.dart"),
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
      String source = getFormattedSource(library);
      source = replaceSourceFragment(source, "${OSUtilities.LINE_SEPARATOR}", "\\n");
      Files.write(source, new File(targetTestFolder + "/scanner_test.dart"), Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildParserTestLibrary();
      EngineSemanticProcessor.replaceReflection_invokeParserMethodImpl(library);
      File libraryFile = new File(targetTestFolder + "/parser_test.dart");
      Files.write(getFormattedSource(library), libraryFile, Charsets.UTF_8);
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
      String source = getFormattedSource(library);
      // TODO(scheglov) restore this test once TestSource is not file based
      source = replaceSourceFragment(
          source,
          "AnalysisDeltaTest.dartSuite();",
          "//AnalysisDeltaTest.dartSuite();");
      Files.write(source, new File(targetTestFolder + "/resolver_test.dart"), Charsets.UTF_8);
    }
    {
      String projectFolder = new File(targetFolder).getParentFile().getParentFile().getParent();
      fixUnnecessaryCastHints(projectFolder);
    }
    System.out.println("Translation complete");
  }

  private static void addNotRemovedCompiationUnitEntries(CompilationUnit targetUnit,
      List<CompilationUnitMember> members) {
    for (CompilationUnitMember member : members) {
      // may be removed
      if (isRemoved(member)) {
        continue;
      }
      // OK, add this member
      targetUnit.getDeclarations().add(member);
    }
  }

  private static void applyEdits(File file, List<Edit> edits) throws IOException {
    // sort in descending order
    Collections.sort(edits, new Comparator<Edit>() {
      @Override
      public int compare(Edit o1, Edit o2) {
        return o2.offset - o1.offset;
      }
    });
    // apply to file
    String content = Files.toString(file, Charsets.UTF_8);
    for (Edit edit : edits) {
      content = content.substring(0, edit.offset) + edit.replacement
          + content.substring(edit.offset + edit.length);
    }
    Files.write(content, file, Charsets.UTF_8);
  }

  private static CompilationUnit buildAstLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "ast"));
    unit.getDirectives().add(importDirective("dart:collection", null));
    unit.getDirectives().add(importDirective("java_core.dart", null));
    unit.getDirectives().add(importDirective("java_engine.dart", null));
    unit.getDirectives().add(
        importDirective("source.dart", null, importShowCombinator("LineInfo", "Source")));
    unit.getDirectives().add(importDirective("scanner.dart", null));
    unit.getDirectives().add(
        importDirective("engine.dart", null, importShowCombinator("AnalysisEngine")));
    unit.getDirectives().add(importDirective("utilities_dart.dart", null));
    unit.getDirectives().add(
        importDirective("utilities_collection.dart", null, importShowCombinator("TokenMap")));
    unit.getDirectives().add(importDirective("element.dart", null));
    unit.getDirectives().add(importDirective("constant.dart", null));
    for (CompilationUnitMember member : dartUnit.getDeclarations()) {
      File file = context.getMemberToFile().get(member);
      if (isEnginePath(file, "ast/") || isEnginePath(file, "utilities/ast/")) {
        unit.getDeclarations().add(member);
      }
    }
    return unit;
  }

  private static CompilationUnit buildAstTestLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "ast_test"));
    unit.getDirectives().add(importDirective(src_package + "java_core.dart", null));
    unit.getDirectives().add(importDirective(src_package + "java_engine.dart", null));
    unit.getDirectives().add(importDirective(src_package + "java_junit.dart", null));
    unit.getDirectives().add(
        importDirective(src_package + "java_engine.dart", null, importShowCombinator("Predicate")));
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
    unit.getDirectives().add(importDirective("dart:collection", null));
    unit.getDirectives().add(importDirective("java_core.dart", null));
    unit.getDirectives().add(
        importDirective("java_engine.dart", null, importShowCombinator("ObjectUtilities")));
    unit.getDirectives().add(importDirective("source.dart", null, importShowCombinator("Source")));
    unit.getDirectives().add(
        importDirective(
            "error.dart",
            null,
            importShowCombinator("AnalysisError", "ErrorCode", "CompileTimeErrorCode")));
    unit.getDirectives().add(
        importDirective("scanner.dart", null, importShowCombinator("Token", "TokenType")));
    unit.getDirectives().add(importDirective("ast.dart", null));
    unit.getDirectives().add(importDirective("element.dart", null));
    unit.getDirectives().add(
        importDirective("resolver.dart", null, importShowCombinator("TypeProvider")));
    unit.getDirectives().add(
        importDirective("engine.dart", null, importShowCombinator("AnalysisEngine")));
    unit.getDirectives().add(
        importDirective("utilities_dart.dart", null, importShowCombinator("ParameterKind")));
    unit.getDirectives().add(importDirective("utilities_collection.dart", null));
    for (CompilationUnitMember member : dartUnit.getDeclarations()) {
      File file = context.getMemberToFile().get(member);
      if (isEnginePath(file, "constant/") || isEnginePath(file, "internal/constant/")
          || isEnginePath(file, "internal/object/")) {
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
    unit.getDirectives().add(importDirective("utilities_collection.dart", null));
    unit.getDirectives().add(importDirective("source.dart", null));
    unit.getDirectives().add(importDirective("scanner.dart", null, importShowCombinator("Keyword")));
    unit.getDirectives().add(importDirective("ast.dart", null));
    unit.getDirectives().add(importDirective("sdk.dart", null, importShowCombinator("DartSdk")));
    unit.getDirectives().add(
        importDirective("html.dart", null, importShowCombinator("XmlAttributeNode", "XmlTagNode")));
    unit.getDirectives().add(
        importDirective(
            "engine.dart",
            null,
            importShowCombinator("AnalysisContext", "AnalysisEngine", "AnalysisException")));
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
    unit.getDirectives().add(importDirective(src_package + "java_core.dart", null));
    unit.getDirectives().add(importDirective(src_package + "java_engine_io.dart", null));
    unit.getDirectives().add(importDirective(src_package + "java_junit.dart", null));
    unit.getDirectives().add(importDirective(src_package + "source_io.dart", null));
    unit.getDirectives().add(importDirective(src_package + "utilities_dart.dart", null));
    unit.getDirectives().add(importDirective(src_package + "ast.dart", null));
    unit.getDirectives().add(importDirective(src_package + "element.dart", null));
    unit.getDirectives().add(
        importDirective(
            src_package + "engine.dart",
            null,
            importShowCombinator("AnalysisContext", "AnalysisContextImpl")));
    unit.getDirectives().add(importDirective("package:unittest/unittest.dart", "_ut"));
    unit.getDirectives().add(importDirective("test_support.dart", null));
    unit.getDirectives().add(
        importDirective("ast_test.dart", null, importShowCombinator("AstFactory")));
    unit.getDirectives().add(
        importDirective(
            "resolver_test.dart",
            null,
            importShowCombinator("TestTypeProvider", "AnalysisContextHelper")));
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
    unit.getDirectives().add(importDirective("dart:collection", null));
    unit.getDirectives().add(importDirective("java_core.dart", null));
    unit.getDirectives().add(importDirective("java_engine.dart", null));
    unit.getDirectives().add(importDirective("utilities_collection.dart", null));
    unit.getDirectives().add(importDirective("utilities_general.dart", null));
    unit.getDirectives().add(importDirective("instrumentation.dart", null));
    unit.getDirectives().add(importDirective("error.dart", null));
    unit.getDirectives().add(importDirective("source.dart", null));
    unit.getDirectives().add(importDirective("scanner.dart", null));
    unit.getDirectives().add(importDirective("ast.dart", null));
    unit.getDirectives().add(
        importDirective("parser.dart", null, importShowCombinator("Parser", "IncrementalParser")));
    unit.getDirectives().add(importDirective("sdk.dart", null, importShowCombinator("DartSdk")));
    unit.getDirectives().add(importDirective("constant.dart", null));
    unit.getDirectives().add(importDirective("element.dart", null));
    unit.getDirectives().add(importDirective("resolver.dart", null));
    unit.getDirectives().add(importDirective("html.dart", "ht"));
    for (CompilationUnitMember member : dartUnit.getDeclarations()) {
      File file = context.getMemberToFile().get(member);
      // ignore, part of index
      if (isEnginePath(file, "internal/html/angular/AngularDartIndexContributor.java")
          || isEnginePath(file, "internal/html/angular/AngularHtmlIndexContributor.java")) {
        continue;
      }
      // add
      if (isEnginePath(file, "AnalysisEngine.java") || isEnginePath(file, "utilities/logging/")
          || isEnginePath(file, "context/") || isEnginePath(file, "internal/cache/")
          || isEnginePath(file, "internal/context/") || isEnginePath(file, "internal/html/angular")
          || isEnginePath(file, "internal/html/polymer") || isEnginePath(file, "internal/task/")) {
        unit.getDeclarations().add(member);
      }
    }
    // TODO(scheglov) restore NgRepeatProcessor
    {
      removeClass(unit, "NgRepeatProcessor");
    }
    EngineSemanticProcessor.useImportPrefix(
        context,
        unit,
        "ht",
        new String[] {"com.google.dart.engine.html."},
        false);
    return unit;
  }

  private static CompilationUnit buildErrorLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "error"));
    unit.getDirectives().add(importDirective("dart:collection", null));
    unit.getDirectives().add(importDirective("java_core.dart", null));
    unit.getDirectives().add(importDirective("source.dart", null));
    unit.getDirectives().add(importDirective("scanner.dart", null, importShowCombinator("Token")));
    unit.getDirectives().add(importDirective("ast.dart", null, importShowCombinator("AstNode")));
    unit.getDirectives().add(importDirective("element.dart", null));
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
    unit.getDirectives().add(
        importDirective("error.dart", null, importShowCombinator("AnalysisErrorListener")));
    unit.getDirectives().add(
        importDirective(
            "scanner.dart",
            "sc",
            importShowCombinator("Scanner", "SubSequenceReader", "Token")));
    unit.getDirectives().add(importDirective("parser.dart", null, importShowCombinator("Parser")));
    unit.getDirectives().add(importDirective("ast.dart", null));
    unit.getDirectives().add(importDirective("element.dart", null));
    unit.getDirectives().add(
        importDirective(
            "engine.dart",
            null,
            importShowCombinator("AnalysisEngine", "AngularHtmlUnitResolver", "ExpressionVisitor")));
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isEnginePath(file, "html/scanner/") || isEnginePath(file, "html/ast/")
          || isEnginePath(file, "html/parser/")) {
        addNotRemovedCompiationUnitEntries(unit, entry.getValue());
      }
    }
    EngineSemanticProcessor.useImportPrefix(
        context,
        unit,
        "sc",
        new String[] {"com.google.dart.engine.scanner."},
        false);
    return unit;
  }

  private static CompilationUnit buildIndexLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "index"));
    unit.getDirectives().add(
        importDirective("dart:collection", null, importShowCombinator("Queue")));
    unit.getDirectives().add(importDirective("java_core.dart", null));
    unit.getDirectives().add(importDirective("java_engine.dart", null));
    unit.getDirectives().add(importDirective("source.dart", null));
    unit.getDirectives().add(importDirective("scanner.dart", null, importShowCombinator("Token")));
    unit.getDirectives().add(importDirective("ast.dart", null));
    unit.getDirectives().add(importDirective("element.dart", null));
    unit.getDirectives().add(
        importDirective(
            "resolver.dart",
            null,
            importShowCombinator("Namespace", "NamespaceBuilder")));
    unit.getDirectives().add(importDirective("engine.dart", null));
    unit.getDirectives().add(importDirective("html.dart", "ht"));
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (file.getName().contains("MemoryIndexReader")
          || file.getName().contains("MemoryIndexWriter")) {
        continue;
      }
      if (isEnginePath(file, "index/") || isEnginePath(file, "internal/index/")
          || isEnginePath(file, "internal/html/angular/ExpressionVisitor.java")
          || isEnginePath(file, "internal/html/angular/AngularDartIndexContributor.java")
          || isEnginePath(file, "internal/html/angular/AngularHtmlIndexContributor.java")) {
        addNotRemovedCompiationUnitEntries(unit, entry.getValue());
      }
    }
    EngineSemanticProcessor.useImportPrefix(
        context,
        unit,
        "ht",
        new String[] {"com.google.dart.engine.html."},
        false);
    unit.accept(new RecursiveAstVisitor<Void>() {
      @Override
      public Void visitCatchClause(CatchClause node) {
        TypeName exceptionType = node.getExceptionType();
        if (exceptionType != null
            && exceptionType.getName().getName().equals("InterruptedException")) {
          TryStatement tryStatement = (TryStatement) node.getParent();
          SemanticProcessor.replaceNode(tryStatement, tryStatement.getBody());
          return null;
        }
        return super.visitCatchClause(node);
      }

      @Override
      public Void visitFieldDeclaration(FieldDeclaration node) {
        VariableDeclarationList fields = node.getFields();
        for (VariableDeclaration field : fields.getVariables()) {
          if (field.getName().getName().equals("_removedContexts")) {
            fields.setType(typeName("Expando"));
            field.setInitializer(instanceCreationExpression(Keyword.NEW, typeName("Expando")));
          }
        }
        return null;
      }
    });
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
    unit.getDirectives().add(
        importDirective(
            "engine.dart",
            null,
            importShowCombinator("AnalysisEngine", "AnalysisOptionsImpl")));
    unit.getDirectives().add(
        importDirective("utilities_collection.dart", null, importShowCombinator("TokenMap")));
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
    unit.getDirectives().add(importDirective(src_package + "java_core.dart", null));
    unit.getDirectives().add(importDirective(src_package + "java_junit.dart", null));
    unit.getDirectives().add(importDirective(src_package + "error.dart", null));
    unit.getDirectives().add(
        importDirective(src_package + "source.dart", null, importShowCombinator("Source")));
    unit.getDirectives().add(importDirective(src_package + "scanner.dart", null));
    unit.getDirectives().add(importDirective(src_package + "ast.dart", null));
    unit.getDirectives().add(importDirective(src_package + "parser.dart", null));
    unit.getDirectives().add(importDirective(src_package + "element.dart", null));
    unit.getDirectives().add(importDirective(src_package + "utilities_dart.dart", null));
    unit.getDirectives().add(importDirective("package:unittest/unittest.dart", "_ut"));
    unit.getDirectives().add(importDirective("test_support.dart", null));
    unit.getDirectives().add(
        importDirective("scanner_test.dart", null, importShowCombinator("TokenFactory")));
    unit.getDirectives().add(
        importDirective("ast_test.dart", null, importShowCombinator("AstFactory")));
    unit.getDirectives().add(
        importDirective("element_test.dart", null, importShowCombinator("ElementFactory")));
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
    unit.getDirectives().add(importDirective("utilities_general.dart", null));
    unit.getDirectives().add(importDirective("ast.dart", null));
    unit.getDirectives().add(
        importDirective("parser.dart", null, importShowCombinator("Parser", "ParserErrorCode")));
    unit.getDirectives().add(
        importDirective("sdk.dart", null, importShowCombinator("DartSdk", "SdkLibrary")));
    unit.getDirectives().add(importDirective("element.dart", null));
    unit.getDirectives().add(importDirective("html.dart", "ht"));
    unit.getDirectives().add(importDirective("engine.dart", null));
    unit.getDirectives().add(importDirective("constant.dart", null));
    for (CompilationUnitMember member : dartUnit.getDeclarations()) {
      File file = context.getMemberToFile().get(member);
      if (isEnginePath(file, "resolver/") || isEnginePath(file, "internal/resolver/")
          || isEnginePath(file, "internal/builder/") || isEnginePath(file, "internal/hint/")
          || isEnginePath(file, "internal/scope/") || isEnginePath(file, "internal/verifier/")) {
        unit.getDeclarations().add(member);
      }
    }
    EngineSemanticProcessor.useImportPrefix(
        context,
        unit,
        "sc",
        new String[] {"com.google.dart.engine.scanner."},
        false);
    EngineSemanticProcessor.useImportPrefix(
        context,
        unit,
        "ht",
        new String[] {"com.google.dart.engine.html."},
        false);
    // done
    return unit;
  }

  private static CompilationUnit buildResolverTestLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "resolver_test"));
    unit.getDirectives().add(importDirective("dart:collection", null));
    unit.getDirectives().add(importDirective(src_package + "java_core.dart", null));
    unit.getDirectives().add(importDirective(src_package + "java_junit.dart", null));
    unit.getDirectives().add(importDirective(src_package + "java_engine.dart", null));
    unit.getDirectives().add(importDirective(src_package + "java_engine_io.dart", null));
    unit.getDirectives().add(importDirective(src_package + "source_io.dart", null));
    unit.getDirectives().add(importDirective(src_package + "error.dart", null));
    unit.getDirectives().add(importDirective(src_package + "scanner.dart", null));
    unit.getDirectives().add(importDirective(src_package + "ast.dart", null));
    unit.getDirectives().add(
        importDirective(src_package + "parser.dart", null, importShowCombinator("ParserErrorCode")));
    unit.getDirectives().add(importDirective(src_package + "element.dart", null));
    unit.getDirectives().add(importDirective(src_package + "resolver.dart", null));
    unit.getDirectives().add(importDirective(src_package + "engine.dart", null));
    unit.getDirectives().add(importDirective(src_package + "utilities_dart.dart", null));
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
        importDirective("ast_test.dart", null, importShowCombinator("AstFactory")));
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
          if (isRemoved(unitMember)) {
            continue;
          }
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
    unit.getDirectives().add(
        importDirective("utilities_collection.dart", null, importShowCombinator("TokenMap")));
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
    unit.getDirectives().add(importDirective(src_package + "java_core.dart", null));
    unit.getDirectives().add(importDirective(src_package + "java_junit.dart", null));
    unit.getDirectives().add(importDirective(src_package + "source.dart", null));
    unit.getDirectives().add(importDirective(src_package + "error.dart", null));
    unit.getDirectives().add(importDirective(src_package + "scanner.dart", null));
    unit.getDirectives().add(
        importDirective(
            src_package + "utilities_collection.dart",
            null,
            importShowCombinator("TokenMap")));
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
    unit.getDirectives().add(libraryDirective("engine", "sdk", "io"));
    unit.getDirectives().add(importDirective("java_core.dart", null));
    unit.getDirectives().add(importDirective("java_io.dart", null));
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
          || isEnginePath(file, "internal/sdk/LibraryMap")
          || isEnginePath(file, "internal/sdk/LibraryBuilder")) {
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
    unit.getDirectives().add(importDirective("dart:collection", null));
    unit.getDirectives().add(
        importDirective(
            "source.dart",
            null,
            importShowCombinator("ContentCache", "Source", "UriKind")));
    unit.getDirectives().add(importDirective("ast.dart", null));
    unit.getDirectives().add(
        importDirective("engine.dart", null, importShowCombinator("AnalysisContext")));
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      List<CompilationUnitMember> members = entry.getValue();
      {
        for (Iterator<CompilationUnitMember> iter = members.iterator(); iter.hasNext();) {
          CompilationUnitMember member = iter.next();
          if (member instanceof ClassDeclaration
              && ((ClassDeclaration) member).getName().getName().endsWith(
                  "SdkLibrariesReader_LibraryBuilder")) {
            unit.getDeclarations().add(member);
            iter.remove();
            continue;
          }
        }
      }
      if (isEnginePath(file, "sdk/DartSdk") || isEnginePath(file, "sdk/SdkLibrary")
          || isEnginePath(file, "internal/sdk/SdkLibraryImpl")
          || isEnginePath(file, "internal/sdk/LibraryMap")) {
        addNotRemovedCompiationUnitEntries(unit, members);
      }
    }
    return unit;
  }

  private static CompilationUnit buildSourceIoLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "source", "io"));
    unit.getDirectives().add(importDirective("source.dart", null));
    unit.getDirectives().add(importDirective("java_core.dart", null));
    unit.getDirectives().add(importDirective("java_io.dart", null));
    unit.getDirectives().add(importDirective("utilities_general.dart", null));
    unit.getDirectives().add(importDirective("instrumentation.dart", null));
    unit.getDirectives().add(importDirective("engine.dart", null));
    unit.getDirectives().add(exportDirective("source.dart"));
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isEnginePath(file, "source/Source.java")
          || isEnginePath(file, "source/ContentCache.java")
          || isEnginePath(file, "source/DartUriResolver.java")
          || isEnginePath(file, "source/SourceFactory.java")
          || isEnginePath(file, "source/SourceContainer.java")
          || isEnginePath(file, "source/SourceKind.java")
          || isEnginePath(file, "source/UriKind.java")
          || isEnginePath(file, "source/UriResolver.java")
          || isEnginePath(file, "utilities/source/")) {
        continue;
      }
      if (isEnginePath(file, "source/ExplicitPackageUriResolver.java")) {
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
    unit.getDirectives().add(importDirective("dart:collection", null));
    unit.getDirectives().add(importDirective("java_core.dart", null));
    unit.getDirectives().add(importDirective("sdk.dart", null, importShowCombinator("DartSdk")));
    unit.getDirectives().add(
        importDirective(
            "engine.dart",
            null,
            importShowCombinator("AnalysisContext", "TimestampedData")));
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isEnginePath(file, "source/Source.java")
          || isEnginePath(file, "source/ContentCache.java")
          || isEnginePath(file, "source/DartUriResolver.java")
          || isEnginePath(file, "source/LocalSourcePredicate.java")
          || isEnginePath(file, "source/SourceFactory.java")
          || isEnginePath(file, "source/SourceContainer.java")
          || isEnginePath(file, "source/SourceKind.java")
          || isEnginePath(file, "source/UriKind.java")
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
    unit.getDirectives().add(importDirective(src_package + "java_core.dart", null));
    unit.getDirectives().add(importDirective(src_package + "java_junit.dart", null));
    unit.getDirectives().add(importDirective(src_package + "java_engine.dart", null));
    unit.getDirectives().add(importDirective(src_package + "source.dart", null));
    unit.getDirectives().add(importDirective(src_package + "error.dart", null));
    unit.getDirectives().add(importDirective(src_package + "scanner.dart", null));
    unit.getDirectives().add(
        importDirective(
            src_package + "ast.dart",
            null,
            importShowCombinator("AstNode", "NodeLocator")));
    unit.getDirectives().add(
        importDirective(
            src_package + "element.dart",
            null,
            importShowCombinator("InterfaceType", "MethodElement", "PropertyAccessorElement")));
    unit.getDirectives().add(importDirective(src_package + "engine.dart", null));
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

  private static CompilationUnit buildUtilitiesCollectionLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "utilities", "collection"));
    unit.getDirectives().add(importDirective("dart:collection", null));
    unit.getDirectives().add(importDirective("java_core.dart", null));
    unit.getDirectives().add(importDirective("scanner.dart", null, importShowCombinator("Token")));
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isEnginePath(file, "utilities/collection/")) {
        addNotRemovedCompiationUnitEntries(unit, entry.getValue());
      }
    }
    return unit;
  }

  private static CompilationUnit buildUtilitiesDartLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "utilities", "dart"));
    unit.getDirectives().add(importDirective("java_core.dart", null));
    for (CompilationUnitMember member : dartUnit.getDeclarations()) {
      File file = context.getMemberToFile().get(member);
      if (isEnginePath(file, "utilities/dart/ParameterKind")) {
        unit.getDeclarations().add(member);
      }
    }
    return unit;
  }

  private static CompilationUnit buildUtilitiesGeneralLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "utilities", "general"));
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isEnginePath(file, "utilities/general/")) {
        addNotRemovedCompiationUnitEntries(unit, entry.getValue());
      }
    }
    return unit;
  }

  private static void fixUnnecessaryCastHints(String projectPath) throws Exception {
    System.out.println();
    System.out.println("Removing unnecessary casts.");
    AnalysisContext context = AnalysisEngine.getInstance().createAnalysisContext();
    DirectoryBasedDartSdk sdk = DirectoryBasedDartSdk.getDefaultSdk();
    SourceFactory sourceFactory = new SourceFactory(
        new DartUriResolver(sdk),
        new FileUriResolver(),
        new PackageUriResolver(new File(projectPath + "/packages")));
    context.setSourceFactory(sourceFactory);
    // prepare sources
    List<Source> sources = Lists.newArrayList();
    Map<Source, File> sourceToFile = Maps.newHashMap();
    for (File file : FileUtils.listFiles(
        new File(projectPath + "/lib/src/generated"),
        new String[] {"dart"},
        true)) {
      if (file.getAbsolutePath().contains("/packages/")) {
        continue;
      }
      FileBasedSource source = new FileBasedSource(file);
      sources.add(source);
      sourceToFile.put(source, file);
    }
    for (File file : FileUtils.listFiles(
        new File(projectPath + "/test/generated"),
        new String[] {"dart"},
        true)) {
      if (file.getAbsolutePath().contains("/packages/")) {
        continue;
      }
      FileBasedSource source = new FileBasedSource(file);
      sources.add(source);
      sourceToFile.put(source, file);
    }
    // add sources to AnalysisContext
    {
      ChangeSet changeSet = new ChangeSet();
      for (Source source : sources) {
        changeSet.addedSource(source);
      }
      context.applyChanges(changeSet);
    }
    System.out.println(sources.size() + " sources to analyze.");
    // perform analysis
    while (true) {
      AnalysisResult analysisResult = context.performAnalysisTask();
      if (analysisResult.getChangeNotices() == null) {
        break;
      }
    }
    System.out.println("Analysis done.");
    // process errors
    for (Source source : sources) {
      CompilationUnit unit = context.parseCompilationUnit(source);
      List<Edit> edits = Lists.newArrayList();
      AnalysisErrorInfo errorInfo = context.getErrors(source);
      AnalysisError[] errors = errorInfo.getErrors();
      for (AnalysisError error : errors) {
        if (error.getErrorCode() == HintCode.UNNECESSARY_CAST) {
          AstNode node = new NodeLocator(error.getOffset()).searchWithin(unit);
          AsExpression asExpression = node.getAncestor(AsExpression.class);
          if (asExpression != null) {
            // remove "as" and its enclosing ()
            AstNode enclosing = asExpression;
            if (enclosing.getParent() instanceof ParenthesizedExpression) {
              enclosing = enclosing.getParent();
            }
            // add Edit
            Expression expr = asExpression.getExpression();
            int enOffset = enclosing.getOffset();
            int exEnd = expr.getEnd();
            edits.add(new Edit(enOffset, expr.getOffset() - enOffset, ""));
            edits.add(new Edit(exEnd, enclosing.getEnd() - exEnd, ""));
          }
        }
      }
      // apply edits to file
      File file = sourceToFile.get(source);
      applyEdits(file, edits);
    }
    System.out.println("Edits applied.");
    System.out.println();
  }

  /**
   * @return the formatted Dart source dump of the given {@link AstNode}.
   */
  private static String getFormattedSource(CompilationUnit unit) {
    PrintStringWriter writer = new PrintStringWriter();
    writer.append(HEADER);
    sortUnitMembersByName(unit);
    unit.accept(new ToFormattedSourceVisitor(writer));
    String source = writer.toString();
    source = removeTrailingWhitespaces(source);
    return source;
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

  private static boolean isRemoved(CompilationUnitMember member) {
    CompilationUnit memberUnit = (CompilationUnit) member.getParent();
    return memberUnit == null || !memberUnit.getDeclarations().contains(member);
  }

  private static String makeSource(String... lines) {
    return Joiner.on("\n").join(lines);
  }

  /**
   * Removes {@link ClassDeclaration} with the given name.
   */
  private static void removeClass(CompilationUnit unit, String name) {
    NodeList<CompilationUnitMember> declarations = unit.getDeclarations();
    for (Iterator<CompilationUnitMember> iter = declarations.iterator(); iter.hasNext();) {
      CompilationUnitMember member = iter.next();
      if (member instanceof ClassDeclaration) {
        ClassDeclaration classDeclaration = (ClassDeclaration) member;
        if (classDeclaration.getName().getName().equals(name)) {
          iter.remove();
        }
      }
    }
  }

  /**
   * Removes trailing spaces from the given Dart source.
   */
  private static String removeTrailingWhitespaces(String source) {
    String[] lines = StringUtils.splitPreserveAllTokens(source, '\n');
    for (int i = 0; i < lines.length; i++) {
      lines[i] = StringUtils.stripEnd(lines[i], null);
    }
    return StringUtils.join(lines, "\n");
  }

  /**
   * Replaces the fragment of the source specified by the RE pattern with the given source.
   * 
   * @param source the source to replace fragment in
   * @param pattern the fragment to replace
   * @param replacement the source to replace fragment with
   * @return the source with the replacement fragment
   */
  private static String replaceSourceFragment(String source, String pattern, String replacement) {
    int index = source.indexOf(pattern);
    if (index == -1) {
      throw new IllegalArgumentException("Not found: " + pattern);
    }
    return StringUtils.replace(source, pattern, replacement);
  }

  /**
   * Replaces the fragment of the source specified by the RE pattern with the given source.
   * 
   * @param source the source to replace fragment in
   * @param pattern the regular expression describing fragment
   * @param replacement the source to replace fragment with
   * @return the source with the replacement fragment
   */
  private static String replaceSourceFragmentRE(String source, String pattern, String replacement) {
    Matcher matcher = Pattern.compile(pattern, Pattern.MULTILINE | Pattern.DOTALL).matcher(source);
    if (!matcher.find()) {
      throw new IllegalArgumentException("Not found: " + pattern);
    }
    return matcher.replaceFirst(replacement);
  }

  private static void sortUnitMembersByName(CompilationUnit unit) {
    Collections.sort(unit.getDeclarations(), new Comparator<CompilationUnitMember>() {
      @Override
      public int compare(CompilationUnitMember o1, CompilationUnitMember o2) {
        String name1 = getName(o1);
        String name2 = getName(o2);
        return name1.compareTo(name2);
      }

      private String getName(CompilationUnitMember member) {
        if (member instanceof ClassDeclaration) {
          return ((ClassDeclaration) member).getName().getName();
        }
        if (member instanceof FunctionDeclaration) {
          return ((FunctionDeclaration) member).getName().getName();
        }
        throw new UnsupportedOperationException(member.toSource());
      }
    });
  }
}
