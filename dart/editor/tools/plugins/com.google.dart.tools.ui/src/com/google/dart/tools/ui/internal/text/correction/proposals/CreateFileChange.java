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
package com.google.dart.tools.ui.internal.text.correction.proposals;

import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableEx;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;
import com.google.dart.tools.ui.internal.util.CoreUtility;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.resource.DeleteResourceChange;

import java.io.ByteArrayInputStream;
import java.io.File;

/**
 * {@link Change} for creating new {@link File}.
 * 
 * @coverage dart.editor.ui.correction
 */
public class CreateFileChange extends Change {
  private final String name;
  private final File file;
  private final String content;

  public CreateFileChange(String name, File file, String content) {
    this.name = name;
    this.file = file;
    this.content = content;
  }

  @Override
  public Object getModifiedElement() {
    return null;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void initializeValidationData(IProgressMonitor pm) {
  }

  @Override
  public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException,
      OperationCanceledException {
    return new RefactoringStatus();
  }

  @Override
  public Change perform(IProgressMonitor pm) throws CoreException {
    // prepare IFile
    final IFile newFile;
    {
      Source source = new FileBasedSource(file);
      IResource resource = DartCore.getProjectManager().getResource(source);
      if (!(resource instanceof IFile)) {
        return null;
      }
      newFile = (IFile) resource;
    }
    // ensure that folder with 'newFile' exists
    {
      IContainer container = newFile.getParent();
      if (container instanceof IFolder && !container.exists()) {
        CoreUtility.createFolder((IFolder) container, true, true, null);
      }
    }
    // do create
    ExecutionUtils.runLog(new RunnableEx() {
      @Override
      public void run() throws Exception {
        newFile.create(new ByteArrayInputStream(content.getBytes("UTF-8")), true, null);
        newFile.setCharset("UTF-8", null);
      }
    });
    // open editor
    EditorUtility.openInEditor(newFile);
    return new DeleteResourceChange(newFile.getFullPath(), true);
  }
}
