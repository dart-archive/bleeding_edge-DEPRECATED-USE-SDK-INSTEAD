/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.formatter;

import com.google.dart.compiler.DartCompilationError;
import com.google.dart.compiler.DartCompilerListener;
import com.google.dart.compiler.ast.DartComment;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.parser.CommentPreservingParser;
import com.google.dart.compiler.parser.DartScannerParserContext;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility methods for parsing snippets of code. Not quite sure what goes here.
 * 
 * @deprecated Consider removing this class
 */
@Deprecated
@SuppressWarnings({"unused", "rawtypes"})
public class CodeSnippetParsingUtil {
  class RecordedParsingInformation {
    public CategorizedProblem[] problems;
    public int problemsCount;
    public int[] lineEnds;
    public int[][] commentPositions;

    public RecordedParsingInformation(CategorizedProblem[] problems, int[] lineEnds,
        int[][] commentPositions) {
      this.problems = problems;
      this.lineEnds = lineEnds;
      this.commentPositions = commentPositions;
      this.problemsCount = problems != null ? problems.length : 0;
    }

    void updateRecordedParsingInformation(CompilationResult compilationResult) {
      if (compilationResult.problems != null) {
        this.problems = compilationResult.problems;
        this.problemsCount = this.problems.length;
      }
    }
  }

  private static class CategorizedProblem {
    // probably should be global
  }

  private static class CompilationResult {
    // probably should be global
    CategorizedProblem[] problems;
    int problemCount;
    int[] lineEnds;

    int[] getLineSeparatorPositions() {
      return lineEnds;
    }

    void scanLines(String source) {
      int len = source.length();
      List<Integer> lines = new ArrayList<Integer>();
      for (int i = 0; i < len; i++) {
        char c = source.charAt(i);
        char prev = 0;
        switch (c) {
          case '\r':
            if (prev != '\n') {
              lines.add(i);
            }
            break;
          case '\n':
            if (prev != '\r') {
              lines.add(i);
            }
            break;
          default:
            break;
        }
        prev = c;
      }
      lineEnds = new int[lines.size()];
      for (int i = 0; i < lineEnds.length; i++) {
        lineEnds[i] = lines.get(i);
      }
    }
  }

  public RecordedParsingInformation recordedParsingInformation;
  public boolean ignoreMethodBodies;

  public CodeSnippetParsingUtil() {
    this(false);
  }

  public CodeSnippetParsingUtil(boolean ignoreMethodBodies) {
    DartCore.notYetImplemented();
    this.ignoreMethodBodies = ignoreMethodBodies;
  }

  public DartNode[] parseClassBodyDeclarations(char[] source, int offset, int length, Map settings,
      boolean recordParsingInformation, boolean enabledStatementRecovery) {
    return null;
  }

  public DartNode[] parseClassBodyDeclarations(char[] source, Map settings,
      boolean recordParsingInformation) {
    return parseClassBodyDeclarations(source, 0, source.length, settings, recordParsingInformation,
        false);
  }

  public DartUnit parseCompilationUnit(char[] source, Map settings, boolean recordParsingInformation) {
    if (source == null) {
      throw new IllegalArgumentException();
    }
    String sourceCode = new String(source);
    final CompilationResult compilationResult = new CompilationResult();
    compilationResult.scanLines(sourceCode);
    DartCompilerListener listener = new DartCompilerListener.Empty() {
      @Override
      public void onError(DartCompilationError event) {
        compilationResult.problemCount += 1;
      }
    };
    DartScannerParserContext ctx = new DartScannerParserContext(null, sourceCode, listener);
    CommentPreservingParser parser = new CommentPreservingParser(sourceCode, listener, false);
    DartUnit compilationUnit = DartCompilerUtilities.secureParseUnit(parser, null);
    if (recordParsingInformation) {
      recordedParsingInformation = getRecordedParsingInformation(compilationResult,
          extractCommentLocs(compilationUnit));
      recordedParsingInformation.updateRecordedParsingInformation(compilationResult);
    }
    return compilationUnit;

//    CompilerOptions compilerOptions = new CompilerOptions(settings);
//    compilerOptions.ignoreMethodBodies = this.ignoreMethodBodies;
//    CommentRecorderParser parser = new CommentRecorderParser(
//        new ProblemReporter(
//            DefaultErrorHandlingPolicies.proceedWithAllProblems(),
//            compilerOptions, new DefaultProblemFactory(Locale.getDefault())),
//        false);
//
//    ICompilationUnit sourceUnit = new CompilationUnit(source, "", //$NON-NLS-1$
//        compilerOptions.defaultEncoding);
//    final CompilationResult compilationResult = new CompilationResult(
//        sourceUnit, 0, 0, compilerOptions.maxProblemsPerUnit);
//    CompilationUnitDeclaration compilationUnitDeclaration = parser.dietParse(
//        sourceUnit, compilationResult);
//
//    if (recordParsingInformation) {
//      this.recordedParsingInformation = getRecordedParsingInformation(
//          compilationResult, compilationUnitDeclaration.comments);
//    }
//
//    if (compilationUnitDeclaration.ignoreMethodBodies) {
//      compilationUnitDeclaration.ignoreFurtherInvestigation = true;
//      // if initial diet parse did not work, no need to dig into method bodies.
//      return compilationUnitDeclaration;
//    }
//
//    // fill the methods bodies in order for the code to be generated
//    // real parse of the method....
//    parser.scanner.setSource(compilationResult);
//    org.eclipse.jdt.internal.compiler.ast.TypeDeclaration[] types = compilationUnitDeclaration.types;
//    if (types != null) {
//      for (int i = 0, length = types.length; i < length; i++) {
//        types[i].parseMethods(parser, compilationUnitDeclaration);
//      }
//    }
//
//    if (recordParsingInformation) {
//      this.recordedParsingInformation.updateRecordedParsingInformation(compilationResult);
//    }
//    return compilationUnitDeclaration;
  }

  public DartExpression parseExpression(char[] source, int offset, int length, Map settings,
      boolean recordParsingInformation) {
    return null;
  }

  public DartExpression parseExpression(char[] source, Map settings,
      boolean recordParsingInformation) {
    return parseExpression(source, 0, source.length, settings, recordParsingInformation);
  }

  public DartMethodDefinition parseStatements(char[] source, int offset, int length, Map settings,
      boolean recordParsingInformation, boolean enabledStatementRecovery) {
    return null;
  }

  public DartMethodDefinition parseStatements(char[] source, Map settings,
      boolean recordParsingInformation, boolean enabledStatementRecovery) {
    return parseStatements(source, 0, source.length, settings, recordParsingInformation,
        enabledStatementRecovery);
  }

  private int[][] extractCommentLocs(DartUnit unit) {
    List<DartComment> comments = unit.getComments();
    int n = comments == null ? 0 : comments.size();
    int[][] locs = new int[n][2];
    for (int i = 0; i < locs.length; i++) {
      DartComment comment = comments.get(i);
      int start = comment.getSourceInfo().getOffset();
      int stop = start + comment.getSourceInfo().getLength();
      locs[i][0] = start;
      locs[i][1] = stop;
    }
    return locs;
  }

  private RecordedParsingInformation getRecordedParsingInformation(
      CompilationResult compilationResult, int[][] commentPositions) {
    int problemsCount = compilationResult.problemCount;
    CategorizedProblem[] problems = null;
    if (problemsCount != 0) {
      final CategorizedProblem[] compilationResultProblems = compilationResult.problems;
      if (compilationResultProblems != null) {
        if (compilationResultProblems.length == problemsCount) {
          problems = compilationResultProblems;
        } else {
          System.arraycopy(compilationResultProblems, 0,
              problems = new CategorizedProblem[problemsCount], 0, problemsCount);
        }
      }
    }
    return new RecordedParsingInformation(problems, compilationResult.getLineSeparatorPositions(),
        commentPositions);
  }
}
