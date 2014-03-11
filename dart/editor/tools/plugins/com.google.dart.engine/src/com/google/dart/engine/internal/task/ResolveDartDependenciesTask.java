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
import com.google.dart.engine.ast.StringInterpolation;
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.ast.UriBasedDirective;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.context.PerformanceStatistics;
import com.google.dart.engine.internal.context.RecordingErrorListener;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.general.TimeCounter.TimeCounterHandle;
import com.google.dart.engine.utilities.io.UriUtilities;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;

/**
 * Instances of the class {@code ResolveDartDependenciesTask} resolve the import, export, and part
 * directives in a single source.
 */
public class ResolveDartDependenciesTask extends AnalysisTask {
  /**
   * The source containing the directives to be resolved.
   */
  private Source source;

  /**
   * The time at which the contents of the source were last modified.
   */
  private long modificationTime;

  /**
   * The compilation unit used to resolve the dependencies.
   */
  private CompilationUnit unit;

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
   * The errors that were produced by resolving the directives.
   */
  private AnalysisError[] errors = AnalysisError.NO_ERRORS;

  /**
   * The prefix of a URI using the {@code dart-ext} scheme to reference a native code library.
   */
  private static final String DART_EXT_SCHEME = "dart-ext:";

  /**
   * Initialize a newly created task to perform analysis within the given context.
   * 
   * @param context the context in which the task is to be performed
   * @param source the source to be parsed
   * @param modificationTime the time at which the contents of the source were last modified
   * @param unit the compilation unit used to resolve the dependencies
   */
  public ResolveDartDependenciesTask(InternalAnalysisContext context, Source source,
      long modificationTime, CompilationUnit unit) {
    super(context);
    this.source = source;
    this.modificationTime = modificationTime;
    this.unit = unit;
  }

  @Override
  public <E> E accept(AnalysisTaskVisitor<E> visitor) throws AnalysisException {
    return visitor.visitResolveDartDependenciesTask(this);
  }

  /**
   * Return the errors that were produced by resolving the directives, or an empty array if the task
   * has not yet been performed or if an exception occurred.
   * 
   * @return the errors that were produced by resolving the directives.
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

  @Override
  protected String getTaskDescription() {
    if (source == null) {
      return "resolve dart dependencies null source";
    }
    return "resolve dart dependencies " + source.getFullName();
  }

  @Override
  protected void internalPerform() throws AnalysisException {
    TimeCounterHandle timeCounterParse = PerformanceStatistics.parse.start();
    try {
      RecordingErrorListener errorListener = new RecordingErrorListener();
      for (Directive directive : unit.getDirectives()) {
        if (directive instanceof ExportDirective) {
          Source exportSource = resolveSource(source, (ExportDirective) directive, errorListener);
          if (exportSource != null) {
            exportedSources.add(exportSource);
          }
        } else if (directive instanceof ImportDirective) {
          Source importSource = resolveSource(source, (ImportDirective) directive, errorListener);
          if (importSource != null) {
            importedSources.add(importSource);
          }
        } else if (directive instanceof PartDirective) {
          Source partSource = resolveSource(source, (PartDirective) directive, errorListener);
          if (partSource != null) {
            includedSources.add(partSource);
          }
        }
      }
      errors = errorListener.getErrors();
    } finally {
      timeCounterParse.stop();
    }
  }

  /**
   * Return the result of resolving the URI of the given URI-based directive against the URI of the
   * given library, or {@code null} if the URI is not valid.
   * 
   * @param librarySource the source representing the library containing the directive
   * @param directive the directive which URI should be resolved
   * @param errorListener the error listener to which errors should be reported
   * @return the result of resolving the URI against the URI of the library
   */
  private Source resolveSource(Source librarySource, UriBasedDirective directive,
      AnalysisErrorListener errorListener) {
    StringLiteral uriLiteral = directive.getUri();
    if (uriLiteral instanceof StringInterpolation) {
      errorListener.onError(new AnalysisError(
          librarySource,
          uriLiteral.getOffset(),
          uriLiteral.getLength(),
          CompileTimeErrorCode.URI_WITH_INTERPOLATION));
      return null;
    }
    String uriContent = uriLiteral.getStringValue().trim();
    directive.setUriContent(uriContent);
    if (directive instanceof ImportDirective && uriContent.startsWith(DART_EXT_SCHEME)) {
      return null;
    }
    try {
      String encodedUriContent = UriUtilities.encode(uriContent);
      new URI(encodedUriContent);
      AnalysisContext analysisContext = getContext();
      Source source = analysisContext.getSourceFactory().resolveUri(
          librarySource,
          encodedUriContent);
      if (!analysisContext.exists(source)) {
        errorListener.onError(new AnalysisError(
            librarySource,
            uriLiteral.getOffset(),
            uriLiteral.getLength(),
            CompileTimeErrorCode.URI_DOES_NOT_EXIST,
            uriContent));
      }
      directive.setSource(source);
      return source;
    } catch (URISyntaxException exception) {
      errorListener.onError(new AnalysisError(
          librarySource,
          uriLiteral.getOffset(),
          uriLiteral.getLength(),
          CompileTimeErrorCode.INVALID_URI,
          uriContent));
    }
    return null;
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
