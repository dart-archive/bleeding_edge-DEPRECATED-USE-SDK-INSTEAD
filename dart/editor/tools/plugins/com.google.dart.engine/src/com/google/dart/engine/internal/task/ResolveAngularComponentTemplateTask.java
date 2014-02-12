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
package com.google.dart.engine.internal.task;

import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.angular.AngularComponentElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.context.RecordingErrorListener;
import com.google.dart.engine.internal.context.ResolvableHtmlUnit;
import com.google.dart.engine.internal.element.angular.AngularApplication;
import com.google.dart.engine.internal.html.angular.AngularHtmlUnitResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.LineInfo;

/**
 * Instances of the class {@code ResolveAngularComponentTemplateTask} resolve HTML template
 * referenced by {@link AngularComponentElement}.
 */
public class ResolveAngularComponentTemplateTask extends AnalysisTask {
  /**
   * The {@link AngularComponentElement} to resolve template for.
   */
  private final AngularComponentElement component;

  /**
   * The Angular application to resolve in context of.
   */
  private final AngularApplication application;

  /**
   * The source to be resolved.
   */
  private final Source source;

  /**
   * The time at which the contents of the source were last modified.
   */
  private long modificationTime = -1L;

  /**
   * The {@link HtmlUnit} that was resolved by this task.
   */
  private HtmlUnit resolvedUnit;

  /**
   * The resolution errors that were discovered while resolving the source.
   */
  private AnalysisError[] resolutionErrors = AnalysisError.NO_ERRORS;

  /**
   * Initialize a newly created task to perform analysis within the given context.
   * 
   * @param context the context in which the task is to be performed
   * @param source the source to be resolved
   * @param component the component that uses this HTML template, not {@code null}
   * @param application the Angular application to resolve in context of
   */
  public ResolveAngularComponentTemplateTask(InternalAnalysisContext context, Source source,
      AngularComponentElement component, AngularApplication application) {
    super(context);
    this.source = source;
    this.component = component;
    this.application = application;
  }

  @Override
  public <E> E accept(AnalysisTaskVisitor<E> visitor) throws AnalysisException {
    return visitor.visitResolveAngularComponentTemplateTask(this);
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

  public AnalysisError[] getResolutionErrors() {
    return resolutionErrors;
  }

  /**
   * Return the {@link HtmlUnit} that was resolved by this task.
   * 
   * @return the {@link HtmlUnit} that was resolved by this task
   */
  public HtmlUnit getResolvedUnit() {
    return resolvedUnit;
  }

  /**
   * Return the source that was or is to be resolved.
   * 
   * @return the source was or is to be resolved
   */
  public Source getSource() {
    return source;
  }

  @Override
  protected String getTaskDescription() {
    return "resolving Angular template " + source;
  }

  @Override
  protected void internalPerform() throws AnalysisException {
    ResolvableHtmlUnit resolvableHtmlUnit = getContext().computeResolvableAngularComponentHtmlUnit(
        source);
    HtmlUnit unit = resolvableHtmlUnit.getCompilationUnit();
    if (unit == null) {
      throw new AnalysisException(
          "Internal error: computeResolvableHtmlUnit returned a value without a parsed HTML unit");
    }
    modificationTime = resolvableHtmlUnit.getModificationTime();
    // prepare for resolution
    RecordingErrorListener errorListener = new RecordingErrorListener();
    LineInfo lineInfo = getContext().getLineInfo(source);
    // do resolve
    if (application != null) {
      AngularHtmlUnitResolver resolver = new AngularHtmlUnitResolver(
          getContext(),
          errorListener,
          source,
          lineInfo,
          unit);
      resolver.resolveComponentTemplate(application, component);
      resolvedUnit = unit;
    }
    // remember errors
    resolutionErrors = errorListener.getErrors(source);
  }
}
