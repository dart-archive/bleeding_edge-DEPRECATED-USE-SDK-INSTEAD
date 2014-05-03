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
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.context.PerformanceStatistics;
import com.google.dart.engine.internal.context.RecordingErrorListener;
import com.google.dart.engine.internal.context.ResolvableCompilationUnit;
import com.google.dart.engine.internal.error.ErrorReporter;
import com.google.dart.engine.internal.resolver.DeclarationResolver;
import com.google.dart.engine.internal.resolver.InheritanceManager;
import com.google.dart.engine.internal.resolver.ResolverVisitor;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.internal.resolver.TypeResolverVisitor;
import com.google.dart.engine.internal.verifier.ConstantVerifier;
import com.google.dart.engine.internal.verifier.ErrorVerifier;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.general.TimeCounter.TimeCounterHandle;

/**
 * Instances of the class {@code ResolveDartUnitTask} resolve a single Dart file based on a existing
 * element model.
 */
public class ResolveDartUnitTask extends AnalysisTask {
  /**
   * The source that is to be resolved.
   */
  private Source source;

  /**
   * The element model for the library containing the source.
   */
  private LibraryElement libraryElement;

  /**
   * The time at which the contents of the source were last modified.
   */
  private long modificationTime = -1L;

  /**
   * The compilation unit that was resolved by this task.
   */
  private CompilationUnit resolvedUnit;

  /**
   * Initialize a newly created task to perform analysis within the given context.
   * 
   * @param context the context in which the task is to be performed
   * @param source the source to be parsed
   * @param libraryElement the element model for the library containing the source
   */
  public ResolveDartUnitTask(InternalAnalysisContext context, Source source,
      LibraryElement libraryElement) {
    super(context);
    this.source = source;
    this.libraryElement = libraryElement;
  }

  @Override
  public <E> E accept(AnalysisTaskVisitor<E> visitor) throws AnalysisException {
    return visitor.visitResolveDartUnitTask(this);
  }

  /**
   * Return the source for the library containing the source that is to be resolved.
   * 
   * @return the source for the library containing the source that is to be resolved
   */
  public Source getLibrarySource() {
    return libraryElement.getSource();
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
   * Return the compilation unit that was resolved by this task.
   * 
   * @return the compilation unit that was resolved by this task
   */
  public CompilationUnit getResolvedUnit() {
    return resolvedUnit;
  }

  /**
   * Return the source that is to be resolved.
   * 
   * @return the source to be resolved
   */
  public Source getSource() {
    return source;
  }

  @Override
  protected String getTaskDescription() {
    Source librarySource = libraryElement.getSource();
    if (librarySource == null) {
      return "resolve unit null source";
    }
    return "resolve unit " + librarySource.getFullName();
  }

  @Override
  protected void internalPerform() throws AnalysisException {
    TypeProvider typeProvider = ((InternalAnalysisContext) libraryElement.getContext()).getTypeProvider();
    ResolvableCompilationUnit resolvableUnit = getContext().computeResolvableCompilationUnit(source);
    modificationTime = resolvableUnit.getModificationTime();
    CompilationUnit unit = resolvableUnit.getCompilationUnit();
    if (unit == null) {
      throw new AnalysisException(
          "Internal error: computeResolvableCompilationUnit returned a value without a parsed Dart unit");
    }
    //
    // Resolve names in declarations.
    //
    new DeclarationResolver().resolve(unit, find(libraryElement, source));
    //
    // Resolve the type names.
    //
    RecordingErrorListener errorListener = new RecordingErrorListener();
    TypeResolverVisitor typeResolverVisitor = new TypeResolverVisitor(
        libraryElement,
        source,
        typeProvider,
        errorListener);
    unit.accept(typeResolverVisitor);
    //
    // Resolve the rest of the structure
    //
    InheritanceManager inheritanceManager = new InheritanceManager(libraryElement);
    ResolverVisitor resolverVisitor = new ResolverVisitor(
        libraryElement,
        source,
        typeProvider,
        inheritanceManager,
        errorListener);
    unit.accept(resolverVisitor);
    //
    // Perform additional error checking.
    //
    TimeCounterHandle counterHandleErrors = PerformanceStatistics.errors.start();
    try {
      ErrorReporter errorReporter = new ErrorReporter(errorListener, source);
      ErrorVerifier errorVerifier = new ErrorVerifier(
          errorReporter,
          libraryElement,
          typeProvider,
          inheritanceManager);
      unit.accept(errorVerifier);

      ConstantVerifier constantVerifier = new ConstantVerifier(
          errorReporter,
          libraryElement,
          typeProvider);
      unit.accept(constantVerifier);
    } finally {
      counterHandleErrors.stop();
    }
    //
    // Capture the results.
    //
    resolvedUnit = unit;
  }

  /**
   * Search the compilation units that are part of the given library and return the element
   * representing the compilation unit with the given source. Return {@code null} if there is no
   * such compilation unit.
   * 
   * @param libraryElement the element representing the library being searched through
   * @param unitSource the source for the compilation unit whose element is to be returned
   * @return the element representing the compilation unit
   */
  private CompilationUnitElement find(LibraryElement libraryElement, Source unitSource) {
    CompilationUnitElement element = libraryElement.getDefiningCompilationUnit();
    if (element.getSource().equals(unitSource)) {
      return element;
    }
    for (CompilationUnitElement partElement : libraryElement.getParts()) {
      if (partElement.getSource().equals(unitSource)) {
        return partElement;
      }
    }
    return null;
  }
}
