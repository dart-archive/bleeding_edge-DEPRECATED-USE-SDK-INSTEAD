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
import com.google.dart.tools.core.internal.model.CompilationUnitImpl;
import com.google.dart.tools.core.model.CompilationUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.ContentStamp;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.UndoEdit;

/**
 * A {@link TextFileChange} that operates on an {@link ICompilationUnit} in the workspace.
 */
public class CompilationUnitChange extends TextFileChange {

  private static IFile getFile(CompilationUnit cunit) {
    return (IFile) cunit.getResource();
  }

  private final CompilationUnit fCUnit;

  /** The (optional) refactoring descriptor */
  private ChangeDescriptor fDescriptor;

  /**
   * Creates a new <code>CompilationUnitChange</code>.
   * 
   * @param name the change's name, mainly used to render the change in the UI
   * @param cunit the compilation unit this change works on
   */
  public CompilationUnitChange(String name, CompilationUnit cunit) {
    super(name, getFile(cunit));
    Assert.isNotNull(cunit);
    fCUnit = cunit;
    setTextType("java"); //$NON-NLS-1$
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Object getAdapter(Class adapter) {
    if (CompilationUnit.class.equals(adapter)) {
      return fCUnit;
    }

    return super.getAdapter(adapter);
  }

  /**
   * Returns the compilation unit this change works on.
   * 
   * @return the compilation unit this change works on
   */
  public CompilationUnit getCompilationUnit() {
    return fCUnit;
  }

  @Override
  public ChangeDescriptor getDescriptor() {
    return fDescriptor;
  }

  @Override
  public Object getModifiedElement() {
    return fCUnit;
  }

  /**
   * Sets the refactoring descriptor for this change.
   * 
   * @param descriptor the descriptor to set, or <code>null</code> to set no descriptor
   */
  public void setDescriptor(ChangeDescriptor descriptor) {
    fDescriptor = descriptor;
  }

  @Override
  protected IDocument acquireDocument(IProgressMonitor pm) throws CoreException {
    pm.beginTask("", 2); //$NON-NLS-1$
    fCUnit.becomeWorkingCopy(new SubProgressMonitor(pm, 1));
    return super.acquireDocument(new SubProgressMonitor(pm, 1));
  }

  @Override
  protected Change createUndoChange(UndoEdit edit, ContentStamp stampToRestore) {
    try {
      return new UndoCompilationUnitChange(getName(), fCUnit, edit, stampToRestore, getSaveMode());
    } catch (CoreException e) {
      DartCore.logError(e);
      return null;
    }
  }

  @Override
  protected void releaseDocument(IDocument document, IProgressMonitor pm) throws CoreException {
    boolean isModified = isDocumentModified();
    super.releaseDocument(document, pm);
    try {
      fCUnit.discardWorkingCopy();
    } finally {
      if (isModified && !isDocumentAcquired()) {
        if (fCUnit.isWorkingCopy()) {
          ((CompilationUnitImpl) fCUnit).reconcile(true, pm);
        } else {
          fCUnit.makeConsistent(pm);
        }
      }
    }
  }
}
