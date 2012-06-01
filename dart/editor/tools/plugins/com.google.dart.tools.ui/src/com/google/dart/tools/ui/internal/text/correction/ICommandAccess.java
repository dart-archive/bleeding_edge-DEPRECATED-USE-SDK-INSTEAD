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
package com.google.dart.tools.ui.internal.text.correction;

/**
 * Correction proposals implement this interface to by invokable by a command. (e.g. keyboard
 * shortcut)
 * 
 * @coverage dart.editor.ui.correction
 */
public interface ICommandAccess {

  /**
   * Returns the id of the command that should invoke this correction proposal
   * 
   * @return the id of the command. This id must start with
   *         {@link CorrectionCommandInstaller#COMMAND_PREFIX} to be recognized as a correction
   *         command.
   */
  String getCommandId();

}
