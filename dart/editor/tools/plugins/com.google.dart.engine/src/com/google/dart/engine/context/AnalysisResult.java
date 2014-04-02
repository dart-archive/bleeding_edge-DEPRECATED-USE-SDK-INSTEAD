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
package com.google.dart.engine.context;

/**
 * Instances of the class {@code AnalysisResult}
 */
public class AnalysisResult {
  /**
   * The change notices associated with this result, or {@code null} if there were no changes and
   * there is no more work to be done.
   */
  private ChangeNotice[] notices;

  /**
   * The number of milliseconds required to determine which task was to be performed.
   */
  private long getTime;

  /**
   * The name of the class of the task that was performed.
   */
  private String taskClassName;

  /**
   * The number of milliseconds required to perform the task.
   */
  private long performTime;

  /**
   * Initialize a newly created analysis result to have the given values.
   * 
   * @param notices the change notices associated with this result
   * @param getTime the number of milliseconds required to determine which task was to be performed
   * @param taskClassName the name of the class of the task that was performed
   * @param performTime the number of milliseconds required to perform the task
   */
  public AnalysisResult(ChangeNotice[] notices, long getTime, String taskClassName, long performTime) {
    this.notices = notices;
    this.getTime = getTime;
    this.taskClassName = taskClassName;
    this.performTime = performTime;
  }

  /**
   * Return the change notices associated with this result, or {@code null} if there were no changes
   * and there is no more work to be done.
   * 
   * @return the change notices associated with this result
   */
  public ChangeNotice[] getChangeNotices() {
    return notices;
  }

  /**
   * Return the number of milliseconds required to determine which task was to be performed.
   * 
   * @return the number of milliseconds required to determine which task was to be performed
   */
  public long getGetTime() {
    return getTime;
  }

  /**
   * Return the number of milliseconds required to perform the task, or zero (-1L) if no task was
   * performed.
   * 
   * @return the number of milliseconds required to perform the task
   */
  public long getPerformTime() {
    return performTime;
  }

  /**
   * Return the name of the class of the task that was performed, or {@code null} if no task was
   * performed.
   * 
   * @return the name of the class of the task that was performed
   */
  public String getTaskClassName() {
    return taskClassName;
  }

  /**
   * Return {@code true} if there is more to be performed after the task that was performed.
   * 
   * @return {@code true} if there is more to be performed after the task that was performed
   */
  public boolean hasMoreWork() {
    return notices != null;
  }
}
