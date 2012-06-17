/*
 * Copyright 2012 Dart project authors.
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
package com.google.dart.tools.core.analysis;

/**
 * An analysis task. Not intended to be subclassed by clients.
 */
public abstract class Task {

  /**
   * Answer <code>true</code> if this task is an analysis task and does not have a callback. The
   * assumption is that analysis tasks with explicit callbacks are related to user requests and thus
   * are not considered "background" analysis.
   */
  abstract boolean isBackgroundAnalysis();

  /**
   * Answer <code>true</code> if this task removes cached information and thus should be executed
   * before other tasks
   */
  abstract boolean isPriority();

  /**
   * Perform the task. This is executed in the background thread and may modify any aspect of the
   * analysis model or cached elements.
   */
  abstract void perform();
}
