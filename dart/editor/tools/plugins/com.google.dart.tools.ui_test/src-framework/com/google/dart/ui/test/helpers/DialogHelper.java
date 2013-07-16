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

package com.google.dart.ui.test.helpers;

import com.google.dart.ui.test.util.UiContext;

import org.eclipse.swt.widgets.Button;

import static org.junit.Assert.assertTrue;

/**
 * Helper for testing dialogs.
 */
public class DialogHelper {
  protected final UiContext context;

  public DialogHelper(UiContext context) {
    this.context = context;
  }

  public void closeCancel() {
    Button button = context.findButton("Cancel");
    assertTrue(button.isEnabled());
    context.clickButton(button);
  }

  public void closeOK() {
    Button button = context.findButton("OK");
    assertTrue(button.isEnabled());
    context.clickButton(button);
  }
}
