/*
 * Copyright (c) 2014, the Dart project authors.
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
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.ast.AsExpression;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ParenthesizedExpression;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisErrorInfo;
import com.google.dart.engine.context.AnalysisResult;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.HintCode;
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

import static com.google.dart.java2dart.util.AstFactory.importDirective;
import static com.google.dart.java2dart.util.AstFactory.importHideCombinator;
import static com.google.dart.java2dart.util.AstFactory.importShowCombinator;
import static com.google.dart.java2dart.util.AstFactory.libraryDirective;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Translates some parts of "com.google.dart.engine" project.
 */
public class MainAnalysisServer {
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

  private static final Context context = new Context();
  private static File engineFolder;
  private static File serverFolder;
  private static File serviceFolder;

  private static CompilationUnit dartUnit;

  private static final String HEADER = "// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file\n"
      + "// for details. All rights reserved. Use of this source code is governed by a\n"
      + "// BSD-style license that can be found in the LICENSE file.\n"
      + "\n"
      + "// This code was auto-generated, is not intended to be edited, and is subject to\n"
      + "// significant change. Please see the README file for more information.\n\n";

  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      System.out.println("Usage: java2dart <services-target-src-folder> <server-target-src-folder>");
      System.exit(0);
    }
    String targetFolderServices = args[0];
    String targetFolderServer = args[1];
    System.out.println("Generating files into\n\t" + targetFolderServices + "\n\t"
        + targetFolderServer);
    new File(targetFolderServer).mkdirs();
    //
    engineFolder = new File("../../../tools/plugins/com.google.dart.engine/src");
    serviceFolder = new File("../../../tools/plugins/com.google.dart.engine.services/src");
    serverFolder = new File("../../../tools/plugins/com.google.dart.server/src");
    engineFolder = engineFolder.getCanonicalFile();
    // configure Context
    context.addClasspathFile(new File("../../../../third_party/guava/r13/guava-13.0.1.jar"));
    context.addClasspathFile(new File(
        "../../../../third_party/commons-lang/3.2.1/commons-lang3-3.2.1.jar"));
    context.addClasspathFile(new File("../../../../third_party/junit/v4_8_2/junit.jar"));
    context.addSourceFolder(engineFolder);
    context.addSourceFolder(serviceFolder);
    context.addSourceFolder(serverFolder);
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/"));
    context.addSourceFiles(new File(serverFolder, "com/google/dart/server/"));
    context.removeSourceFiles(new File(serverFolder, "com/google/dart/server/internal/remote/"));
    context.addSourceFiles(new File(serviceFolder, "com/google/dart/engine/services/assist"));
    context.addSourceFiles(new File(serviceFolder, "com/google/dart/engine/services/change"));
    context.addSourceFiles(new File(serviceFolder, "com/google/dart/engine/services/completion"));
    context.addSourceFiles(new File(serviceFolder, "com/google/dart/engine/services/correction"));
    context.addSourceFiles(new File(
        serviceFolder,
        "com/google/dart/engine/services/internal/completion"));
    context.addSourceFiles(new File(
        serviceFolder,
        "com/google/dart/engine/services/internal/correction"));
    context.addSourceFiles(new File(
        serviceFolder,
        "com/google/dart/engine/services/internal/refactoring"));
    context.addSourceFiles(new File(serviceFolder, "com/google/dart/engine/services/internal/util"));
    context.addSourceFiles(new File(serviceFolder, "com/google/dart/engine/services/refactoring"));
    context.addSourceFiles(new File(serviceFolder, "com/google/dart/engine/services/status"));
    context.addSourceFiles(new File(serviceFolder, "com/google/dart/engine/services/util"));
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
    {
      CompilationUnit library = buildServiceInterfacesLibrary();
      Files.write(getFormattedSource(library), new File(targetFolderServer
          + "/service_interfaces.dart"), Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildServiceComputersLibrary();
      Files.write(getFormattedSource(library), new File(targetFolderServer
          + "/service_computers.dart"), Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildServicesLibrary_change();
      Files.write(
          getFormattedSource(library),
          new File(targetFolderServices + "/change.dart"),
          Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildServicesLibrary_completion();
      String source = getFormattedSource(library);
      // TODO(scheglov) improve java2dart to make this translated correctly
      source = replaceSourceFragment(
          source,
          "new CompletionEngine_CommentReferenceCompleter(this, _completionNode)",
          "new CompletionEngine_CommentReferenceCompleter(CompletionEngine_this, _completionNode)");
      source = replaceSourceFragment(
          source,
          "new CompletionEngine_TypeNameCompleter(this, _completionNode, node)",
          "new CompletionEngine_TypeNameCompleter(CompletionEngine_this, _completionNode, node)");
//      source = replaceSourceFragment(
//          source,
//          "new CompletionEngine_NameCollector(this)",
//          "new CompletionEngine_NameCollector(CompletionEngine_this)");
      source = replaceSourceFragment(
          source,
          "new CompletionEngine_IdentifierCompleter(this, node)",
          "new CompletionEngine_IdentifierCompleter(CompletionEngine_this, node)");
      source = replaceSourceFragment(
          source,
          "new CompletionEngine_StringCompleter(this, node)",
          "new CompletionEngine_StringCompleter(CompletionEngine_this, node)");
      Files.write(source, new File(targetFolderServices + "/completion.dart"), Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildServicesLibrary_status();
      Files.write(
          getFormattedSource(library),
          new File(targetFolderServices + "/status.dart"),
          Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildServicesLibrary_proposal();
      Files.write(
          getFormattedSource(library),
          new File(targetFolderServices + "/proposal.dart"),
          Charsets.UTF_8);
    }
    {
      CompilationUnit library = buildServicesLibrary_util();
      Files.write(
          getFormattedSource(library),
          new File(targetFolderServices + "/util.dart"),
          Charsets.UTF_8);
    }
    // TODO(scheglov) restore to translate more
//    {
//      CompilationUnit library = buildServicesLibrary_assist();
//      Files.write(getFormattedSource(library), new File(targetFolder2
//          + "/service_correction.dart"), Charsets.UTF_8);
//    }
    {
      CompilationUnit library = buildServicesLibrary_refactoring();
      Files.write(
          getFormattedSource(library),
          new File(targetFolderServices + "/refactoring.dart"),
          Charsets.UTF_8);
    }
    {
      String projectFolder = new File(targetFolderServices).getParentFile().getParentFile().getParent();
      fixUnnecessaryCastHints(projectFolder);
    }
    {
      String projectFolder = new File(targetFolderServer).getParentFile().getParentFile().getParent();
      fixUnnecessaryCastHints(projectFolder);
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

  private static CompilationUnit buildServiceComputersLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("service", "computers"));
    unit.getDirectives().add(
        importDirective(
            "package:analyzer/src/generated/java_core.dart",
            null,
            importShowCombinator("JavaStringBuilder", "StringUtils")));
    unit.getDirectives().add(
        importDirective("package:analyzer/src/generated/java_engine.dart", null));
    unit.getDirectives().add(
        importDirective(
            "package:analyzer/src/generated/source.dart",
            null,
            importShowCombinator("Source")));
    unit.getDirectives().add(
        importDirective(
            "package:analyzer/src/generated/scanner.dart",
            null,
            importShowCombinator("Token")));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/ast.dart", null));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/element.dart", "pae"));
    unit.getDirectives().add(
        importDirective(
            "package:analyzer/src/generated/element.dart",
            null,
            importShowCombinator("DartType")));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/source.dart", null));
    unit.getDirectives().add(importDirective("service_interfaces.dart", "psi"));
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isServerPath(file, "internal/local/computer/")) {
        addNotRemovedCompiationUnitEntries(unit, entry.getValue());
      }
    }
    EngineSemanticProcessor.useImportPrefix(
        context,
        unit,
        "psi",
        new String[] {"com.google.dart.server."},
        true);
    EngineSemanticProcessor.useImportPrefix(
        context,
        unit,
        "pae",
        new String[] {"com.google.dart.engine.element."},
        false);
    return unit;
  }

  private static CompilationUnit buildServiceInterfacesLibrary() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("service", "interfaces"));
    unit.getDirectives().add(
        importDirective(
            "package:analyzer/src/generated/java_core.dart",
            null,
            importShowCombinator("Enum", "StringUtils")));
    unit.getDirectives().add(
        importDirective(
            "package:analyzer/src/generated/source.dart",
            null,
            importShowCombinator("Source")));
    unit.getDirectives().add(
        importDirective(
            "package:analysis_services/src/generated/change.dart",
            null,
            importShowCombinator("SourceChange")));
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isServerPath(file, "AssistsConsumer.java")
          || isServerPath(file, "CompletionSuggestion.java")
          || isServerPath(file, "CompletionSuggestionKind.java")
          || isServerPath(file, "Consumer.java") || isServerPath(file, "Element.java")
          || isServerPath(file, "ElementKind.java") || isServerPath(file, "HighlightRegion.java")
          || isServerPath(file, "HighlightType.java") || isServerPath(file, "ListSourceSet.java")
          || isServerPath(file, "NavigationRegion.java")
          || isServerPath(file, "NotificationKind.java") || isServerPath(file, "Outline.java")
          || isServerPath(file, "OutlineKind.java") || isServerPath(file, "SearchResult.java")
          || isServerPath(file, "SearchResultKind.java")
          || isServerPath(file, "SearchResultsConsumer.java")
          || isServerPath(file, "SourceRegion.java") || isServerPath(file, "SourceSet.java")
          || isServerPath(file, "SourceSetKind.java")
          || isServerPath(file, "TypeHierarchyItem.java")
          || isServerPath(file, "internal/local/ImplicitSourceSet.java")) {
        addNotRemovedCompiationUnitEntries(unit, entry.getValue());
      }
    }
    return unit;
  }

  private static CompilationUnit buildServicesLibrary_assist() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("service", "correction"));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/ast.dart", null));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/element.dart", null));
    unit.getDirectives().add(
        importDirective(
            "package:analyzer/src/generated/engine.dart",
            null,
            importShowCombinator("AnalysisContext")));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/error.dart", null));
    unit.getDirectives().add(
        importDirective(
            "package:analyzer/src/generated/java_core.dart",
            null,
            importHideCombinator("StringUtils")));
    unit.getDirectives().add(
        importDirective(
            "package:analyzer/src/generated/java_io.dart",
            null,
            importShowCombinator("JavaFile")));
    unit.getDirectives().add(
        importDirective(
            "package:analyzer/src/generated/parser.dart",
            null,
            importShowCombinator("ParserErrorCode")));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/scanner.dart", null));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/sdk.dart", null));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/source_io.dart", null));
    unit.getDirectives().add(
        importDirective("package:analyzer/src/generated/utilities_dart.dart", null));
    unit.getDirectives().add(importDirective("change.dart", null));
    unit.getDirectives().add(importDirective("proposal.dart", null));
    unit.getDirectives().add(importDirective("stubs.dart", null));
    unit.getDirectives().add(importDirective("util.dart", null));
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isCorrectionProposalFile(file)) {
        continue;
      }
      if (isCorrectionUtilFile(file)) {
        continue;
      }
      if (isServicePath(file, "assist/") || isServicePath(file, "correction/")
          || isServicePath(file, "internal/correction/")
          || isServicePath(file, "internal/util/TokenUtils.java") || isServicePath(file, "util/")) {
        addNotRemovedCompiationUnitEntries(unit, entry.getValue());
      }
    }
    return unit;
  }

  private static CompilationUnit buildServicesLibrary_change() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("services", "change"));
    unit.getDirectives().add(importDirective("dart:collection", null));
    unit.getDirectives().add(
        importDirective(
            "package:analyzer/src/generated/java_io.dart",
            null,
            importShowCombinator("JavaFile")));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/source.dart", null));
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isServicePath(file, "change/")) {
        addNotRemovedCompiationUnitEntries(unit, entry.getValue());
      }
    }
    return unit;
  }

  private static CompilationUnit buildServicesLibrary_completion() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("services", "completion"));
    unit.getDirectives().add(importDirective("dart:collection", null));
    unit.getDirectives().add(
        importDirective(
            "package:analyzer/src/generated/java_core.dart",
            null,
            importHideCombinator("StringUtils")));
    unit.getDirectives().add(
        importDirective("package:analyzer/src/generated/java_engine.dart", null));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/java_io.dart", null));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/ast.dart", null));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/element.dart", null));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/engine.dart", null));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/error.dart", null));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/resolver.dart", null));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/scanner.dart", null));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/sdk.dart", null));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/source_io.dart", null));
    unit.getDirectives().add(
        importDirective("package:analyzer/src/generated/utilities_dart.dart", null));
    unit.getDirectives().add(importDirective("stubs.dart", null));
    unit.getDirectives().add(importDirective("util.dart", null));
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isServicePath(file, "completion/") || isServicePath(file, "internal/completion/")) {
        addNotRemovedCompiationUnitEntries(unit, entry.getValue());
      }
    }
    return unit;
  }

  private static CompilationUnit buildServicesLibrary_proposal() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("services", "proposal"));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/java_core.dart", null));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/java_io.dart", null));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/source.dart", null));
    unit.getDirectives().add(importDirective("change.dart", null));
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isCorrectionProposalFile(file)) {
        addNotRemovedCompiationUnitEntries(unit, entry.getValue());
      }
    }
    return unit;
  }

  private static CompilationUnit buildServicesLibrary_refactoring() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("service", "refactoring"));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/ast.dart", null));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/element.dart", null));
//    unit.getDirectives().add(
//        importDirective(
//            "package:analyzer/src/generated/engine.dart",
//            null,
//            importShowCombinator("AnalysisContext")));
//    unit.getDirectives().add(importDirective("package:analyzer/src/generated/error.dart", null));
//    unit.getDirectives().add(
//        importDirective(
//            "package:analyzer/src/generated/java_core.dart",
//            null,
//            importHideCombinator("StringUtils")));
//    unit.getDirectives().add(
//        importDirective(
//            "package:analyzer/src/generated/java_io.dart",
//            null,
//            importShowCombinator("JavaFile")));
//    unit.getDirectives().add(
//        importDirective(
//            "package:analyzer/src/generated/parser.dart",
//            null,
//            importShowCombinator("ParserErrorCode")));
//    unit.getDirectives().add(importDirective("package:analyzer/src/generated/scanner.dart", null));
//    unit.getDirectives().add(importDirective("package:analyzer/src/generated/sdk.dart", null));
//    unit.getDirectives().add(importDirective("package:analyzer/src/generated/source_io.dart", null));
//    unit.getDirectives().add(
//        importDirective("package:analyzer/src/generated/utilities_dart.dart", null));
//    unit.getDirectives().add(importDirective("change.dart", null));
//    unit.getDirectives().add(importDirective("proposal.dart", null));
//    unit.getDirectives().add(importDirective("stubs.dart", null));
//    unit.getDirectives().add(importDirective("util.dart", null));
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isServicePath(file, "refactoring/") || isServicePath(file, "internal/refactoring/")) {
        addNotRemovedCompiationUnitEntries(unit, entry.getValue());
      }
    }
    return unit;
  }

  private static CompilationUnit buildServicesLibrary_status() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("services", "status"));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/java_core.dart", null));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/ast.dart", null));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/element.dart", null));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/engine.dart", null));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/source.dart", null));
    unit.getDirectives().add(importDirective("stubs.dart", null));
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isServicePath(file, "status/")) {
        addNotRemovedCompiationUnitEntries(unit, entry.getValue());
      }
    }
    return unit;
  }

  private static CompilationUnit buildServicesLibrary_util() throws Exception {
    CompilationUnit unit = new CompilationUnit(null, null, null, null, null);
    unit.getDirectives().add(libraryDirective("services", "util"));
    unit.getDirectives().add(importDirective("dart:collection", null));
    unit.getDirectives().add(
        importDirective(
            "package:analyzer/src/generated/java_core.dart",
            null,
            importHideCombinator("StringUtils")));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/ast.dart", null));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/element.dart", null));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/engine.dart", null));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/error.dart", null));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/resolver.dart", null));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/source.dart", null));
    unit.getDirectives().add(importDirective("package:analyzer/src/generated/scanner.dart", null));
    unit.getDirectives().add(importDirective("change.dart", null));
    unit.getDirectives().add(importDirective("proposal.dart", null));
    unit.getDirectives().add(importDirective("status.dart", null));
    unit.getDirectives().add(importDirective("stubs.dart", null));
    for (Entry<File, List<CompilationUnitMember>> entry : context.getFileToMembers().entrySet()) {
      File file = entry.getKey();
      if (isCorrectionUtilFile(file)) {
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

  private static boolean isCorrectionProposalFile(File file) throws Exception {
    return isServicePath(file, "correction/AddDependencyCorrectionProposal.java")
        || isServicePath(file, "correction/ChangeCorrectionProposal.java")
        || isServicePath(file, "correction/CorrectionImage.java")
        || isServicePath(file, "correction/CorrectionKind.java")
        || isServicePath(file, "correction/CorrectionProposal.java")
        || isServicePath(file, "correction/CreateFileCorrectionProposal.java")
        || isServicePath(file, "correction/LinkedPositionProposal.java")
        || isServicePath(file, "correction/SourceCorrectionProposal.java");
  }

  private static boolean isCorrectionUtilFile(File file) throws Exception {
    return isServicePath(file, "assist/AssistContext.java")
        || isServicePath(file, "util/HierarchyUtils.java")
        || isServicePath(file, "util/NameOccurrencesFinder.java")
        || isServicePath(file, "util/SelectionAnalyzer.java")
        || isServicePath(file, "internal/correction/CorrectionUtils.java")
        || isServicePath(file, "internal/correction/SourceBuilder.java")
        || isServicePath(file, "internal/correction/StatementAnalyzer.java")
        || isServicePath(file, "internal/correction/URIUtils.java")
        || isServicePath(file, "internal/util/TokenUtils.java");
  }

  /**
   * @param serverPackage the sub-package in <code>com/google/dart/server</code>.
   * @return <code>true</code> if given {@link File} is located in sub-package of Engine project.
   */
  private static boolean isServerPath(File file, String serverPackage) throws Exception {
    String filePath = file.getCanonicalPath();
    String prefix = serverFolder.getCanonicalPath() + "/com/google/dart/server/" + serverPackage;
    return filePath.startsWith(prefix);
  }

  /**
   * @param servicePackage the sub-package in <code>com/google/dart/engine/services</code>.
   * @return <code>true</code> if given {@link File} is located in sub-package of Engine project.
   */
  private static boolean isServicePath(File file, String servicePackage) throws Exception {
    String filePath = file.getCanonicalPath();
    String prefix = serviceFolder.getCanonicalPath() + "/com/google/dart/engine/services/"
        + servicePackage;
    return filePath.startsWith(prefix);
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

  private static void sortUnitMembersByName(CompilationUnit unit) {
    Collections.sort(unit.getDeclarations(), new Comparator<CompilationUnitMember>() {
      @Override
      public int compare(CompilationUnitMember o1, CompilationUnitMember o2) {
        String name1 = ((ClassDeclaration) o1).getName().getName();
        String name2 = ((ClassDeclaration) o2).getName().getName();
        return name1.compareTo(name2);
      }
    });
  }
}
