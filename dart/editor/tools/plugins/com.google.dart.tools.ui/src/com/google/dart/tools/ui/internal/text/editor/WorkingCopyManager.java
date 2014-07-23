/*
 * Copyright (c) 2011, the Dart project authors.
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

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.ui.IWorkingCopyManager;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorInput;

import java.util.Map;

/**
 * This working copy manager works together with a given compilation unit document provider and
 * additionally offers to "overwrite" the working copy provided by this document provider.
 */
public class WorkingCopyManager implements IWorkingCopyManager {

  private final ICompilationUnitDocumentProvider fDocumentProvider;
  private Map<IEditorInput, CompilationUnit> fMap;
  private boolean fIsShuttingDown;

  /**
   * Creates a new working copy manager that co-operates with the given compilation unit document
   * provider.
   * 
   * @param provider the provider
   */
  public WorkingCopyManager(ICompilationUnitDocumentProvider provider) {
    Assert.isNotNull(provider);
    fDocumentProvider = provider;
  }

  @Override
  public void connect(IEditorInput input) throws CoreException {
    fDocumentProvider.connect(input);
  }

  @Override
  public void disconnect(IEditorInput input) {
    fDocumentProvider.disconnect(input);
  }

  @Override
  public CompilationUnit getWorkingCopy(IEditorInput input) {
    return getWorkingCopy(input, true);
  }

  /**
   * Returns the working copy remembered for the compilation unit encoded in the given editor input.
   * <p>
   * Note: This method must not be part of the public {@link IWorkingCopyManager} API.
   * </p>
   * 
   * @param input the editor input
   * @param primaryOnly if <code>true</code> only primary working copies will be returned
   * @return the working copy of the compilation unit, or <code>null</code> if the input does not
   *         encode an editor input, or if there is no remembered working copy for this compilation
   *         unit
   */
  public CompilationUnit getWorkingCopy(IEditorInput input, boolean primaryOnly) {
    return null;
  }

  @Override
  public void shutdown() {
    if (!fIsShuttingDown) {
      fIsShuttingDown = true;
      try {
        if (fMap != null) {
          fMap.clear();
          fMap = null;
        }
        fDocumentProvider.shutdown();
      } finally {
        fIsShuttingDown = false;
      }
    }
  }
}
