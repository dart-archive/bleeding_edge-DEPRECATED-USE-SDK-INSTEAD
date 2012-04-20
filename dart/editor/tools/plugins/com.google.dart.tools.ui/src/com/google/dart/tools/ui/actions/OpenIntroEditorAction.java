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
package com.google.dart.tools.ui.actions;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.intro.IntroEditor;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

/**
 * Opens {@link IntroEditor}.
 */
public class OpenIntroEditorAction extends AbstractInstrumentedAction {

  public OpenIntroEditorAction() {
    super("&Welcome Page");

    setImageDescriptor(DartToolsPlugin.findImageDescriptor("icons/dart_16_16.gif"));
  }

  @Override
  public void run() {
    EmitInstrumentationCommand();
    try {
      IDE.openEditor(DartToolsPlugin.getActivePage(), IntroEditor.INPUT, IntroEditor.ID);
    } catch (PartInitException e) {
      DartToolsPlugin.log(e);
    }
  }
}
