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

package com.google.dart.tools.ui.console;

import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.internal.console.ConsoleManager;

/**
 * A subclass of the standard ConsoleManager which sends additional notifications out when an
 * IConsole's content changes.
 */
@SuppressWarnings("restriction")
class ReplacementConsoleManager extends ConsoleManager {

  public ReplacementConsoleManager() {

  }

  @Override
  public void warnOfContentChange(IConsole console) {
    DartConsoleManager dartConsoleManager = DartConsoleManager.getManager();
    if (dartConsoleManager != null) {
      dartConsoleManager.warnOfContentChange(console);
    }

    super.warnOfContentChange(console);
  }

}
