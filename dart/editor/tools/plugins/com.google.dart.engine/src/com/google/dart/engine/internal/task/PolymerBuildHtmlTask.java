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
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.context.RecordingErrorListener;
import com.google.dart.engine.internal.html.polymer.PolymerHtmlUnitBuilder;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.LineInfo;

/**
 * Instances of the class {@code PolymerBuildHtmlTask} build Polymer specific elements.
 */
public class PolymerBuildHtmlTask extends AnalysisTask {
  /**
   * The source to build which Polymer HTML elements for.
   */
  private final Source source;

  /**
   * The time at which the contents of the source were last modified.
   */
  private final long modificationTime;

  /**
   * The line information associated with the source.
   */
  private final LineInfo lineInfo;

  /**
   * The HTML unit to be resolved.
   */
  private final HtmlUnit unit;

  /**
   * The resolution errors that were discovered while building elements.
   */
  private AnalysisError[] errors = AnalysisError.NO_ERRORS;

  /**
   * Initialize a newly created task to perform analysis within the given context.
   * 
   * @param context the context in which the task is to be performed
   * @param source the source to be resolved
   * @param modificationTime the time at which the contents of the source were last modified
   * @param lineInfo the line information associated with the source
   * @param unit the HTML unit to build Polymer elements for
   */
  public PolymerBuildHtmlTask(InternalAnalysisContext context, Source source,
      long modificationTime, LineInfo lineInfo, HtmlUnit unit) {
    super(context);
    this.source = source;
    this.modificationTime = modificationTime;
    this.lineInfo = lineInfo;
    this.unit = unit;
  }

  @Override
  public <E> E accept(AnalysisTaskVisitor<E> visitor) throws AnalysisException {
    return visitor.visitPolymerBuildHtmlTask(this);
  }

  public AnalysisError[] getErrors() {
    return errors;
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
   * Return the source that was or is to be created elements in.
   * 
   * @return the source was or is to be created elements in
   */
  public Source getSource() {
    return source;
  }

  @Override
  protected String getTaskDescription() {
    return "build Polymer elements " + source.getFullName();
  }

  @Override
  protected void internalPerform() throws AnalysisException {
    RecordingErrorListener errorListener = new RecordingErrorListener();
    PolymerHtmlUnitBuilder resolver = new PolymerHtmlUnitBuilder(
        getContext(),
        errorListener,
        source,
        lineInfo,
        unit);
    resolver.build();
    errors = errorListener.getErrorsForSource(source);
  }
}
