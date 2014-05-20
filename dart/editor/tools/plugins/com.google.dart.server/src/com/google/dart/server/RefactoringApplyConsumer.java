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
package com.google.dart.server;

import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.status.RefactoringStatus;

/**
 * The interface {@code RefactoringApplyConsumer} defines the behavior of objects that consume a
 * status of the final validation and a {@link Change} that will apply the refactoring.
 * 
 * @coverage dart.server
 */
public interface RefactoringApplyConsumer extends Consumer {
  /**
   * The final validation has been performed and a {@link Change} computed.
   * 
   * @param status the status of the validation
   * @param change the {@link Change} to apply (if user agree after checking the status)
   */
  public void computed(RefactoringStatus status, Change change);
}
