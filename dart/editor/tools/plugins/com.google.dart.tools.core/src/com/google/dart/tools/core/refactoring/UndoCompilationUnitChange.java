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
package com.google.dart.tools.core.refactoring;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.util.Messages;
import com.google.dart.tools.core.model.CompilationUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ContentStamp;
import org.eclipse.ltk.core.refactoring.UndoTextFileChange;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.text.edits.UndoEdit;

/* package */class UndoCompilationUnitChange extends UndoTextFileChange {

  static IFile getFile(CompilationUnit unit) throws CoreException {
    IFile file = (IFile) unit.getResource();
    if (file == null) {
      String message = Messages.bind(
          Messages.change_library_has_no_file,
          TextProcessor.process(unit.getElementName()));
      throw new CoreException(new Status(IStatus.ERROR, DartCore.PLUGIN_ID, message));
    }
    return file;
  }

  private CompilationUnit fCUnit;

  public UndoCompilationUnitChange(String name, CompilationUnit unit, UndoEdit undo,
      ContentStamp stampToRestore, int saveMode) throws CoreException {
    super(name, getFile(unit), undo, stampToRestore, saveMode);
    fCUnit = unit;
  }

  @Override
  public Object getModifiedElement() {
    return fCUnit;
  }

  @Override
  public Change perform(IProgressMonitor pm) throws CoreException {
    pm.beginTask("", 2); //$NON-NLS-1$
    fCUnit.becomeWorkingCopy(new SubProgressMonitor(pm, 1));
    try {
      return super.perform(new SubProgressMonitor(pm, 1));
    } finally {
      fCUnit.discardWorkingCopy();
    }
  }

  @Override
  protected Change createUndoChange(UndoEdit edit, ContentStamp stampToRestore)
      throws CoreException {
    return new UndoCompilationUnitChange(getName(), fCUnit, edit, stampToRestore, getSaveMode());
  }
}
