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

import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.context.PerformanceStatistics;
import com.google.dart.engine.internal.context.RecordingErrorListener;
import com.google.dart.engine.scanner.CharSequenceReader;
import com.google.dart.engine.scanner.Scanner;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.general.TimeCounter.TimeCounterHandle;
import com.google.dart.engine.utilities.source.LineInfo;

/**
 * Instances of the class {@code ScanDartTask} scan a specific source as a Dart file.
 */
public class ScanDartTask extends AnalysisTask {
  /**
   * The source to be scanned.
   */
  private Source source;

  /**
   * The time at which the contents of the source were last modified.
   */
  private long modificationTime;

  /**
   * The contents of the source.
   */
  private CharSequence content;

  /**
   * The token stream that was produced by scanning the source.
   */
  private Token tokenStream;

  /**
   * The line information that was produced.
   */
  private LineInfo lineInfo;

  /**
   * The errors that were produced by scanning the source.
   */
  private AnalysisError[] errors = AnalysisError.NO_ERRORS;

  /**
   * Initialize a newly created task to perform analysis within the given context.
   * 
   * @param context the context in which the task is to be performed
   * @param source the source to be parsed
   * @param modificationTime the time at which the contents of the source were last modified
   * @param content the contents of the source
   */
  public ScanDartTask(InternalAnalysisContext context, Source source, long modificationTime,
      CharSequence content) {
    super(context);
    this.source = source;
    this.modificationTime = modificationTime;
    this.content = content;
  }

  @Override
  public <E> E accept(AnalysisTaskVisitor<E> visitor) throws AnalysisException {
    return visitor.visitScanDartTask(this);
  }

  /**
   * Return the errors that were produced by scanning the source, or {@code null} if the task has
   * not yet been performed or if an exception occurred.
   * 
   * @return the errors that were produced by scanning the source
   */
  public AnalysisError[] getErrors() {
    return errors;
  }

  /**
   * Return the line information that was produced, or {@code null} if the task has not yet been
   * performed or if an exception occurred.
   * 
   * @return the line information that was produced
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
   * Return the source that is to be scanned.
   * 
   * @return the source to be scanned
   */
  public Source getSource() {
    return source;
  }

  /**
   * Return the token stream that was produced by scanning the source, or {@code null} if the task
   * has not yet been performed or if an exception occurred.
   * 
   * @return the token stream that was produced by scanning the source
   */
  public Token getTokenStream() {
    return tokenStream;
  }

  @Override
  protected String getTaskDescription() {
    if (source == null) {
      return "scan as dart null source";
    }
    return "scan as dart " + source.getFullName();
  }

  @Override
  protected void internalPerform() throws AnalysisException {
    final RecordingErrorListener errorListener = new RecordingErrorListener();
    TimeCounterHandle timeCounterScan = PerformanceStatistics.scan.start();
    try {
      Scanner scanner = new Scanner(source, new CharSequenceReader(content), errorListener);
      scanner.setPreserveComments(getContext().getAnalysisOptions().getPreserveComments());
      tokenStream = scanner.tokenize();
      lineInfo = new LineInfo(scanner.getLineStarts());
      errors = errorListener.getErrorsForSource(source);
    } catch (Exception exception) {
      throw new AnalysisException(exception);
    } finally {
      timeCounterScan.stop();
    }
  }
}
