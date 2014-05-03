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
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.ast.UriBasedDirective;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.context.PerformanceStatistics;
import com.google.dart.engine.internal.context.RecordingErrorListener;
import com.google.dart.engine.internal.error.ErrorReporter;
import com.google.dart.engine.internal.resolver.InheritanceManager;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.internal.verifier.ConstantVerifier;
import com.google.dart.engine.internal.verifier.ErrorVerifier;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.general.TimeCounter.TimeCounterHandle;

/**
 * Instances of the class {@code GenerateDartErrorsTask} generate errors and warnings for a single
 * Dart source.
 */
public class GenerateDartErrorsTask extends AnalysisTask {
  /**
   * Check each directive in the given compilation unit to see if the referenced source exists and
   * report an error if it does not.
   * 
   * @param context the context in which the library exists
   * @param librarySource the source representing the library containing the directives
   * @param unit the compilation unit containing the directives to be validated
   * @param errorListener the error listener to which errors should be reported
   */
  public static void validateDirectives(AnalysisContext context, Source librarySource,
      CompilationUnit unit, AnalysisErrorListener errorListener) {
    for (Directive directive : unit.getDirectives()) {
      if (directive instanceof UriBasedDirective) {
        validateReferencedSource(
            context,
            librarySource,
            (UriBasedDirective) directive,
            errorListener);
      }
    }
  }

  /**
   * Check the given directive to see if the referenced source exists and report an error if it does
   * not.
   * 
   * @param context the context in which the library exists
   * @param librarySource the source representing the library containing the directive
   * @param directive the directive to be verified
   * @param errorListener the error listener to which errors should be reported
   */
  public static void validateReferencedSource(AnalysisContext context, Source librarySource,
      UriBasedDirective directive, AnalysisErrorListener errorListener) {
    Source source = directive.getSource();
    if (source != null) {
      if (context.exists(source)) {
        return;
      }
    } else {
      // Don't report errors already reported by ParseDartTask#resolveDirective
      if (directive.validate() != null) {
        return;
      }
    }
    StringLiteral uriLiteral = directive.getUri();
    errorListener.onError(new AnalysisError(
        librarySource,
        uriLiteral.getOffset(),
        uriLiteral.getLength(),
        CompileTimeErrorCode.URI_DOES_NOT_EXIST,
        directive.getUriContent()));
  }

  /**
   * The source for which errors and warnings are to be produced.
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
   * The element model for the library containing the source.
   */
  private LibraryElement libraryElement;

  /**
   * The errors that were generated for the source.
   */
  private AnalysisError[] errors;

  /**
   * Initialize a newly created task to perform analysis within the given context.
   * 
   * @param context the context in which the task is to be performed
   * @param source the source for which errors and warnings are to be produced
   * @param modificationTime the time at which the contents of the source were last modified
   * @param unit the compilation unit used to resolve the dependencies
   * @param libraryElement the element model for the library containing the source
   */
  public GenerateDartErrorsTask(InternalAnalysisContext context, Source source,
      long modificationTime, CompilationUnit unit, LibraryElement libraryElement) {
    super(context);
    this.source = source;
    this.modificationTime = modificationTime;
    this.unit = unit;
    this.libraryElement = libraryElement;
  }

  @Override
  public <E> E accept(AnalysisTaskVisitor<E> visitor) throws AnalysisException {
    return visitor.visitGenerateDartErrorsTask(this);
  }

  /**
   * Return the errors that were generated for the source.
   * 
   * @return the errors that were generated for the source
   */
  public AnalysisError[] getErrors() {
    return errors;
  }

  /**
   * Return the element model for the library containing the source.
   * 
   * @return the element model for the library containing the source
   */
  public LibraryElement getLibraryElement() {
    return libraryElement;
  }

  /**
   * Return the time at which the contents of the source that was verified were last modified, or a
   * negative value if the task has not yet been performed or if an exception occurred.
   * 
   * @return the time at which the contents of the source that was verified were last modified
   */
  public long getModificationTime() {
    return modificationTime;
  }

  /**
   * Return the source for which errors and warnings are to be produced.
   * 
   * @return the source for which errors and warnings are to be produced
   */
  public Source getSource() {
    return source;
  }

  @Override
  protected String getTaskDescription() {
    return "generate errors and warnings for " + source.getFullName();
  }

  @Override
  protected void internalPerform() throws AnalysisException {
    TimeCounterHandle timeCounter = PerformanceStatistics.errors.start();
    try {
      RecordingErrorListener errorListener = new RecordingErrorListener();
      ErrorReporter errorReporter = new ErrorReporter(errorListener, source);
      TypeProvider typeProvider = getContext().getTypeProvider();
      //
      // Validate the directives
      //
      validateDirectives(getContext(), source, unit, errorListener);
      //
      // Use the ConstantVerifier to verify the use of constants. This needs to happen before using
      // the ErrorVerifier because some error codes need the computed constant values.
      //
      ConstantVerifier constantVerifier = new ConstantVerifier(
          errorReporter,
          libraryElement,
          typeProvider);
      unit.accept(constantVerifier);
      //
      // Use the ErrorVerifier to compute the rest of the errors.
      //
      ErrorVerifier errorVerifier = new ErrorVerifier(
          errorReporter,
          libraryElement,
          typeProvider,
          new InheritanceManager(libraryElement));
      unit.accept(errorVerifier);
      errors = errorListener.getErrorsForSource(source);
    } finally {
      timeCounter.stop();
    }
  }
}
