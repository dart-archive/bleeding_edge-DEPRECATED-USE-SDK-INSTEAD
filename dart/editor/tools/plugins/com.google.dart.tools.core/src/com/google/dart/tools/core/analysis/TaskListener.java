/*
 * Copyright (c) 2012, the Dart project authors.
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
 * This class will eventually replace {@link IdleListener} to report progress and idle events.
 */
public interface TaskListener extends IdleListener {

  /**
   * Called when the {@link TaskProcessor} is about to remove and perform a task from the queue .
   * 
   * @param toBeProcessed the number of tasks in the queue.
   */
  void processing(int toBeProcessed);
}
