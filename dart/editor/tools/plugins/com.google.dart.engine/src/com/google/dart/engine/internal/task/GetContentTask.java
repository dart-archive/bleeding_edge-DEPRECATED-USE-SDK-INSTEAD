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
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.context.TimestampedData;
import com.google.dart.engine.source.Source;

/**
 * Instances of the class {@code GetContentTask} get the contents of a source.
 */
public class GetContentTask extends AnalysisTask {
  /**
   * The source to be read.
   */
  private Source source;

  /**
   * A flag indicating whether this task is complete.
   */
  private boolean complete = false;

  /**
   * The contents of the source.
   */
  private CharSequence content;

  /**
   * The time at which the contents of the source were last modified.
   */
  private long modificationTime = -1L;

  /**
   * Initialize a newly created task to perform analysis within the given context.
   * 
   * @param context the context in which the task is to be performed
   * @param source the source to be parsed
   * @param contentData the time-stamped contents of the source
   */
  public GetContentTask(InternalAnalysisContext context, Source source) {
    super(context);
    if (source == null) {
      throw new IllegalArgumentException("Cannot get contents of null source");
    }
    this.source = source;
  }

  @Override
  public <E> E accept(AnalysisTaskVisitor<E> visitor) throws AnalysisException {
    return visitor.visitGetContentTask(this);
  }

  /**
   * Return the contents of the source, or {@code null} if the task has not completed or if there
   * was an exception while getting the contents.
   * 
   * @return the contents of the source
   */
  public CharSequence getContent() {
    return content;
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
   * Return {@code true} if this task is complete. Unlike most tasks, this task is allowed to be
   * visited more than once in order to support asynchronous IO. If the task is not complete when it
   * is visited synchronously as part of the {@link AnalysisTask#perform(AnalysisTaskVisitor)}
   * method, it will be visited again, using the same visitor, when the IO operation has been
   * performed.
   * 
   * @return {@code true} if this task is complete
   */
  public boolean isComplete() {
    return complete;
  }

  @Override
  protected String getTaskDescription() {
    return "get contents of " + source.getFullName();
  }

  @Override
  protected void internalPerform() throws AnalysisException {
    complete = true;
    try {
      TimestampedData<CharSequence> data = getContext().getContents(source);
      content = data.getData();
      modificationTime = data.getModificationTime();
    } catch (Throwable exception) {
      throw new AnalysisException("Could not get contents of " + source, exception);
    }
  }
}
