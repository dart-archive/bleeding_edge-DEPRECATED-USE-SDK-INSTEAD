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

import com.google.dart.tools.core.model.LibraryConfigurationFile;

import static com.google.dart.tools.core.refactoring.LibraryConfigurationChange.getFile;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ContentStamp;
import org.eclipse.ltk.core.refactoring.UndoTextFileChange;
import org.eclipse.text.edits.UndoEdit;

/**
 * An {@link UndoTextFileChange} that undoes a change made on a {@link LibraryConfigurationFile} in
 * the workspace.
 */
public class UndoLibraryConfigurationChange extends UndoTextFileChange {

  private final LibraryConfigurationFile libraryConfig;

  /**
   * Creates a new <code>UndoLibraryConfigurationChange</code>.
   * 
   * @param name the change's name, mainly used to render the change in the UI
   * @param libraryConfig the library configuration this change works on
   * @throws CoreException if the associated file resource cannot be found
   */
  public UndoLibraryConfigurationChange(String name, LibraryConfigurationFile libraryConfig,
      UndoEdit edit, ContentStamp stampToRestore, int saveMode) throws CoreException {
    super(name, getFile(libraryConfig), edit, stampToRestore, saveMode);
    this.libraryConfig = libraryConfig;
  }

  @Override
  public Object getModifiedElement() {
    return libraryConfig;
  }

  @Override
  public Change perform(IProgressMonitor pm) throws CoreException {
    pm.beginTask("", 2); //$NON-NLS-1$
    // TODO (pquitslund) add working copy support for libraries
//    libraryConfig.becomeWorkingCopy(new SubProgressMonitor(pm, 1));
    try {
      return super.perform(new SubProgressMonitor(pm, 1));
    } finally {
//      libraryConfig.discardWorkingCopy();
    }
  }

  @Override
  protected Change createUndoChange(UndoEdit edit, ContentStamp stampToRestore)
      throws CoreException {
    return new UndoLibraryConfigurationChange(
        getName(),
        libraryConfig,
        edit,
        stampToRestore,
        getSaveMode());
  }
}
