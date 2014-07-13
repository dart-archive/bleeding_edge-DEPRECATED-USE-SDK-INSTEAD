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
package com.google.dart.engine.internal.task;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.PartOfDirective;
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.ast.UriBasedDirective;
import com.google.dart.engine.ast.UriBasedDirective.UriValidationCode;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.context.AnalysisOptions;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.context.PerformanceStatistics;
import com.google.dart.engine.internal.context.RecordingErrorListener;
import com.google.dart.engine.parser.Parser;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.general.TimeCounter.TimeCounterHandle;
import com.google.dart.engine.utilities.io.UriUtilities;
import com.google.dart.engine.utilities.source.LineInfo;

import java.util.HashSet;

/**
 * Instances of the class {@code ParseDartTask} parse a specific source as a Dart file.
 */
public class ParseDartTask extends AnalysisTask {

  /**
   * Return the result of resolving the URI of the given URI-based directive against the URI of the
   * given library, or {@code null} if the URI is not valid.
   * 
   * @param context the context in which the resolution is to be performed
   * @param librarySource the source representing the library containing the directive
   * @param directive the directive which URI should be resolved
   * @param errorListener the error listener to which errors should be reported
   * @return the result of resolving the URI against the URI of the library
   */
  public static Source resolveDirective(AnalysisContext context, Source librarySource,
      UriBasedDirective directive, AnalysisErrorListener errorListener) {
    StringLiteral uriLiteral = directive.getUri();
    String uriContent = uriLiteral.getStringValue();
    if (uriContent != null) {
      uriContent = uriContent.trim();
      directive.setUriContent(uriContent);
    }
    UriValidationCode code = directive.validate();
    if (code == null) {
      String encodedUriContent = UriUtilities.encode(uriContent);
      Source source = context.getSourceFactory().resolveUri(librarySource, encodedUriContent);
      directive.setSource(source);
      return source;
    }
    if (code == UriValidationCode.URI_WITH_DART_EXT_SCHEME) {
      return null;
    }
    if (code == UriValidationCode.URI_WITH_INTERPOLATION) {
      errorListener.onError(new AnalysisError(
          librarySource,
          uriLiteral.getOffset(),
          uriLiteral.getLength(),
          CompileTimeErrorCode.URI_WITH_INTERPOLATION));
      return null;
    }
    if (code == UriValidationCode.INVALID_URI) {
      errorListener.onError(new AnalysisError(
          librarySource,
          uriLiteral.getOffset(),
          uriLiteral.getLength(),
          CompileTimeErrorCode.INVALID_URI,
          uriContent));
      return null;
    }
    throw new RuntimeException("Failed to handle validation code: " + code);
  }

  /**
   * The source to be parsed.
   */
  private Source source;

  /**
   * The time at which the contents of the source were last modified.
   */
  private long modificationTime;

  /**
   * The head of the token stream used for parsing.
   */
  private Token tokenStream;

  /**
   * The line information associated with the source.
   */
  private LineInfo lineInfo;

  /**
   * The compilation unit that was produced by parsing the source.
   */
  private CompilationUnit unit;

  /**
   * A flag indicating whether the source contains a 'part of' directive.
   */
  private boolean containsPartOfDirective = false;

  /**
   * A flag indicating whether the source contains any directive other than a 'part of' directive.
   */
  private boolean containsNonPartOfDirective = false;

  /**
   * A set containing the sources referenced by 'export' directives.
   */
  private HashSet<Source> exportedSources = new HashSet<Source>();

  /**
   * A set containing the sources referenced by 'import' directives.
   */
  private HashSet<Source> importedSources = new HashSet<Source>();

  /**
   * A set containing the sources referenced by 'part' directives.
   */
  private HashSet<Source> includedSources = new HashSet<Source>();

  /**
   * The errors that were produced by scanning and parsing the source.
   */
  private AnalysisError[] errors = AnalysisError.NO_ERRORS;

  /**
   * Initialize a newly created task to perform analysis within the given context.
   * 
   * @param context the context in which the task is to be performed
   * @param source the source to be parsed
   * @param modificationTime the time at which the contents of the source were last modified
   * @param tokenStream the head of the token stream used for parsing
   * @param lineInfo the line information associated with the source
   */
  public ParseDartTask(InternalAnalysisContext context, Source source, long modificationTime,
      Token tokenStream, LineInfo lineInfo) {
    super(context);
    this.source = source;
    this.modificationTime = modificationTime;
    this.tokenStream = tokenStream;
    this.lineInfo = lineInfo;
  }

  @Override
  public <E> E accept(AnalysisTaskVisitor<E> visitor) throws AnalysisException {
    return visitor.visitParseDartTask(this);
  }

  /**
   * Return the compilation unit that was produced by parsing the source, or {@code null} if the
   * task has not yet been performed or if an exception occurred.
   * 
   * @return the compilation unit that was produced by parsing the source
   */
  public CompilationUnit getCompilationUnit() {
    return unit;
  }

  /**
   * Return the errors that were produced by scanning and parsing the source, or an empty array if
   * the task has not yet been performed or if an exception occurred.
   * 
   * @return the errors that were produced by scanning and parsing the source
   */
  public AnalysisError[] getErrors() {
    return errors;
  }

  /**
   * Return an array containing the sources referenced by 'export' directives, or an empty array if
   * the task has not yet been performed or if an exception occurred.
   * 
   * @return an array containing the sources referenced by 'export' directives
   */
  public Source[] getExportedSources() {
    return toArray(exportedSources);
  }

  /**
   * Return an array containing the sources referenced by 'import' directives, or an empty array if
   * the task has not yet been performed or if an exception occurred.
   * 
   * @return an array containing the sources referenced by 'import' directives
   */
  public Source[] getImportedSources() {
    return toArray(importedSources);
  }

  /**
   * Return an array containing the sources referenced by 'part' directives, or an empty array if
   * the task has not yet been performed or if an exception occurred.
   * 
   * @return an array containing the sources referenced by 'part' directives
   */
  public Source[] getIncludedSources() {
    return toArray(includedSources);
  }

  /**
   * Return the line information associated with the source.
   * 
   * @return the line information associated with the source
   */
  public LineInfo getLineInfo() {
    return lineInfo;
  }

  /**
   * Return the time at which the contents of the source that was parsed were last modified, or a
   * negative value if the task has not yet been performed or if an exception occurred.
   * 
   * @return the time at which the contents of the source that was parsed were last modified
   */
  public long getModificationTime() {
    return modificationTime;
  }

  /**
   * Return the source that is to be parsed.
   * 
   * @return the source to be parsed
   */
  public Source getSource() {
    return source;
  }

  /**
   * Return {@code true} if the source contains any directive other than a 'part of' directive, or
   * {@code false} if the task has not yet been performed or if an exception occurred.
   * 
   * @return {@code true} if the source contains any directive other than a 'part of' directive
   */
  public boolean hasNonPartOfDirective() {
    return containsNonPartOfDirective;
  }

  /**
   * Return {@code true} if the source contains a 'part of' directive, or {@code false} if the task
   * has not yet been performed or if an exception occurred.
   * 
   * @return {@code true} if the source contains a 'part of' directive
   */
  public boolean hasPartOfDirective() {
    return containsPartOfDirective;
  }

  @Override
  protected String getTaskDescription() {
    if (source == null) {
      return "parse as dart null source";
    }
    return "parse as dart " + source.getFullName();
  }

  @Override
  protected void internalPerform() throws AnalysisException {
    //
    // Then parse the token stream.
    //
    TimeCounterHandle timeCounterParse = PerformanceStatistics.parse.start();
    try {
      final RecordingErrorListener errorListener = new RecordingErrorListener();
      Parser parser = new Parser(source, errorListener);
      AnalysisOptions options = getContext().getAnalysisOptions();
      parser.setParseFunctionBodies(options.getAnalyzeFunctionBodies());
      parser.setParseAsync(options.getEnableAsync());
      parser.setParseDeferredLibraries(options.getEnableDeferredLoading());
      parser.setParseEnum(options.getEnableEnum());
      unit = parser.parseCompilationUnit(tokenStream);
      unit.setLineInfo(lineInfo);
      AnalysisContext analysisContext = getContext();
      for (Directive directive : unit.getDirectives()) {
        if (directive instanceof PartOfDirective) {
          containsPartOfDirective = true;
        } else {
          containsNonPartOfDirective = true;
          if (directive instanceof UriBasedDirective) {
            Source referencedSource = resolveDirective(
                analysisContext,
                source,
                (UriBasedDirective) directive,
                errorListener);
            if (referencedSource != null) {
              if (directive instanceof ExportDirective) {
                exportedSources.add(referencedSource);
              } else if (directive instanceof ImportDirective) {
                importedSources.add(referencedSource);
              } else if (directive instanceof PartDirective) {
                if (!referencedSource.equals(source)) {
                  includedSources.add(referencedSource);
                }
              } else {
                throw new AnalysisException(getClass().getSimpleName() + " failed to handle a "
                    + directive.getClass().getName());
              }
            }
          }
        }
      }
      errors = errorListener.getErrorsForSource(source);
    } finally {
      timeCounterParse.stop();
    }
  }

  /**
   * Efficiently convert the given set of sources to an array.
   * 
   * @param sources the set to be converted
   * @return an array containing all of the sources in the given set
   */
  private Source[] toArray(HashSet<Source> sources) {
    int size = sources.size();
    if (size == 0) {
      return Source.EMPTY_ARRAY;
    }
    return sources.toArray(new Source[size]);
  }
}
