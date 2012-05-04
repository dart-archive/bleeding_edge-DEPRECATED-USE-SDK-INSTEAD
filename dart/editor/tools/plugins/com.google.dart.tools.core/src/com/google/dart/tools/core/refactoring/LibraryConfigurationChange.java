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
import com.google.dart.tools.core.model.LibraryConfigurationFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.ContentStamp;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.text.edits.UndoEdit;

/**
 * A {@link TextFileChange} that operates on a {@link LibraryConfigurationFile} in the workspace.
 */
public class LibraryConfigurationChange extends TextFileChange {
  /**
   * Get the file resource associated with a given library configuration file.
   * 
   * @param libraryConfig the library config file
   * @return the associated file
   * @throws CoreException if there is no associated resource
   */
  static IFile getFile(LibraryConfigurationFile libraryConfig) throws CoreException {
    IFile file = (IFile) libraryConfig.getResource();
    if (file == null) {
      String message = Messages.bind(
          Messages.change_library_has_no_file,
          TextProcessor.process(libraryConfig.getElementName()));
      throw new CoreException(new Status(IStatus.ERROR, DartCore.PLUGIN_ID, message));
    }
    return file;
  }

  /** The (optional) refactoring descriptor */
  private ChangeDescriptor descriptor;

  private final LibraryConfigurationFile libraryConfig;

  /**
   * Creates a new <code>LibraryConfigurationChange</code>.
   * 
   * @param name the change's name, mainly used to render the change in the UI
   * @param libraryConfig the library configuration this change works on
   * @throws CoreException
   */
  public LibraryConfigurationChange(String name, LibraryConfigurationFile libraryConfig)
      throws CoreException {
    super(name, getFile(libraryConfig));
    this.libraryConfig = libraryConfig;
  }

  @Override
  public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
    if (LibraryConfigurationFile.class.equals(adapter)) {
      return libraryConfig;
    }
    return super.getAdapter(adapter);
  }

  @Override
  public ChangeDescriptor getDescriptor() {
    return descriptor;
  }

  @Override
  public Object getModifiedElement() {
    return libraryConfig;
  }

  /**
   * Sets the refactoring descriptor for this change.
   * 
   * @param descriptor the descriptor to set, or <code>null</code> to set no descriptor
   */
  public void setDescriptor(ChangeDescriptor descriptor) {
    this.descriptor = descriptor;
  }

  @Override
  protected Change createUndoChange(UndoEdit edit, ContentStamp stampToRestore) {
    try {
      return new UndoLibraryConfigurationChange(
          getName(),
          libraryConfig,
          edit,
          stampToRestore,
          getSaveMode());
    } catch (CoreException e) {
      DartCore.logError(e);
      return null;
    }
  }
}
