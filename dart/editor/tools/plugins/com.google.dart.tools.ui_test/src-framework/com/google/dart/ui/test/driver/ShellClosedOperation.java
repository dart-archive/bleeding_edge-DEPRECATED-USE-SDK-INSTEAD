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

import org.eclipse.swt.widgets.Shell;

/**
 * The {@link Operation} that waits for the {@link Shell} closing.
 */
public class ShellClosedOperation extends Operation {
  private final String shellText;

  public ShellClosedOperation(String shellText) {
    this.shellText = shellText;
  }

  @Override
  public boolean isReady(UiContext context) throws Exception {
    Shell shell = context.findShell(shellText);
    return shell == null || shell.isDisposed();
  }

  @Override
  public void run(UiContext context) throws Exception {
  }
}
