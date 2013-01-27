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
import com.google.common.io.Files;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.java2dart.Context;
import com.google.dart.java2dart.processor.CollectionSemanticProcessor;
import com.google.dart.java2dart.processor.ObjectSemanticProcessor;
import com.google.dart.java2dart.processor.SemanticProcessor;
import com.google.dart.java2dart.util.ToFormattedSourceVisitor;

import static com.google.dart.java2dart.util.ASTFactory.importDirective;
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
  private static File engineFolder2;
  private static CompilationUnit dartUnit;

  private static final List<SemanticProcessor> PROCESSORS = ImmutableList.of(
      ObjectSemanticProcessor.INSTANCE,
      CollectionSemanticProcessor.INSTANCE,
      EngineSemanticProcessor.INSTANCE);

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.out.println("Usage: java2dart <target-folder>");
      System.exit(0);
    }
    String targetFolder = args[0];
    System.out.println("Generating files into " + targetFolder);
    new File(targetFolder).mkdirs();
    //
    engineFolder = new File("../../../tools/plugins/com.google.dart.engine/src");
    engineFolder2 = new File("src");
    engineFolder = engineFolder.getCanonicalFile();
    // configure Context
    context.addSourceFolder(engineFolder);
    context.addSourceFiles(new File(
        engineFolder,
        "com/google/dart/engine/utilities/instrumentation"));
    context.addSourceFile(new File(engineFolder, "com/google/dart/engine/source/Source.java"));
    context.addSourceFile(new File(
        engineFolder,
        "com/google/dart/engine/utilities/dart/ParameterKind.java"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/error"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/scanner"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/ast"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/parser"));
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/internal/parser"));
    context.addSourceFile(new File(
        engineFolder2,
        "com/google/dart/java2dart/util/ToFormattedSourceVisitor.java"));
    // translate into single CompilationUnit
    dartUnit = context.translate();
    // run processors
    for (SemanticProcessor processor : PROCESSORS) {
      processor.process(context, dartUnit);
    }
    // dump as several libraries
    Files.copy(new File("resources/javalib.dart"), new File(targetFolder + "/javalib.dart"));
    Files.copy(new File("resources/enginelib.dart"), new File(targetFolder + "/enginelib.dart"));
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
          Files.toString(new File("resources/include_ast.dart"), Charsets.UTF_8),
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
    System.out.println("Translation complete");
  }

  private static CompilationUnit buildAstLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "ast"));
    unit.getDirectives().add(importDirective("dart:collection", null));
    unit.getDirectives().add(importDirective("javalib.dart", null));
    unit.getDirectives().add(importDirective("enginelib.dart", null));
    unit.getDirectives().add(importDirective("error.dart", null));
    unit.getDirectives().add(importDirective("scanner.dart", null));
    for (CompilationUnitMember member : dartUnit.getDeclarations()) {
      File file = context.getMemberToFile().get(member);
      if (isEnginePath(file, "ast/") || isEnginePath(file, "utilities/dart/ParameterKind")) {
        unit.getDeclarations().add(member);
      }
    }
    return unit;
  }

  private static CompilationUnit buildErrorLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "error"));
    unit.getDirectives().add(importDirective("javalib.dart", null));
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
    unit.getDirectives().add(importDirective("javalib.dart", null));
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
    unit.getDirectives().add(importDirective("javalib.dart", null));
    unit.getDirectives().add(importDirective("enginelib.dart", null));
    unit.getDirectives().add(importDirective("error.dart", null));
    unit.getDirectives().add(importDirective("source.dart", null));
    unit.getDirectives().add(importDirective("scanner.dart", null));
    unit.getDirectives().add(importDirective("ast.dart", null));
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
    unit.getDirectives().add(importDirective("javalib.dart", null));
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

  private static CompilationUnit buildSourceLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("engine", "source"));
    unit.getDirectives().add(importDirective("javalib.dart", null));
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isEnginePath(file, "source/")) {
        unit.getDeclarations().addAll(entry.getValue());
      }
    }
    return unit;
  }

  /**
   * @return the formatted Dart source dump of the given {@link ASTNode}.
   */
  private static String getFormattedSource(ASTNode node) {
    PrintStringWriter writer = new PrintStringWriter();
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
}
