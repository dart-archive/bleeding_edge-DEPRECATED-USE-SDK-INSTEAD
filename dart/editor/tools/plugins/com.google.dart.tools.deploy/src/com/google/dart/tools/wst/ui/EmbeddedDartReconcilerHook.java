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
package com.google.dart.tools.wst.ui;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.ui.internal.text.dart.DartReconcilingEditor;

import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusListener;

public class EmbeddedDartReconcilerHook implements DartReconcilingEditor {

  @Override
  public void addViewerDisposeListener(DisposeListener listener) {
  }

  @Override
  public void addViewerFocusListener(FocusListener listener) {
  }

  @Override
  public void applyCompilationUnitElement(CompilationUnit unit) {
  }

  @Override
  public AnalysisContext getInputAnalysisContext() {
    return null;
  }

  @Override
  public Project getInputProject() {
    return null;
  }

  @Override
  public Source getInputSource() {
    return null;
  }

  @Override
  public String getTitle() {
    return null;
  }

}
