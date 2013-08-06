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

package com.google.dart.ui.test;

import org.eclipse.jface.dialogs.ErrorDialog;

/**
 * A class used to set and restore the Eclipse headless state.
 */
public class HeadlessTestManager {
  private boolean oldAutomatedValue;

  public HeadlessTestManager() {

  }

  public void install() {
    oldAutomatedValue = ErrorDialog.AUTOMATED_MODE;
    ErrorDialog.AUTOMATED_MODE = true;
  }

  public void uninstall() {
    ErrorDialog.AUTOMATED_MODE = oldAutomatedValue;
  }

}
