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
package com.google.dart.tools.ui.internal.refactoring.actions;

import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Event;

/**
 * @coverage dart.editor.ui.refactoring.ui
 */
public interface RenameDartElementAction_I {
  void doRun(IStructuredSelection selection, Event event, InstrumentationBuilder instrumentation);

  void doRun(ITextSelection selection, Event event, InstrumentationBuilder instrumentation);

  boolean isEnabled();

  void selectionChanged(SelectionChangedEvent event);

  void setText(String text);

  void update(ISelection selection);
}
