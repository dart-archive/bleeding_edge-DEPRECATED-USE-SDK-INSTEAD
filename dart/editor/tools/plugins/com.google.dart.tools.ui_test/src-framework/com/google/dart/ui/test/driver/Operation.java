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
package com.google.dart.ui.test.driver;

import com.google.dart.ui.test.util.UiContext;

/**
 * The simple and quick UI operation to run once it is ready.
 */
public abstract class Operation {
  /**
   * Checks if system under test is in the right state, so we can run this operation. "Right state"
   * may mean different thing depending on the operation - dialog opened, action enabled, etc.
   */
  public abstract boolean isReady(UiContext context) throws Exception;

  /**
   * This method is invoked if by some reason {@link #run} caused an exception.
   */
  public void onError(UiContext context) throws Exception {
  }

  /**
   * Performs some action. This action should be quick and should not block the UI.
   */
  public abstract void run(UiContext context) throws Exception;
}
