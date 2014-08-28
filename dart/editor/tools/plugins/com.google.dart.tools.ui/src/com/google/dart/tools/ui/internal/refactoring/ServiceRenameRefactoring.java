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

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.services.refactoring.RenameRefactoring;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.internal.corext.refactoring.tagging.INameUpdating;

import static com.google.dart.tools.ui.internal.refactoring.ServiceUtils_OLD.toLTK;

/**
 * LTK wrapper around Engine Services {@link RenameRefactoring}.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public class ServiceRenameRefactoring extends ServiceRefactoring implements INameUpdating {
  private final RenameRefactoring refactoring;

  public ServiceRenameRefactoring(RenameRefactoring refactoring) {
    super(refactoring);
    this.refactoring = refactoring;
  }

  @Override
  public org.eclipse.ltk.core.refactoring.RefactoringStatus checkNewElementName(String newName) {
    RefactoringStatus status = refactoring.checkNewName(newName);
    status = status.escalateErrorToFatal();
    return toLTK(status);
  }

  @Override
  public String getCurrentElementName() {
    return refactoring.getCurrentName();
  }

  @Override
  public String getNewElementName() {
    return refactoring.getNewName();
  }

  @Override
  public void setNewElementName(String newName) {
    refactoring.setNewName(newName);
  }

  /**
   * @return {@code true} if given {@link Source} may be affected by this refactoring, so we should
   *         warn user about it.
   */
  @Override
  protected boolean shouldReportUnsafeRefactoringSource(AnalysisContext context, Source source) {
    return refactoring.shouldReportUnsafeRefactoringSource(context, source);
  }
}
