/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.functions;

import com.google.dart.tools.ui.internal.text.functions.TypingRun.ChangeType;

/**
 * Listener for <code>TypingRun</code> events.
 */
public interface ITypingRunListener {
  /**
   * Called whenever a <code>TypingRun</code> is ended.
   * 
   * @param run the ended run
   * @param reason the type of change that caused the end of the run
   */
  void typingRunEnded(TypingRun run, ChangeType reason);

  /**
   * Called when a new <code>TypingRun</code> is started.
   * 
   * @param run the newly started run
   */
  void typingRunStarted(TypingRun run);
}
