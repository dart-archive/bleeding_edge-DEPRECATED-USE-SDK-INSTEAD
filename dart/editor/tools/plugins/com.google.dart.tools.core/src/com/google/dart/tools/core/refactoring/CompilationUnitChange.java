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

import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.utilities.general.ScriptUtils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;

import java.util.Properties;

/**
 * A {@link TextFileChange} that operates on an {@link Source} in the workspace.
 * 
 * @coverage dart.tools.core
 */
public class CompilationUnitChange extends TextFileChange {

  public CompilationUnitChange(String name, IFile file) {
    super(name, file);
    setTextType("dart"); //$NON-NLS-1$
  }

  @Override
  public RefactoringStatus isValid(IProgressMonitor monitor) throws CoreException {
    // attempt to make read-only file editable
    if (needsSaving()) {
      IFile file = getFile();
      if (file.isReadOnly()) {
        Properties properties = ScriptUtils.getScriptProperties();
        String scriptLocation = (String) properties.get("file-refactoring-change");
        if (scriptLocation != null) {
          IStatus status = ScriptUtils.runScript(
              scriptLocation,
              file.getLocation().toOSString(),
              new SubProgressMonitor(monitor, 1));
          if (!status.isOK()) {
            return RefactoringStatus.createFatalErrorStatus(status.getMessage());
          }
        }
      }
    }
    // continue validation
    return super.isValid(monitor);
  }
}
