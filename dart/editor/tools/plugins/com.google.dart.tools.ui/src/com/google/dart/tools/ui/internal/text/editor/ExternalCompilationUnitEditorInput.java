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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.tools.core.internal.model.ExternalCompilationUnitImpl;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ide.FileStoreEditorInput;

/**
 * Instances of the class <code>ExternalCompilationUnitEditorInput</code> represent an editor input
 * on an external compilation unit.
 */
public class ExternalCompilationUnitEditorInput extends FileStoreEditorInput {
  /**
   * The external compilation unit being edited.
   */
  private ExternalCompilationUnitImpl compilationUnit;

  /**
   * Initialize a newly created editor input to represent the given external compilation unit.
   * 
   * @param fileStore the file store containing the external file representing the compilation unit
   * @param compilationUnit the external compilation unit being edited
   */
  public ExternalCompilationUnitEditorInput(IFileStore fileStore,
      ExternalCompilationUnitImpl compilationUnit) {
    super(fileStore);
    if (compilationUnit == null) {
      throw new IllegalArgumentException("compilationUnit");
    }
    this.compilationUnit = compilationUnit;
  }

  @Override
  public boolean exists() {
    return true;
  }

  /**
   * Return the external compilation unit being edited.
   * 
   * @return the external compilation unit being edited
   */
  public ExternalCompilationUnitImpl getCompilationUnit() {
    return compilationUnit;
  }

  @Override
  public String getFactoryId() {
    return ExternalCompilationUnitEditorInputFactory.ID;
  }

  /**
   * Returns whether this editor input if modifiable.
   * 
   * @return whether this editor input if modifiable
   */
  public boolean isModifiable() {
    //external files should never be treated as modifiable
    return false;
  }

  @Override
  public void saveState(IMemento memento) {
    ExternalCompilationUnitEditorInputFactory.saveState(memento, this);
  }
}
