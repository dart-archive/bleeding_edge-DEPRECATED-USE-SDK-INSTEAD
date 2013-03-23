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

package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.services.refactoring.InlineMethodRefactoring;
import com.google.dart.engine.services.refactoring.InlineMethodRefactoring.Mode;
import com.google.dart.tools.internal.corext.refactoring.code.InlineMethodRefactoring_I;

/**
 * LTK wrapper around Engine Services {@link InlineMethodRefactoring}.
 */
public class ServiceInlineMethodRefactoring extends ServiceRefactoring implements
    InlineMethodRefactoring_I {
  private final InlineMethodRefactoring refactoring;

  public ServiceInlineMethodRefactoring(InlineMethodRefactoring refactoring) {
    super(refactoring);
    this.refactoring = refactoring;
  }

  @Override
  public boolean canEnableDeleteSource() {
    return refactoring.canDeleteSource();
  }

  @Override
  public Mode getInitialMode() {
    return refactoring.getInitialMode();
  }

  @Override
  public String getMethodLabel() {
    ExecutableElement element = refactoring.getElement();
    if (element instanceof FunctionElement) {
      return element.getName();
    }
    return element.getEnclosingElement().getName() + "." + element.getName();
  }

  @Override
  public void setCurrentMode(Mode mode) {
    refactoring.setCurrentMode(mode);
  }

  @Override
  public void setDeleteSource(boolean delete) {
    refactoring.setDeleteSource(delete);
  }
}
